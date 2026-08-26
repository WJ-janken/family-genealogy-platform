/**
 * 动画系统模块
 * 
 * 提供平滑过渡动画：
 * - 视口平移/缩放动画
 * - 搜索定位飞行动画
 * - 节点高亮闪烁动画
 * - 展开/折叠过渡动画
 */

class Animation {
  constructor(engine) {
    this.engine = engine;
    this._animations = [];
    this._frameId = null;
  }

  /**
   * 视口动画 - 平滑移动到目标位置
   * @param {Object} target - 目标状态 {offsetX, offsetY, scale}
   * @param {number} duration - 动画时长（ms）
   * @param {Function} onFrame - 每帧回调
   */
  animateTo(target, duration = 300, onFrame) {
    const viewport = this.engine.viewport;
    const startState = {
      offsetX: viewport.offsetX,
      offsetY: viewport.offsetY,
      scale: viewport.scale
    };

    const targetState = {
      offsetX: target.offsetX !== undefined ? target.offsetX : startState.offsetX,
      offsetY: target.offsetY !== undefined ? target.offsetY : startState.offsetY,
      scale: target.scale !== undefined ? target.scale : startState.scale
    };

    const startTime = Date.now();

    const animate = () => {
      const elapsed = Date.now() - startTime;
      const progress = Math.min(elapsed / duration, 1);
      const eased = this._easeOutCubic(progress);

      viewport.offsetX = startState.offsetX + (targetState.offsetX - startState.offsetX) * eased;
      viewport.offsetY = startState.offsetY + (targetState.offsetY - startState.offsetY) * eased;
      viewport.scale = startState.scale + (targetState.scale - startState.scale) * eased;

      if (onFrame) onFrame();

      if (progress < 1) {
        this._frameId = this.engine.canvas.requestAnimationFrame(animate);
      }
    };

    // 取消之前的动画
    this._cancelFrame();
    this._frameId = this.engine.canvas.requestAnimationFrame(animate);
  }

  /**
   * 高亮闪烁动画
   * @param {LayoutNode} node - 目标节点
   * @param {Function} onComplete - 完成回调
   */
  animateHighlight(node, onComplete) {
    const duration = 2000; // 闪烁持续 2 秒
    const startTime = Date.now();
    const blinkCount = 4; // 闪烁次数

    const animate = () => {
      const elapsed = Date.now() - startTime;
      const progress = elapsed / duration;

      if (progress >= 1) {
        if (onComplete) onComplete();
        return;
      }

      // 正弦波闪烁效果
      const blink = Math.sin(progress * blinkCount * Math.PI * 2);
      node._highlightAlpha = Math.abs(blink);

      this.engine.requestRender();
      this._frameId = this.engine.canvas.requestAnimationFrame(animate);
    };

    this._cancelFrame();
    this._frameId = this.engine.canvas.requestAnimationFrame(animate);
  }

  /**
   * 布局变化过渡动画
   * @param {Function} onComplete - 完成回调
   */
  animateLayoutChange(onComplete) {
    // 简单实现：直接触发重绘
    // 高级实现可以记录旧坐标，插值到新坐标
    if (onComplete) onComplete();
  }

  /**
   * 缩放弹性动画（超出边界回弹）
   */
  animateBounceBack(targetScale, centerX, centerY, onFrame) {
    const viewport = this.engine.viewport;
    const startScale = viewport.scale;
    const duration = 200;
    const startTime = Date.now();

    const animate = () => {
      const elapsed = Date.now() - startTime;
      const progress = Math.min(elapsed / duration, 1);
      const eased = this._easeOutBack(progress);

      const newScale = startScale + (targetScale - startScale) * eased;
      const factor = newScale / viewport.scale;
      viewport.zoomBy(factor, centerX, centerY);

      if (onFrame) onFrame();

      if (progress < 1) {
        this._frameId = this.engine.canvas.requestAnimationFrame(animate);
      }
    };

    this._cancelFrame();
    this._frameId = this.engine.canvas.requestAnimationFrame(animate);
  }

  /**
   * 取消当前动画帧
   */
  _cancelFrame() {
    if (this._frameId && this.engine.canvas) {
      this.engine.canvas.cancelAnimationFrame(this._frameId);
      this._frameId = null;
    }
  }

  // ==================== 缓动函数 ====================

  /**
   * Ease Out Cubic - 减速缓出
   */
  _easeOutCubic(t) {
    return 1 - Math.pow(1 - t, 3);
  }

  /**
   * Ease Out Back - 带回弹的缓出
   */
  _easeOutBack(t) {
    const c1 = 1.70158;
    const c3 = c1 + 1;
    return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
  }

  /**
   * Ease In Out Quad - 先加速后减速
   */
  _easeInOutQuad(t) {
    return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
  }

  /**
   * 销毁
   */
  destroy() {
    this._cancelFrame();
    this._animations = [];
  }
}

module.exports = Animation;
