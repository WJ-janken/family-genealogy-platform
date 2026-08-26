const CanvasEngine = require('../../canvas-engine/index');
const { get } = require('../../utils/request');

Page({
  data: {
    canvasWidth: 0,
    canvasHeight: 0,
    loading: true,
    // 视口信息
    scalePercent: 100,
    currentGeneration: '',
    // 节点详情弹窗
    showDetail: false,
    detailNode: null,
    // 搜索
    showSearch: false,
    searchKeyword: '',
    searchResults: [],
    // 布局方向
    orientation: 'vertical'
  },

  engine: null,

  onLoad(options) {
    const systemInfo = wx.getSystemInfoSync();
    const canvasWidth = systemInfo.windowWidth;
    const canvasHeight = systemInfo.windowHeight;

    this.setData({
      canvasWidth,
      canvasHeight
    });

    // 如果有指定成员 ID，后续定位到该节点
    this._targetMemberId = options.memberId ? parseInt(options.memberId) : null;
  },

  onReady() {
    this.initCanvas();
  },

  onUnload() {
    if (this.engine) {
      this.engine.destroy();
      this.engine = null;
    }
  },

  /**
   * 初始化 Canvas 引擎
   */
  async initCanvas() {
    try {
      const query = wx.createSelectorQuery();
      query.select('#treeCanvas').fields({ node: true, size: true }).exec((res) => {
        if (!res[0] || !res[0].node) {
          console.error('Canvas 节点获取失败');
          this.setData({ loading: false });
          return;
        }

        const canvas = res[0].node;
        const { canvasWidth, canvasHeight } = this.data;

        // 创建引擎实例
        this.engine = new CanvasEngine({
          nodeWidth: 140,
          nodeHeight: 180,
          horizontalGap: 40,
          verticalGap: 80,
          orientation: 'vertical'
        });

        // 初始化引擎
        this.engine.init(canvas, canvasWidth, canvasHeight);

        // 设置回调
        this.engine.onNodeTap = (nodeData) => this.onNodeTap(nodeData);
        this.engine.onNodeLongPress = (nodeData) => this.onNodeLongPress(nodeData);
        this.engine.onViewportChange = (state) => this.onViewportChange(state);

        // 加载数据
        this.loadTreeData();
      });
    } catch (err) {
      console.error('Canvas 初始化失败:', err);
      this.setData({ loading: false });
      wx.showToast({ title: '初始化失败', icon: 'none' });
    }
  },

  /**
   * 加载族谱树数据
   */
  async loadTreeData() {
    try {
      this.setData({ loading: true });

      // 从后端获取族谱树节点数据
      const data = await get('/members/tree', { maxDepth: 10 });

      if (!data || data.length === 0) {
        this.setData({ loading: false });
        wx.showToast({ title: '暂无族谱数据', icon: 'none' });
        return;
      }

      // 预加载头像图片
      const avatarUrls = data.filter(n => n.avatarUrl).map(n => n.avatarUrl);
      if (avatarUrls.length > 0 && this.engine.renderer) {
        await this.engine.renderer.preloadImages(avatarUrls);
      }

      // 加载数据到引擎
      this.engine.loadData(data);

      // 如果有目标节点，定位到该节点
      if (this._targetMemberId) {
        setTimeout(() => {
          this.engine.searchAndLocate(this._targetMemberId);
        }, 500);
      }

      this.setData({ loading: false });
    } catch (err) {
      console.error('加载族谱数据失败:', err);
      this.setData({ loading: false });
      wx.showToast({ title: '数据加载失败', icon: 'none' });
    }
  },

  // ==================== 触摸事件 ====================

  onTouchStart(e) {
    if (this.engine && this.engine.gesture) {
      this.engine.gesture.onTouchStart(e);
    }
  },

  onTouchMove(e) {
    if (this.engine && this.engine.gesture) {
      this.engine.gesture.onTouchMove(e);
    }
  },

  onTouchEnd(e) {
    if (this.engine && this.engine.gesture) {
      this.engine.gesture.onTouchEnd(e);
    }
  },

  // ==================== 节点交互回调 ====================

  /**
   * 节点点击
   */
  onNodeTap(nodeData) {
    this.setData({
      showDetail: true,
      detailNode: {
        id: nodeData.id,
        name: nodeData.name,
        gender: nodeData.gender,
        generation: nodeData.generation,
        branch: nodeData.branch,
        birthDate: nodeData.birthDate,
        deathDate: nodeData.deathDate,
        avatarUrl: nodeData.avatarUrl
      }
    });
  },

  /**
   * 节点长按 - 折叠/展开
   */
  onNodeLongPress(nodeData) {
    const node = this.engine.treeBuilder.findNodeById(nodeData.id);
    if (node && node.children && node.children.length > 0) {
      this.engine.toggleCollapse(node);
      wx.showToast({
        title: node.collapsed ? '已折叠' : '已展开',
        icon: 'none',
        duration: 1000
      });
    }
  },

  /**
   * 视口变化回调
   */
  onViewportChange(state) {
    this.setData({
      scalePercent: Math.round(state.scale * 100)
    });
  },

  // ==================== 工具栏操作 ====================

  goBack() {
    wx.navigateBack({ delta: 1 });
  },

  openSearch() {
    this.setData({ showSearch: true });
  },

  toggleLayout() {
    if (this.engine) {
      this.engine.toggleOrientation();
      const orientation = this.engine.options.orientation;
      this.setData({ orientation });
      wx.showToast({
        title: orientation === 'vertical' ? '竖向布局' : '横向布局',
        icon: 'none',
        duration: 1000
      });
    }
  },

  // ==================== 浮动按钮操作 ====================

  zoomIn() {
    if (this.engine) {
      this.engine.zoomIn();
    }
  },

  zoomOut() {
    if (this.engine) {
      this.engine.zoomOut();
    }
  },

  resetView() {
    if (this.engine) {
      this.engine.resetZoom();
    }
  },

  async exportImage() {
    if (!this.engine) return;

    wx.showLoading({ title: '导出中...' });
    try {
      const tempFilePath = await this.engine.exportImage();
      if (tempFilePath) {
        await wx.saveImageToPhotosAlbum({ filePath: tempFilePath });
        wx.showToast({ title: '已保存到相册', icon: 'success' });
      } else {
        wx.showToast({ title: '导出失败', icon: 'none' });
      }
    } catch (err) {
      if (err.errMsg && err.errMsg.includes('auth deny')) {
        wx.showToast({ title: '请授权相册权限', icon: 'none' });
      } else {
        wx.showToast({ title: '导出失败', icon: 'none' });
      }
    } finally {
      wx.hideLoading();
    }
  },

  // ==================== 详情弹窗 ====================

  closeDetail() {
    this.setData({ showDetail: false, detailNode: null });
  },

  viewFullDetail() {
    const { detailNode } = this.data;
    if (detailNode) {
      this.setData({ showDetail: false });
      wx.navigateTo({
        url: `/pages/member-detail/member-detail?id=${detailNode.id}`
      });
    }
  },

  // ==================== 搜索 ====================

  closeSearch() {
    this.setData({ showSearch: false, searchKeyword: '', searchResults: [] });
  },

  onSearchInput(e) {
    this.setData({ searchKeyword: e.detail.value });
  },

  async doSearch() {
    const { searchKeyword } = this.data;
    if (!searchKeyword.trim()) return;

    try {
      const data = await get('/members/search', {
        keyword: searchKeyword,
        page: 1,
        pageSize: 20
      });
      this.setData({ searchResults: data.records || [] });
    } catch (err) {
      wx.showToast({ title: '搜索失败', icon: 'none' });
    }
  },

  locateNode(e) {
    const memberId = e.currentTarget.dataset.id;
    if (this.engine) {
      const found = this.engine.searchAndLocate(memberId);
      if (found) {
        this.setData({ showSearch: false, searchKeyword: '', searchResults: [] });
      } else {
        wx.showToast({ title: '该节点不在当前视图中', icon: 'none' });
      }
    }
  }
});
