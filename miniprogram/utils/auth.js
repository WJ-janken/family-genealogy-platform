const { post } = require('./request');

/**
 * 登录态管理
 */
const auth = {
  /**
   * 微信登录并获取 token
   */
  async login() {
    try {
      const code = await new Promise((resolve, reject) => {
        wx.login({
          success: (res) => res.code ? resolve(res.code) : reject(new Error('获取code失败')),
          fail: reject
        });
      });

      const data = await post('/auth/wx-login', { code });
      this.setToken(data.token);
      this.setUserInfo(data.userInfo);
      return data;
    } catch (err) {
      console.error('登录失败:', err);
      throw err;
    }
  },

  /**
   * 获取用户信息（需要用户授权）
   */
  async updateUserProfile(userInfo) {
    const data = await post('/auth/update-profile', userInfo);
    this.setUserInfo(data);
    return data;
  },

  setToken(token) {
    wx.setStorageSync('token', token);
    getApp().globalData.token = token;
  },

  getToken() {
    return wx.getStorageSync('token');
  },

  setUserInfo(userInfo) {
    wx.setStorageSync('userInfo', JSON.stringify(userInfo));
    getApp().globalData.userInfo = userInfo;
  },

  getUserInfo() {
    const info = wx.getStorageSync('userInfo');
    return info ? JSON.parse(info) : null;
  },

  isLoggedIn() {
    return !!this.getToken();
  },

  logout() {
    wx.removeStorageSync('token');
    wx.removeStorageSync('userInfo');
    const app = getApp();
    app.globalData.token = null;
    app.globalData.userInfo = null;
  }
};

module.exports = auth;
