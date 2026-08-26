/**
 * 手势识别器模块
 * 
 * 处理微信小程序的触摸事件，识别以下手势：
 * - 单指拖拽（平移）
 * - 双指缩放（Pinch）
 * - 单击（Tap）
 * - 长按（Long Press）
 */

class Gesture {
  constructor(engine) {
    this.engine = engine;

    // 触摸状态
    this._touches = [];
    this._startTime = 0;
    this._startPos = null;
    this._lastPos = null;
    this._lastMoveTime = 0;
    this._velocityX = 0;
    this._velocityY = 0;

    // 双指缩放状态
    this._pinchStartDistance = 0;
    this._pinchStartScale = 1;
    this._isPinching = false;

    // 手势识别阈值
    this._tapThreshold = 10;       // 点击移动容差（px）
    this._longPressDelay = 600;    // 长按时间（ms）
    this._longPressTimer = null;
    this._isMoved = false;
    this._isLongPressed = false;

    // 绑定方法
    this.onTouchStart = this.onTouchStart.bind(this);
    this.onTouchMove = this.onTouchMove.bind(this);
    this.onTouchEnd = this.onTouchEnd.bind(this);
  }

  /**
   * 绑定事件（由页面调用）
   */
  bindEvents() {
    // 事件由页面 WXML 绑定，通过 engine 转发
  }

  /**
   * 解绑事件
   */
  unbindEvents() {
    this._clearLongPressTimer();
  }

  /**
   * 触摸开始
   */
  onTouchStart(e) {
    const touches = e.touches;
    this._touches = touches;
    this._startTime = Date.now();
    this._isMoved = false;
    this._isLongPressed = false;

    // 停止惯性滑动
    this.engine.viewport.stopInertia();

    if (touches.length === 1) {
      // 单指触摸
      this._startPos = { x: touches[0].x, y: touches[0].y };
      this._lastPos = { x: touches[0].x, y: touches[0].y };
      this._lastMoveTime = Date.now();
      this._velocityX = 0;
      this._velocityY = 0;

      // 启动长按计时器
      this._startLongPressTimer(touches[0].x, touches[0].y);
    } else if (touches.length === 2) {
      // 双指触摸 - 开始缩放
      this._clearLongPressTimer();
      this._isPinching = true;
      this._pinchStartDistance = this._getDistance(touches[0], touches[1]);
      this._pinchStartScale = this.engine.viewport.scale;
    }
  }

  /**
   * 触摸移动
   */
  onTouchMove(e) {
    const touches = e.touches;

    if (touches.length === 1 && !this._isPinching) {
      // 单指拖拽
      const currentX = touches[0].x;
      const currentY = touches[0].y;
      const deltaX = currentX - this._lastPos.x;
      const deltaY = currentY - this._lastPos.y;

      // 检查是否超过点击容差
      if (!this._isMoved) {
        const totalDelta = Math.sqrt(
          Math.pow(currentX - this._startPos.x, 2) +
          Math.pow(currentY - this._startPos.y, 2)
        );
        if (totalDelta > this._tapThreshold) {
          this._isMoved = true;
          this._clearLongPressTimer();
        }
      }

      if (this._isMoved) {
        // 计算速度（用于惯性滑动）
        const now = Date.now();
        const dt = now - this._lastMoveTime;
        if (dt > 0) {
          this._velocityX = deltaX / dt * 16; // 归一化到 16ms 帧率
          this._velocityY = deltaY / dt * 16;
        }
        this._lastMoveTime = now;

        this.engine.handlePan(deltaX, deltaY);
      }

      this._lastPos = { x: currentX, y: currentY };
    } else if (touches.length === 2) {
      // 双指缩放
      this._clearLongPressTimer();
      this._isPinching = true;
      this._isMoved = true;

      const currentDistance = this._getDistance(touches[0], touches[1]);
      const scaleFactor = currentDistance / this._pinchStartDistance;
      const newScale = this._pinchStartScale * scaleFactor;
      const factor = newScale / this.engine.viewport.scale;

      // 缩放中心为两指中点
      const centerX = (touches[0].x + touches[1].x) / 2;
      const centerY = (touches[0].y + touches[1].y) / 2;

      this.engine.handlePinch(factor, centerX, centerY);
    }
  }

  /**
   * 触摸结束
   */
  onTouchEnd(e) {
    this._clearLongPressTimer();

    if (this._isPinching) {
      // 缩放结束
      if (e.touches.length < 2) {
        this._isPinching = false;
      }
      return;
    }

    if (!this._isMoved && !this._isLongPressed) {
      // 判定为点击
      const duration = Date.now() - this._startTime;
      if (duration < this._longPressDelay && this._startPos) {
        this.engine.handleTap(this._startPos.x, this._startPos.y);
      }
    } else if (this._isMoved) {
      // 拖拽结束 - 启动惯性滑动
      if (Math.abs(this._velocityX) > 1 || Math.abs(this._velocityY) > 1) {
        this.engine.viewport.startInertia(
          this._velocityX,
          this._velocityY,
          () => this.engine.requestRender()
        );
      }
    }

    this._touches = [];
  }

  /**
   * 启动长按计时器
   */
  _startLongPressTimer(x, y) {
    this._clearLongPressTimer();
    this._longPressTimer = setTimeout(() => {
      if (!this._isMoved) {
        this._isLongPressed = true;
        // 触发震动反馈
        wx.vibrateShort({ type: 'medium' });
        this.engine.handleLongPress(x, y);
      }
    }, this._longPressDelay);
  }

  /**
   * 清除长按计时器
   */
  _clearLongPressTimer() {
    if (this._longPressTimer) {
      clearTimeout(this._longPressTimer);
      this._longPressTimer = null;
    }
  }

  /**
   * 计算两点之间的距离
   */
  _getDistance(touch1, touch2) {
    const dx = touch1.x - touch2.x;
    const dy = touch1.y - touch2.y;
    return Math.sqrt(dx * dx + dy * dy);
  }
}

module.exports = Gesture;
