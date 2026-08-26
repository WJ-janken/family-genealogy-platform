const { get } = require('../../utils/request');
const { debounce } = require('../../utils/util');

Page({
  data: {
    keyword: '',
    generation: '',
    branch: '',
    gender: '',
    results: [],
    loading: false,
    hasMore: true,
    page: 1,
    pageSize: 20,
    generations: [],
    branches: []
  },

  onLoad() {
    this.loadFilters();
    this.debouncedSearch = debounce(this.doSearch.bind(this), 500);
  },

  async loadFilters() {
    try {
      const [generations, branches] = await Promise.all([
        get('/family/generations'),
        get('/family/branches')
      ]);
      this.setData({ generations: generations || [], branches: branches || [] });
    } catch (err) {
      console.error('加载筛选条件失败:', err);
    }
  },

  onInput(e) {
    this.setData({ keyword: e.detail.value });
    this.debouncedSearch();
  },

  onGenerationChange(e) {
    this.setData({ generation: this.data.generations[e.detail.value] || '' });
    this.doSearch();
  },

  onBranchChange(e) {
    this.setData({ branch: this.data.branches[e.detail.value] || '' });
    this.doSearch();
  },

  onGenderChange(e) {
    const genders = ['', 'M', 'F'];
    this.setData({ gender: genders[e.detail.value] || '' });
    this.doSearch();
  },

  async doSearch() {
    this.setData({ page: 1, results: [], hasMore: true });
    await this.loadResults();
  },

  async loadResults() {
    if (this.data.loading) return;
    this.setData({ loading: true });

    try {
      const params = {
        keyword: this.data.keyword,
        generation: this.data.generation,
        branch: this.data.branch,
        gender: this.data.gender,
        page: this.data.page,
        pageSize: this.data.pageSize
      };

      const res = await get('/family/members/search', params);
      const newResults = this.data.page === 1 ? res.list : [...this.data.results, ...res.list];
      this.setData({
        results: newResults,
        hasMore: newResults.length < res.total,
        loading: false
      });
    } catch (err) {
      console.error('搜索失败:', err);
      this.setData({ loading: false });
    }
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.setData({ page: this.data.page + 1 });
      this.loadResults();
    }
  },

  viewDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/member-detail/member-detail?id=${id}` });
  },

  locateOnTree(e) {
    const id = e.currentTarget.dataset.id;
    wx.switchTab({
      url: '/pages/tree/tree',
      success: () => {
        const pages = getCurrentPages();
        const treePage = pages[pages.length - 1];
        if (treePage && treePage.locateNode) {
          treePage.locateNode(id);
        }
      }
    });
  }
});
