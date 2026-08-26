const auth = require('../../utils/auth');

Page({
  data: {
    userInfo: null,
    isLoggedIn: false,
    menuList: [
      { id: 'submissions', title: '我的提交', icon: '📝', url: '/pages/message/message?type=submissions' },
      { id: 'favorites', title: '我的收藏', icon: '⭐', url: '' },
      { id: 'messages', title: '消息通知', icon: '🔔', url: '/pages/message/message' },
      { id: 'identity', title: '身份认证', icon: '🪪', url: '' }
    ],
    settingList: [
      { id: 'about', title: '关于家族', icon: '🏠' },
      { id: 'help', title: '使用帮助', icon: '❓' },
      { id: 'feedback', title: '意见反馈', icon: '💬' }
    ]
  },

  onShow() {
    this.checkLogin();
  },

  checkLogin() {
    const isLoggedIn = auth.isLoggedIn();
    const userInfo = auth.getUserInfo();
    this.setData({ isLoggedIn, userInfo });
  },

  async handleLogin() {
    try {
      wx.showLoading({ title: '登录中...' });
      await auth.login();
      this.checkLogin();
      wx.showToast({ title: '登录成功', icon: 'success' });
    } catch (err) {
      wx.showToast({ title: '登录失败', icon: 'none' });
    } finally {
      wx.hideLoading();
    }
  },

  handleLogout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          auth.logout();
          this.setData({ isLoggedIn: false, userInfo: null });
        }
      }
    });
  },

  navigateTo(e) {
    const url = e.currentTarget.dataset.url;
    if (url) {
      wx.navigateTo({ url });
    }
  }
});
