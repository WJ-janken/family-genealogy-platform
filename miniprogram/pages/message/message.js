const { get, put } = require('../../utils/request');

Page({
  data: {
    messages: [],
    loading: true,
    activeTab: 'all', // all, unread, audit, system
    tabs: [
      { key: 'all', label: '全部' },
      { key: 'unread', label: '未读' },
      { key: 'audit', label: '审核' },
      { key: 'system', label: '系统' }
    ]
  },

  onLoad() {
    this.loadMessages();
  },

  onShow() {
    this.loadMessages();
  },

  onPullDownRefresh() {
    this.loadMessages().then(() => {
      wx.stopPullDownRefresh();
    });
  },

  async loadMessages() {
    try {
      this.setData({ loading: true });
      const { activeTab } = this.data;
      const params = {};
      if (activeTab === 'unread') params.read = false;
      if (activeTab === 'audit') params.type = 'AUDIT';
      if (activeTab === 'system') params.type = 'SYSTEM';

      const messages = await get('/messages', params);
      this.setData({ messages: messages || [], loading: false });
    } catch (e) {
      this.setData({ loading: false });
      wx.showToast({ title: '加载失败', icon: 'none' });
    }
  },

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({ activeTab: tab });
    this.loadMessages();
  },

  async markRead(e) {
    const id = e.currentTarget.dataset.id;
    try {
      await put(`/messages/${id}/read`);
      const messages = this.data.messages.map(m => {
        if (m.id === id) return { ...m, read: true };
        return m;
      });
      this.setData({ messages });
    } catch (e) {
      // 静默处理
    }
  },

  async markAllRead() {
    try {
      await put('/messages/read-all');
      const messages = this.data.messages.map(m => ({ ...m, read: true }));
      this.setData({ messages });
      wx.showToast({ title: '已全部标记已读', icon: 'success' });
    } catch (e) {
      wx.showToast({ title: '操作失败', icon: 'none' });
    }
  },

  viewDetail(e) {
    const item = e.currentTarget.dataset.item;
    // 标记已读
    if (!item.read) {
      this.markRead({ currentTarget: { dataset: { id: item.id } } });
    }

    // 根据消息类型跳转
    if (item.type === 'AUDIT' && item.targetId) {
      wx.navigateTo({
        url: `/pages/member-detail/member-detail?id=${item.targetId}`
      });
    }
  }
});
