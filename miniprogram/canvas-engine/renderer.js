/**
 * Canvas 2D 渲染器模块
 * 
 * 负责将布局后的节点绘制到 Canvas 上：
 * - 节点卡片绘制（头像、姓名、辈分）
 * - 连线绘制（贝塞尔曲线）
 * - 视口变换应用
 * - 选中/高亮状态渲染
 */

class Renderer {
  constructor(ctx, canvas, options, dpr) {
    this.ctx = ctx;
    this.canvas = canvas;
    this.options = options;
    this.dpr = dpr;

    // 颜色配置
    this.colors = {
      background: '#1A1A2E',
      nodeBg: '#FFFFFF',
      nodeBorderMale: '#2196F3',
      nodeBorderFemale: '#E91E63',
      nodeText: '#2C2C2C',
      nodeSubText: '#666666',
      line: '#D4A574',
      lineWidth: 2,
      selectedBorder: '#FF9800',
      highlightGlow: '#FFD700',
      collapsedIndicator: '#8B4513'
    };

    // 图片缓存
    this._imageCache = new Map();
  }

  /**
   * 执行完整渲染
   */
  render(state) {
    const { viewport, nodes, rootNode, selectedNode, highlightedNode, orientation, width, height } = state;
    const ctx = this.ctx;

    // 清空画布
    ctx.save();
    ctx.setTransform(this.dpr, 0, 0, this.dpr, 0, 0);
    ctx.fillStyle = this.colors.background;
    ctx.fillRect(0, 0, width, height);

    // 应用视口变换
    ctx.translate(viewport.offsetX, viewport.offsetY);
    ctx.scale(viewport.scale, viewport.scale);

    // 绘制连线（先画线，再画节点，确保节点在线上方）
    if (rootNode) {
      this._drawConnections(rootNode, orientation);
    }

    // 绘制节点
    for (const node of nodes) {
      this._drawNode(node, node === selectedNode, node === highlightedNode);
    }

    ctx.restore();
  }

  /**
   * 递归绘制连线
   */
  _drawConnections(node, orientation) {
    if (node.collapsed) return;

    const children = node.children || [];
    const ctx = this.ctx;
    const nw = this.options.nodeWidth;
    const nh = this.options.nodeHeight;

    for (const child of children) {
      ctx.beginPath();
      ctx.strokeStyle = this.colors.line;
      ctx.lineWidth = this.colors.lineWidth;
      ctx.lineCap = 'round';

      if (orientation === 'vertical') {
        // 垂直布局：从父节点底部中心到子节点顶部中心
        const startX = node.x + nw / 2;
        const startY = node.y + nh;
        const endX = child.x + nw / 2;
        const endY = child.y;
        const midY = (startY + endY) / 2;

        ctx.moveTo(startX, startY);
        ctx.bezierCurveTo(startX, midY, endX, midY, endX, endY);
      } else {
        // 水平布局：从父节点右侧中心到子节点左侧中心
        const startX = node.x + nw;
        const startY = node.y + nh / 2;
        const endX = child.x;
        const endY = child.y + nh / 2;
        const midX = (startX + endX) / 2;

        ctx.moveTo(startX, startY);
        ctx.bezierCurveTo(midX, startY, midX, endY, endX, endY);
      }

      ctx.stroke();

      // 递归绘制子节点的连线
      this._drawConnections(child, orientation);
    }
  }

