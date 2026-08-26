/**
 * 布局算法模块
 * 
 * 实现 Reingold-Tilford 紧凑树布局算法
 * 自动计算每个节点的 x/y 坐标，避免节点重叠
 * 
 * 参考论文：Tidier Drawings of Trees (Reingold & Tilford, 1981)
 * 改进版本：A Node-Positioning Algorithm for General Trees (Walker, 1990)
 */

class Layout {
  constructor(options = {}) {
    this.nodeWidth = options.nodeWidth || 140;
    this.nodeHeight = options.nodeHeight || 180;
    this.horizontalGap = options.horizontalGap || 40;
    this.verticalGap = options.verticalGap || 80;
  }

  /**
   * 计算树布局
   * @param {LayoutNode} root - 根节点
   * @param {string} orientation - 布局方向 'vertical' | 'horizontal'
   */
  calculate(root, orientation = 'vertical') {
    if (!root) return;

    // 重置布局辅助字段
    this._resetNode(root);

    // 第一遍：自底向上计算初步 x 坐标
    this._firstWalk(root);

    // 第二遍：自顶向下确定最终坐标
    this._secondWalk(root, -root._prelim, 0);

    // 如果是水平布局，交换 x/y
    if (orientation === 'horizontal') {
      this._swapCoordinates(root);
    }

    // 归一化坐标（确保所有坐标为正数）
    this._normalize(root);
  }

  /**
   * 重置节点的布局辅助字段
   */
  _resetNode(node) {
    node._prelim = 0;
    node._modifier = 0;
    node._thread = null;
    node._ancestor = node;
    node._change = 0;
    node._shift = 0;
    node.width = this.nodeWidth;
    node.height = this.nodeHeight;

    const children = this._getChildren(node);
    for (let i = 0; i < children.length; i++) {
      children[i]._number = i;
      this._resetNode(children[i]);
    }
  }

  /**
   * 获取可见子节点（考虑折叠）
   */
  _getChildren(node) {
    if (node.collapsed) return [];
    return node.children || [];
  }

  /**
   * 第一遍遍历（后序）
   * 计算每个节点的初步位置
   */
  _firstWalk(v) {
    const children = this._getChildren(v);

    if (children.length === 0) {
      // 叶子节点
      const leftSibling = this._getLeftSibling(v);
      if (leftSibling) {
        v._prelim = leftSibling._prelim + this._getDistance(leftSibling, v);
      } else {
        v._prelim = 0;
      }
    } else {
      // 内部节点
      let defaultAncestor = children[0];

      for (const child of children) {
        this._firstWalk(child);
        defaultAncestor = this._apportion(child, defaultAncestor);
      }

      this._executeShifts(v);

      // 节点居中于子节点之间
      const firstChild = children[0];
      const lastChild = children[children.length - 1];
      const midpoint = (firstChild._prelim + lastChild._prelim) / 2;

      const leftSibling = this._getLeftSibling(v);
      if (leftSibling) {
        v._prelim = leftSibling._prelim + this._getDistance(leftSibling, v);
        v._modifier = v._prelim - midpoint;
      } else {
        v._prelim = midpoint;
      }
    }
  }

  /**
   * 第二遍遍历（前序）
   * 确定最终坐标
   */
  _secondWalk(v, m, depth) {
    // x 坐标 = 初步位置 + 累积修正
    v.x = (v._prelim + m) * (this.nodeWidth + this.horizontalGap);
    // y 坐标 = 深度 * 层间距
    v.y = depth * (this.nodeHeight + this.verticalGap);

    const children = this._getChildren(v);
    for (const child of children) {
      this._secondWalk(child, m + v._modifier, depth + 1);
    }
  }

