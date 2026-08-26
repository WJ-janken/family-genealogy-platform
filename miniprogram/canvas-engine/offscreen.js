/**
 * 离屏渲染模块
 * 
 * 使用离屏 Canvas 预渲染复杂的节点卡片，
 * 主画布通过 drawImage 贴图，提升渲染性能。
 * 
 * 适用于：
 * - 节点数量较多时的性能优化
 * - 头像图片的缓存管理
 * - 复杂节点样式的预渲染
 */

class OffscreenRenderer {
  constructor() {
    this.offscreenCanvas = null;
    this.offscreenCtx = null;
    this.cardCache = new Map();  // nodeId -> {canvas, dirty}
    this.imageCache = new Map(); // url -> Image
    this.maxCacheSize = 100;     // 最大缓存节点数
  }

  /**
   * 初始化离屏 Canvas
   * @param {Object} mainCanvas - 主 Canvas 节点（用于创建离屏 Canvas）
   */
  init(mainCanvas) {
    // 微信小程序中通过 wx.createOffscreenCanvas 创建
    try {
      this.offscreenCanvas = wx.createOffscreenCanvas({
        type: '2d',
        width: 300,
        height: 400
      });
      this.offscreenCtx = this.offscreenCanvas.getContext('2d');
    } catch (e) {
      // 部分低版本不支持离屏 Canvas，降级处理
      console.warn('离屏 Canvas 不可用，使用直接渲染模式');
      this.offscreenCanvas = null;
    }
  }

  /**
   * 获取节点的预渲染缓存
   * @param {LayoutNode} node - 布局节点
   * @param {Object} options - 渲染选项
   * @returns {Object|null} 缓存的 Canvas 或 null（需要直接渲染）
   */
  getNodeCache(node, options) {
    if (!this.offscreenCanvas) return null;

    const cacheKey = this._getCacheKey(node);
    if (this.cardCache.has(cacheKey)) {
      const cached = this.cardCache.get(cacheKey);
      if (!cached.dirty) {
        return cached.canvas;
      }
    }

    return null;
  }

  /**
   * 预渲染节点到离屏 Canvas
   */
  prerenderNode(node, options, renderFn) {
    if (!this.offscreenCanvas) return null;

    // 检查缓存容量
    if (this.cardCache.size >= this.maxCacheSize) {
      this._evictOldest();
    }

    const cacheKey = this._getCacheKey(node);
    const width = options.nodeWidth || 140;
    const height = options.nodeHeight || 180;

    // 调整离屏 Canvas 尺寸
    this.offscreenCanvas.width = width * 2; // 2x for retina
    this.offscreenCanvas.height = height * 2;

    const ctx = this.offscreenCtx;
    ctx.clearRect(0, 0, width * 2, height * 2);
    ctx.scale(2, 2);

    // 执行渲染
    if (renderFn) {
      renderFn(ctx, 0, 0, width, height, node);
    }

    ctx.setTransform(1, 0, 0, 1, 0, 0);

    this.cardCache.set(cacheKey, {
      canvas: this.offscreenCanvas,
      timestamp: Date.now(),
      dirty: false
    });

    return this.offscreenCanvas;
  }

  /**
   * 标记节点缓存为脏（需要重新渲染）
   */
  invalidateNode(nodeId) {
    const cacheKey = String(nodeId);
    if (this.cardCache.has(cacheKey)) {
      this.cardCache.get(cacheKey).dirty = true;
    }
  }

  /**
   * 清除所有缓存
   */
  clearCache() {
    this.cardCache.clear();
  }

  /**
   * 加载图片到缓存
   */
  loadImage(url, canvas) {
    return new Promise((resolve, reject) => {
      if (this.imageCache.has(url)) {
        resolve(this.imageCache.get(url));
        return;
      }

      const img = canvas.createImage();
      img.onload = () => {
        this.imageCache.set(url, img);
        resolve(img);
      };
      img.onerror = (err) => {
        reject(err);
      };
      img.src = url;
    });
  }

  /**
   * 批量预加载图片
   */
  async preloadImages(urls, canvas) {
    const tasks = urls
      .filter(url => url && !this.imageCache.has(url))
      .map(url => this.loadImage(url, canvas).catch(() => null));
    await Promise.all(tasks);
  }

  /**
   * 获取已缓存的图片
   */
  getCachedImage(url) {
    return this.imageCache.get(url) || null;
  }

  // ==================== 私有方法 ====================

  _getCacheKey(node) {
    return String(node.data.id);
  }

  /**
   * 淘汰最旧的缓存
   */
  _evictOldest() {
    let oldestKey = null;
    let oldestTime = Infinity;

    for (const [key, value] of this.cardCache) {
      if (value.timestamp < oldestTime) {
        oldestTime = value.timestamp;
        oldestKey = key;
      }
    }

    if (oldestKey) {
      this.cardCache.delete(oldestKey);
    }
  }

  /**
   * 销毁
   */
  destroy() {
    this.clearCache();
    this.imageCache.clear();
    this.offscreenCanvas = null;
    this.offscreenCtx = null;
  }
}

module.exports = OffscreenRenderer;
