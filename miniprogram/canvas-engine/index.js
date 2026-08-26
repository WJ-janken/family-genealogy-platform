/**
 * Canvas 族谱树引擎 - 入口模块
 * 
 * 提供族谱树的完整渲染和交互能力：
 * - 树形布局（Reingold-Tilford 算法）
 * - Canvas 2D 高性能渲染
 * - 手势交互（拖拽、缩放、点击）
 * - 视口管理与坐标变换
 * - 搜索定位动画
 * - 节点折叠/展开
 */

const TreeBuilder = require('./tree-builder');
const Layout = require('./layout');
const Renderer = require('./renderer');
const Viewport = require('./viewport');
const Gesture = require('./gesture');
const HitTest = require('./hit-test');
const Animation = require('./animation');

class CanvasEngine {
  constructor(options = {}) {
    this.canvas = null;
    this.ctx = null;
    this.dpr = 1;
    this.width = 0;
    this.height = 0;

    // 配置
    this.options = {
      nodeWidth: 140,
      nodeHeight: 180,
      horizontalGap: 40,
      verticalGap: 80,
      orientation: 'vertical', // vertical | horizontal
      backgroundColor: '#1A1A2E',
      ...options
    };

    // 子模块
    this.treeBuilder = new TreeBuilder();
    this.layout = new Layout(this.options);
    this.viewport = new Viewport();
    this.gesture = new Gesture(this);
    this.hitTest = new HitTest();
    this.animation = new Animation(this);
    this.renderer = null; // 需要 canvas 初始化后创建

    // 状态
    this.rootNode = null;
    this.flatNodes = [];
    this.selectedNode = null;
    this.highlightedNode = null;

    // 回调
    this.onNodeTap = null;
    this.onNodeLongPress = null;
    this.onViewportChange = null;

    // 渲染循环
    this._rafId = null;
    this._needsRender = false;
  }

  /**
   * 初始化引擎
   * @param {Object} canvas - Canvas 节点（通过 wx.createSelectorQuery 获取）
   * @param {number} width - 画布显示宽度
   * @param {number} height - 画布显示高度
   */
  init(canvas, width, height) {
    this.canvas = canvas;
    this.dpr = wx.getSystemInfoSync().pixelRatio;
    this.width = width;
    this.height = height;

    // 设置 canvas 实际尺寸（高清适配）
    canvas.width = width * this.dpr;
    canvas.height = height * this.dpr;

    this.ctx = canvas.getContext('2d');
    this.ctx.scale(this.dpr, this.dpr);

    // 初始化视口
    this.viewport.init(width, height);

    // 创建渲染器
    this.renderer = new Renderer(this.ctx, this.canvas, this.options, this.dpr);

    // 初始化手势
    this.gesture.bindEvents();

    return this;
  }

  /**
   * 加载族谱数据
   * @param {Array} flatData - 后端返回的扁平化节点数组
   */
  loadData(flatData) {
    if (!flatData || flatData.length === 0) return;

    // 构建树结构
    this.rootNode = this.treeBuilder.buildTree(flatData);
    this.flatNodes = flatData;

    // 计算布局
    this.recalculateLayout();

    // 更新空间索引
    this.hitTest.buildIndex(this.treeBuilder.getAllLayoutNodes(), this.options);

    // 居中显示根节点
    this.centerOnNode(this.rootNode);

    // 触发渲染
    this.requestRender();
  }

  /**
   * 重新计算布局
   */
  recalculateLayout() {
    if (!this.rootNode) return;
    this.layout.calculate(this.rootNode, this.options.orientation);
    this.hitTest.buildIndex(this.treeBuilder.getAllLayoutNodes(), this.options);
  }

  /**
   * 切换布局方向
   */
  toggleOrientation() {
    this.options.orientation = this.options.orientation === 'vertical' ? 'horizontal' : 'vertical';
    this.recalculateLayout();
    if (this.rootNode) {
      this.centerOnNode(this.rootNode);
    }
    this.requestRender();
  }

  /**
   * 折叠/展开节点
   */
  toggleCollapse(node) {
    if (!node || !node.children || node.children.length === 0) return;

    node.collapsed = !node.collapsed;
    this.recalculateLayout();

    // 动画过渡
    this.animation.animateLayoutChange(() => {
      this.requestRender();
    });
  }

  /**
   * 居中显示某个节点
   */
  centerOnNode(node, animate = true) {
    if (!node) return;

    const targetX = this.width / 2 - node.x - this.options.nodeWidth / 2;
    const targetY = this.height / 2 - node.y - this.options.nodeHeight / 2;

    if (animate) {
      this.animation.animateTo(
        { offsetX: targetX, offsetY: targetY },
        300,
        () => this.requestRender()
      );
    } else {
      this.viewport.setOffset(targetX, targetY);
      this.requestRender();
    }
  }