  /**
   * Apportion 算法 - 处理子树间的间距
   */
  _apportion(v, defaultAncestor) {
    const w = this._getLeftSibling(v);
    if (w) {
      let vInnerRight = v;
      let vOuterRight = v;
      let vInnerLeft = w;
      let vOuterLeft = this._getLeftmostSibling(v);

      let sInnerRight = vInnerRight._modifier;
      let sOuterRight = vOuterRight._modifier;
      let sInnerLeft = vInnerLeft._modifier;
      let sOuterLeft = vOuterLeft._modifier;

      while (this._nextRight(vInnerLeft) && this._nextLeft(vInnerRight)) {
        vInnerLeft = this._nextRight(vInnerLeft);
        vInnerRight = this._nextLeft(vInnerRight);
        vOuterLeft = this._nextLeft(vOuterLeft);
        vOuterRight = this._nextRight(vOuterRight);

        vOuterRight._ancestor = v;

        const shift = (vInnerLeft._prelim + sInnerLeft) -
                     (vInnerRight._prelim + sInnerRight) +
                     this._getDistance(vInnerLeft, vInnerRight);

        if (shift > 0) {
          const ancestor = this._ancestor(vInnerLeft, v, defaultAncestor);
          this._moveSubtree(ancestor, v, shift);
          sInnerRight += shift;
          sOuterRight += shift;
        }

        sInnerLeft += vInnerLeft._modifier;
        sInnerRight += vInnerRight._modifier;
        sOuterLeft += vOuterLeft._modifier;
        sOuterRight += vOuterRight._modifier;
      }

      if (this._nextRight(vInnerLeft) && !this._nextRight(vOuterRight)) {
        vOuterRight._thread = this._nextRight(vInnerLeft);
        vOuterRight._modifier += sInnerLeft - sOuterRight;
      }

      if (this._nextLeft(vInnerRight) && !this._nextLeft(vOuterLeft)) {
        vOuterLeft._thread = this._nextLeft(vInnerRight);
        vOuterLeft._modifier += sInnerRight - sOuterLeft;
        defaultAncestor = v;
      }
    }
    return defaultAncestor;
  }

  /**
   * 移动子树
   */
  _moveSubtree(wl, wr, shift) {
    const subtrees = wr._number - wl._number;
    if (subtrees > 0) {
      wr._change -= shift / subtrees;
      wr._shift += shift;
      wl._change += shift / subtrees;
      wr._prelim += shift;
      wr._modifier += shift;
    }
  }

  /**
   * 执行累积的位移
   */
  _executeShifts(v) {
    let shift = 0;
    let change = 0;
    const children = this._getChildren(v);
    for (let i = children.length - 1; i >= 0; i--) {
      const w = children[i];
      w._prelim += shift;
      w._modifier += shift;
      change += w._change;
      shift += w._shift + change;
    }
  }

  /**
   * 查找祖先节点
   */
  _ancestor(vil, v, defaultAncestor) {
    const parent = v.parent;
    if (parent && parent.children.includes(vil._ancestor)) {
      return vil._ancestor;
    }
    return defaultAncestor;
  }

  /**
   * 获取左兄弟
   */
  _getLeftSibling(node) {
    if (!node.parent) return null;
    const siblings = this._getChildren(node.parent);
    const idx = siblings.indexOf(node);
    return idx > 0 ? siblings[idx - 1] : null;
  }

  /**
   * 获取最左兄弟
   */
  _getLeftmostSibling(node) {
    if (!node.parent) return node;
    const siblings = this._getChildren(node.parent);
    return siblings[0];
  }

  /**
   * 获取右轮廓的下一个节点
   */
  _nextRight(node) {
    const children = this._getChildren(node);
    if (children.length > 0) {
      return children[children.length - 1];
    }
    return node._thread;
  }

  /**
   * 获取左轮廓的下一个节点
   */
  _nextLeft(node) {
    const children = this._getChildren(node);
    if (children.length > 0) {
      return children[0];
    }
    return node._thread;
  }

  /**
   * 计算两个节点之间的最小距离
   */
  _getDistance(v, w) {
    return 1; // 单位距离，实际像素距离在 secondWalk 中乘以 (nodeWidth + gap)
  }

  /**
   * 交换 x/y 坐标（水平布局）
   */
  _swapCoordinates(node) {
    const temp = node.x;
    node.x = node.y;
    node.y = temp;

    const children = this._getChildren(node);
    for (const child of children) {
      this._swapCoordinates(child);
    }
  }

  /**
   * 归一化坐标（确保所有坐标为正数，添加边距）
   */
  _normalize(root) {
    let minX = Infinity;
    let minY = Infinity;

    const allNodes = [];
    const queue = [root];
    while (queue.length > 0) {
      const node = queue.shift();
      allNodes.push(node);
      minX = Math.min(minX, node.x);
      minY = Math.min(minY, node.y);
      const children = this._getChildren(node);
      for (const child of children) {
        queue.push(child);
      }
    }

    // 添加边距
    const padding = 60;
    const offsetX = -minX + padding;
    const offsetY = -minY + padding;

    for (const node of allNodes) {
      node.x += offsetX;
      node.y += offsetY;
    }
  }
}

module.exports = Layout;