  /**
   * 绘制单个节点卡片
   */
  _drawNode(node, isSelected, isHighlighted) {
    const ctx = this.ctx;
    const { x, y } = node;
    const w = this.options.nodeWidth;
    const h = this.options.nodeHeight;
    const radius = 12;
    const data = node.data;

    // 高亮发光效果
    if (isHighlighted) {
      ctx.save();
      ctx.shadowColor = this.colors.highlightGlow;
      ctx.shadowBlur = 20;
      ctx.shadowOffsetX = 0;
      ctx.shadowOffsetY = 0;
      this._drawRoundRect(x - 4, y - 4, w + 8, h + 8, radius + 4);
      ctx.fillStyle = 'rgba(255, 215, 0, 0.3)';
      ctx.fill();
      ctx.restore();
    }

    // 节点背景
    ctx.save();
    if (isSelected) {
      ctx.shadowColor = 'rgba(255, 152, 0, 0.5)';
      ctx.shadowBlur = 12;
    } else {
      ctx.shadowColor = 'rgba(0, 0, 0, 0.15)';
      ctx.shadowBlur = 8;
      ctx.shadowOffsetY = 2;
    }

    this._drawRoundRect(x, y, w, h, radius);
    ctx.fillStyle = this.colors.nodeBg;
    ctx.fill();
    ctx.restore();

    // 边框
    const borderColor = data.gender === 'F' ? this.colors.nodeBorderFemale : this.colors.nodeBorderMale;
    ctx.beginPath();
    this._drawRoundRect(x, y, w, h, radius);
    ctx.strokeStyle = isSelected ? this.colors.selectedBorder : borderColor;
    ctx.lineWidth = isSelected ? 3 : 2;
    ctx.stroke();

    // 顶部色带
    ctx.save();
    ctx.beginPath();
    ctx.moveTo(x + radius, y);
    ctx.lineTo(x + w - radius, y);
    ctx.quadraticCurveTo(x + w, y, x + w, y + radius);
    ctx.lineTo(x + w, y + 6);
    ctx.lineTo(x, y + 6);
    ctx.lineTo(x, y + radius);
    ctx.quadraticCurveTo(x, y, x + radius, y);
    ctx.closePath();
    ctx.fillStyle = borderColor;
    ctx.fill();
    ctx.restore();

    // 头像区域
    const avatarSize = 50;
    const avatarX = x + (w - avatarSize) / 2;
    const avatarY = y + 20;

    // 头像圆形背景
    ctx.beginPath();
    ctx.arc(avatarX + avatarSize / 2, avatarY + avatarSize / 2, avatarSize / 2 + 2, 0, Math.PI * 2);
    ctx.fillStyle = borderColor;
    ctx.fill();

    // 头像占位（圆形裁剪）
    ctx.save();
    ctx.beginPath();
    ctx.arc(avatarX + avatarSize / 2, avatarY + avatarSize / 2, avatarSize / 2, 0, Math.PI * 2);
    ctx.clip();

    if (data.avatarUrl && this._imageCache.has(data.avatarUrl)) {
      const img = this._imageCache.get(data.avatarUrl);
      ctx.drawImage(img, avatarX, avatarY, avatarSize, avatarSize);
    } else {
      // 默认头像（首字母）
      ctx.fillStyle = '#F5F5F5';
      ctx.fillRect(avatarX, avatarY, avatarSize, avatarSize);
      ctx.fillStyle = borderColor;
      ctx.font = 'bold 22px PingFang SC';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      const initial = data.name ? data.name.charAt(0) : '?';
      ctx.fillText(initial, avatarX + avatarSize / 2, avatarY + avatarSize / 2);
    }
    ctx.restore();

    // 姓名（谱名）
    ctx.fillStyle = this.colors.nodeText;
    ctx.font = 'bold 14px PingFang SC';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'top';
    const name = data.name || '未知';
    const displayName = name.length > 4 ? name.substring(0, 4) + '…' : name;
    ctx.fillText(displayName, x + w / 2, avatarY + avatarSize + 10);

    // 俗名（如有）
    let nextY = avatarY + avatarSize + 28;
    if (data.aliasName) {
      ctx.fillStyle = this.colors.nodeSubText;
      ctx.font = '11px PingFang SC';
      const alias = data.aliasName.length > 5 ? data.aliasName.substring(0, 5) + '…' : data.aliasName;
      ctx.fillText(`(${alias})`, x + w / 2, nextY);
      nextY += 16;
    }

    // 世代/辈分
    ctx.fillStyle = this.colors.nodeSubText;
    ctx.font = '11px PingFang SC';
    const genText = data.generationChar
      ? `${data.generationChar}字辈 · 第${data.generation}世`
      : (data.generation ? `第${data.generation}世` : '');
    ctx.fillText(genText, x + w / 2, nextY);

    // 房支
    if (data.branch) {
      ctx.fillStyle = this.colors.nodeSubText;
      ctx.font = '10px PingFang SC';
      const branchText = data.branch.length > 4 ? data.branch.substring(0, 4) + '…' : data.branch;
      ctx.fillText(branchText, x + w / 2, nextY + 16);
    }

    // 折叠指示器
    if (node.children && node.children.length > 0 && node.collapsed) {
      const indicatorX = x + w / 2;
      const indicatorY = y + h - 14;
      ctx.beginPath();
      ctx.arc(indicatorX, indicatorY, 8, 0, Math.PI * 2);
      ctx.fillStyle = this.colors.collapsedIndicator;
      ctx.fill();
      ctx.fillStyle = '#FFFFFF';
      ctx.font = 'bold 10px PingFang SC';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText('+' + node.children.length, indicatorX, indicatorY);
    }

    // 子节点数量标记（未折叠时）
    if (node.data.childrenCount > 0 && !node.collapsed && node.children.length === 0) {
      const badgeX = x + w - 12;
      const badgeY = y + 12;
      ctx.beginPath();
      ctx.arc(badgeX, badgeY, 8, 0, Math.PI * 2);
      ctx.fillStyle = '#FF9800';
      ctx.fill();
      ctx.fillStyle = '#FFFFFF';
      ctx.font = 'bold 9px PingFang SC';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(String(node.data.childrenCount), badgeX, badgeY);
    }
  }

  /**
   * 绘制圆角矩形路径
   */
  _drawRoundRect(x, y, w, h, r) {
    const ctx = this.ctx;
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.lineTo(x + w - r, y);
    ctx.quadraticCurveTo(x + w, y, x + w, y + r);
    ctx.lineTo(x + w, y + h - r);
    ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
    ctx.lineTo(x + r, y + h);
    ctx.quadraticCurveTo(x, y + h, x, y + h - r);
    ctx.lineTo(x, y + r);
    ctx.quadraticCurveTo(x, y, x + r, y);
    ctx.closePath();
  }

  /**
   * 预加载图片
   */
  loadImage(url) {
    if (this._imageCache.has(url)) return Promise.resolve();

    return new Promise((resolve) => {
      const img = this.canvas.createImage();
      img.onload = () => {
        this._imageCache.set(url, img);
        resolve();
      };
      img.onerror = () => resolve(); // 加载失败静默处理
      img.src = url;
    });
  }

  /**
   * 批量预加载图片
   */
  async preloadImages(urls) {
    const tasks = urls.filter(url => url && !this._imageCache.has(url))
                      .map(url => this.loadImage(url));
    await Promise.all(tasks);
  }
}

module.exports = Renderer;