  /**
   * 搜索并定位到节点
   */
  searchAndLocate(memberId) {
    const node = this.treeBuilder.findNodeById(memberId);
    if (!node) return false;

    // 确保祖先节点都展开
    this.treeBuilder.expandAncestors(node);
    this.recalculateLayout();

    // 动画飞行到目标节点
    this.centerOnNode(node, true);

    // 高亮闪烁
    this.highlightedNode = node;
    this.animation.animateHighlight(node, () => {
      this.highlightedNode = null;
      this.requestRender();
    });

    return true;
  }

  /**
   * 缩放
   */
  zoomIn() {
    this.viewport.zoomBy(1.2, this.width / 2, this.height / 2);
    this.requestRender();
  }

  zoomOut() {
    this.viewport.zoomBy(0.8, this.width / 2, this.height / 2);
    this.requestRender();
  }

  resetZoom() {
    this.viewport.reset();
    if (this.rootNode) {
      this.centerOnNode(this.rootNode, false);
    }
    this.requestRender();
  }

  /**
   * 请求渲染（下一帧）
   */
  requestRender() {
    this._needsRender = true;
    if (!this._rafId) {
      this._rafId = this.canvas.requestAnimationFrame(() => {
        this._rafId = null;
        if (this._needsRender) {
          this._needsRender = false;
          this.render();
        }
      });
    }
  }

  /**
   * 执行渲染
   */
  render() {
    if (!this.ctx || !this.renderer) return;

    const visibleNodes = this.getVisibleNodes();

    this.renderer.render({
      viewport: this.viewport,
      nodes: visibleNodes,
      rootNode: this.rootNode,
      selectedNode: this.selectedNode,
      highlightedNode: this.highlightedNode,
      orientation: this.options.orientation,
      width: this.width,
      height: this.height
    });

    if (this.onViewportChange) {
      this.onViewportChange({
        scale: this.viewport.scale,
        offsetX: this.viewport.offsetX,
        offsetY: this.viewport.offsetY
      });
    }
  }

  /**
   * 获取视口内可见的节点（视口裁剪）
   */
  getVisibleNodes() {
    if (!this.rootNode) return [];

    const vp = this.viewport;
    const padding = 100; // 额外渲染边距

    // 计算视口在世界坐标中的范围
    const worldLeft = (-vp.offsetX - padding) / vp.scale;
    const worldTop = (-vp.offsetY - padding) / vp.scale;
    const worldRight = (this.width - vp.offsetX + padding) / vp.scale;
    const worldBottom = (this.height - vp.offsetY + padding) / vp.scale;

    return this.treeBuilder.getAllLayoutNodes().filter(node => {
      return node.x + this.options.nodeWidth >= worldLeft &&
             node.x <= worldRight &&
             node.y + this.options.nodeHeight >= worldTop &&
             node.y <= worldBottom;
    });
  }

  /**
   * 处理触摸点击（由 Gesture 模块调用）
   */
  handleTap(screenX, screenY) {
    // 屏幕坐标转世界坐标
    const worldPos = this.viewport.screenToWorld(screenX, screenY);
    const hitNode = this.hitTest.test(worldPos.x, worldPos.y);

    if (hitNode) {
      this.selectedNode = hitNode;
      this.requestRender();
      if (this.onNodeTap) {
        this.onNodeTap(hitNode.data);
      }
    } else {
      if (this.selectedNode) {
        this.selectedNode = null;
        this.requestRender();
      }
    }
  }

  /**
   * 处理长按（由 Gesture 模块调用）
   */
  handleLongPress(screenX, screenY) {
    const worldPos = this.viewport.screenToWorld(screenX, screenY);
    const hitNode = this.hitTest.test(worldPos.x, worldPos.y);

    if (hitNode && this.onNodeLongPress) {
      this.onNodeLongPress(hitNode.data);
    }
  }

  /**
   * 处理拖拽（由 Gesture 模块调用）
   */
  handlePan(deltaX, deltaY) {
    this.viewport.pan(deltaX, deltaY);
    this.requestRender();
  }

  /**
   * 处理缩放（由 Gesture 模块调用）
   */
  handlePinch(scale, centerX, centerY) {
    this.viewport.zoomBy(scale, centerX, centerY);
    this.requestRender();
  }

  /**
   * 导出为图片
   */
  exportImage() {
    return new Promise((resolve) => {
      wx.canvasToTempFilePath({
        canvas: this.canvas,
        success: (res) => resolve(res.tempFilePath),
        fail: () => resolve(null)
      });
    });
  }

  /**
   * 销毁引擎
   */
  destroy() {
    if (this._rafId) {
      this.canvas.cancelAnimationFrame(this._rafId);
      this._rafId = null;
    }
    this.gesture.unbindEvents();
    this.rootNode = null;
    this.flatNodes = [];
    this.ctx = null;
    this.canvas = null;
  }
}

module.exports = CanvasEngine;
