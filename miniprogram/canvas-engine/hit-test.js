/**
 * Hit Test 命中检测模块
 * 
 * 使用网格空间索引实现 O(1) 级别的触摸点到节点的映射。
 * 将画布空间划分为网格，每个网格单元记录其中包含的节点，
 * 触摸时只需检查对应网格中的节点。
 */

class HitTest {
  constructor() {
    this.grid = new Map();     // 网格索引 "col_row" -> [nodes]
    this.cellSize = 200;       // 网格单元大小
    this.nodeWidth = 140;
    this.nodeHeight = 180;
  }

  /**
   * 构建空间索引
   * @param {Array} nodes - 所有可见的布局节点
   * @param {Object} options - 引擎配置
   */
  buildIndex(nodes, options) {
    this.grid.clear();
    this.nodeWidth = options.nodeWidth || 140;
    this.nodeHeight = options.nodeHeight || 180;

    for (const node of nodes) {
      // 计算节点覆盖的网格范围
      const startCol = Math.floor(node.x / this.cellSize);
      const endCol = Math.floor((node.x + this.nodeWidth) / this.cellSize);
      const startRow = Math.floor(node.y / this.cellSize);
      const endRow = Math.floor((node.y + this.nodeHeight) / this.cellSize);

      for (let col = startCol; col <= endCol; col++) {
        for (let row = startRow; row <= endRow; row++) {
          const key = `${col}_${row}`;
          if (!this.grid.has(key)) {
            this.grid.set(key, []);
          }
          this.grid.get(key).push(node);
        }
      }
    }
  }

  /**
   * 命中检测
   * @param {number} worldX - 世界坐标 X
   * @param {number} worldY - 世界坐标 Y
   * @returns {LayoutNode|null} 命中的节点，或 null
   */
  test(worldX, worldY) {
    const col = Math.floor(worldX / this.cellSize);
    const row = Math.floor(worldY / this.cellSize);
    const key = `${col}_${row}`;

    const candidates = this.grid.get(key);
    if (!candidates) return null;

    // 在候选节点中精确检测
    for (const node of candidates) {
      if (this._isPointInNode(worldX, worldY, node)) {
        return node;
      }
    }

    return null;
  }

  /**
   * 检测点是否在节点矩形内
   */
  _isPointInNode(x, y, node) {
    return x >= node.x &&
           x <= node.x + this.nodeWidth &&
           y >= node.y &&
           y <= node.y + this.nodeHeight;
  }

  /**
   * 查找某区域内的所有节点
   */
  queryRect(left, top, right, bottom) {
    const results = new Set();
    const startCol = Math.floor(left / this.cellSize);
    const endCol = Math.floor(right / this.cellSize);
    const startRow = Math.floor(top / this.cellSize);
    const endRow = Math.floor(bottom / this.cellSize);

    for (let col = startCol; col <= endCol; col++) {
      for (let row = startRow; row <= endRow; row++) {
        const key = `${col}_${row}`;
        const nodes = this.grid.get(key);
        if (nodes) {
          for (const node of nodes) {
            if (node.x + this.nodeWidth >= left &&
                node.x <= right &&
                node.y + this.nodeHeight >= top &&
                node.y <= bottom) {
              results.add(node);
            }
          }
        }
      }
    }

    return Array.from(results);
  }
}

module.exports = HitTest;
