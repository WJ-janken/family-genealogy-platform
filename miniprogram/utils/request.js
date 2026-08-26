const app = getApp();

/**
 * 网络请求封装
 */
const request = (options) => {
  return new Promise((resolve, reject) => {
    const { url, method = 'GET', data, header = {} } = options;

    // 注入 token
    const token = app.globalData.token || wx.getStorageSync('token');
    if (token) {
      header['Authorization'] = `Bearer ${token}`;
    }
    header['Content-Type'] = header['Content-Type'] || 'application/json';

    wx.request({
      url: `${app.globalData.baseUrl}${url}`,
      method,
      data,
      header,
      success: (res) => {
        if (res.statusCode === 200) {
          const { code, message, data } = res.data;
          if (code === 0) {
            resolve(data);
          } else if (code === 401) {
            // token 过期，重新登录
            handleTokenExpired();
            reject(new Error(message || '登录已过期'));
          } else {
            wx.showToast({ title: message || '请求失败', icon: 'none' });
            reject(new Error(message));
          }
        } else if (res.statusCode === 401) {
          handleTokenExpired();
          reject(new Error('未授权'));
        } else {
          wx.showToast({ title: '网络异常', icon: 'none' });
          reject(new Error(`HTTP ${res.statusCode}`));
        }
      },
      fail: (err) => {
        wx.showToast({ title: '网络连接失败', icon: 'none' });
        reject(err);
      }
    });
  });
};

function handleTokenExpired() {
  wx.removeStorageSync('token');
  wx.removeStorageSync('userInfo');
  app.globalData.token = null;
  app.globalData.userInfo = null;
  wx.navigateTo({ url: '/pages/profile/profile' });
}

// 便捷方法
const get = (url, data) => request({ url, method: 'GET', data });
const post = (url, data) => request({ url, method: 'POST', data });
const put = (url, data) => request({ url, method: 'PUT', data });
const del = (url, data) => request({ url, method: 'DELETE', data });

module.exports = { request, get, post, put, del };
