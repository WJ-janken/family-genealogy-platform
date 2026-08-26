Page({
  data: {
    member: null,
    careers: [],
    relations: null,
    loading: true
  },

  onLoad(options) {
    if (options.id) {
      this.loadMemberDetail(options.id);
    }
  },

  async loadMemberDetail(id) {
    const { get } = require('../../utils/request');
    try {
      const [member, careers, relations] = await Promise.all([
        get(`/family/members/${id}`),
        get(`/family/members/${id}/careers`),
        get(`/family/members/${id}/relations`)
      ]);
      this.setData({ member, careers, relations, loading: false });
    } catch (err) {
      console.error('加载人物详情失败:', err);
      this.setData({ loading: false });
      wx.showToast({ title: '加载失败', icon: 'none' });
    }
  },

  viewRelation(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/member-detail/member-detail?id=${id}` });
  },

  submitEdit() {
    wx.navigateTo({ url: `/pages/submit/submit?id=${this.data.member.id}&type=update` });
  }
});
