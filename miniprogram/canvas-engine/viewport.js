/**
 * 视口管理模块
 * 
 * 管理画布的平移和缩放状态，提供坐标变换功能。
 * 基于仿射矩阵实现视口操作。
 */

class Viewport {
  constructor() {
    this.offsetX = 0;      // 平移偏移 X
    this.offsetY = 0;      // 平移偏移 Y
    this.scale = 1;        // 缩放比例
    this.minScale = 0.2;   // 最小缩放
    this.maxScale = 3.0;   // 最大缩放
    this.width = 0;        // 视口宽度
    this.height = 0;       // 视口高度

    // 惯性滑动
    this._velocityX = 0;
    this._velocityY = 0;
    this._inertiaTimer = null;
    this._friction = 0.92;
  }

  /**
   * 初始化视口尺寸
   */
  init(width, height) {
    this.width = width;
    this.height = height;
  }

  /**
   * 平移
   */
  pan(deltaX, deltaY) {
    this.offsetX += deltaX;
    this.offsetY += deltaY;
  }

  /**
   * 设置偏移
   */
  setOffset(x, y) {
    this.offsetX = x;
    this.offsetY = y;
  }

  /**
   * 以某点为中心缩放
   * @param {number} factor - 缩放因子（>1 放大，<1 缩小）
   * @param {number} centerX - 缩放中心 X（屏幕坐标）
   * @param {number} centerY - 缩放中心 Y（屏幕坐标）
   */
  zoomBy(factor, centerX, centerY) {
    const newScale = Math.max(this.minScale, Math.min(this.maxScale, this.scale * factor));
    const actualFactor = newScale / this.scale;

    // 以缩放中心点为锚点调整偏移
    this.offsetX = centerX - (centerX - this.offsetX) * actualFactor;
    this.offsetY = centerY - (centerY - this.offsetY) * actualFactor;
    this.scale = newScale;
  }

  /**
   * 设置缩放比例
   */
  setScale(scale, centerX, centerY) {
    const factor = scale / this.scale;
    this.zoomBy(factor, centerX || this.width / 2, centerY || this.height / 2);
  }

  /**
   * 屏幕坐标转世界坐标
   */
  screenToWorld(screenX, screenY) {
    return {
      x: (screenX - this.offsetX) / this.scale,
      y: (screenY - this.offsetY) / this.scale
    };
  }

  /**
   * 世界坐标转屏幕坐标
   */
  worldToScreen(worldX, worldY) {
    return {
      x: worldX * this.scale + this.offsetX,
      y: worldY * this.scale + this.offsetY
    };
  }

  /**
   * 开始惯性滑动
   */
  startInertia(velocityX, velocityY, onUpdate) {
    this.stopInertia();
    this._velocityX = velocityX;
    this._velocityY = velocityY;

    const animate = () => {
      if (Math.abs(this._velocityX) < 0.5 && Math.abs(this._velocityY) < 0.5) {
        this.stopInertia();
        return;
      }

      this.offsetX += this._velocityX;
      this.offsetY += this._velocityY;
      this._velocityX *= this._friction;
      this._velocityY *= this._friction;

      if (onUpdate) onUpdate();
      this._inertiaTimer = setTimeout(animate, 16);
    };

    animate();
  }

  /**
   * 停止惯性滑动
   */
  stopInertia() {
    if (this._inertiaTimer) {
      clearTimeout(this._inertiaTimer);
      this._inertiaTimer = null;
    }
    this._velocityX = 0;
    this._velocityY = 0;
  }

  /**
   * 重置视口
   */
  reset() {
    this.stopInertia();
    this.offsetX = 0;
    this.offsetY = 0;
    this.scale = 1;
  }

  /**
   * 获取当前视口状态
   */
  getState() {
    return {
      offsetX: this.offsetX,
      offsetY: this.offsetY,
      scale: this.scale
    };
  }

  /**
   * 检查缩放是否到达边界
   */
  isAtMinScale() {
    return this.scale <= this.minScale;
  }

  isAtMaxScale() {
    return this.scale >= this.maxScale;
  }
}

module.exports = Viewport;
