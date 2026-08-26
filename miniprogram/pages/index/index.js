const { get } = require('../../utils/request');

Page({
  data: {
    familyName: '',
    hallName: '',
    statistics: {
      totalMembers: 0,
      generations: 0,
      branches: 0
    },
    recentUpdates: [],
    loading: true
  },

  onLoad() {
    this.loadData();
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 0 });
    }
  },

  onPullDownRefresh() {
    this.loadData().then(() => wx.stopPullDownRefresh());
  },

  async loadData() {
    try {
      const [stats, updates] = await Promise.all([
        get('/family/statistics'),
        get('/family/recent-updates', { limit: 10 })
      ]);
      this.setData({
        statistics: stats || this.data.statistics,
        recentUpdates: updates || [],
        loading: false
      });
    } catch (err) {
      console.error('加载首页数据失败:', err);
      this.setData({ loading: false });
    }
  },

  navigateToTree() {
    wx.switchTab({ url: '/pages/tree/tree' });
  },

  navigateToSearch() {
    wx.switchTab({ url: '/pages/search/search' });
  },

  navigateToChronicle() {
    wx.navigateTo({ url: '/pages/chronicle/chronicle' });
  },

  navigateToSubmit() {
    wx.navigateTo({ url: '/pages/submit/submit' });
  }
});
