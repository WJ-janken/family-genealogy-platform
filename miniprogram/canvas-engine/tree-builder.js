/**
 * 树结构构建模块
 * 
 * 将后端返回的扁平化节点数组转换为树形结构，
 * 并提供节点查找、索引构建等功能。
 */

class TreeBuilder {
  constructor() {
    this.nodeMap = new Map();    // id -> LayoutNode
    this.layoutNodes = [];       // 所有布局节点（展开状态下可见的）
  }

  /**
   * 从扁平数据构建树结构
   * @param {Array} flatData - 后端返回的节点数组 [{id, name, parentId, ...}]
   * @returns {LayoutNode} 根节点
   */
  buildTree(flatData) {
    this.nodeMap.clear();
    this.layoutNodes = [];

    // 第一遍：创建所有 LayoutNode
    const nodes = flatData.map(item => {
      const layoutNode = {
        data: item,
        x: 0,
        y: 0,
        width: 0,
        height: 0,
        collapsed: false,
        children: [],
        parent: null,
        // 布局算法辅助字段
        _prelim: 0,
        _modifier: 0,
        _thread: null,
        _ancestor: null,
        _change: 0,
        _shift: 0,
        _number: 0
      };
      this.nodeMap.set(item.id, layoutNode);
      return layoutNode;
    });

    // 第二遍：建立父子关系
    let root = null;
    for (const node of nodes) {
      const parentId = node.data.parentId;
      if (parentId && this.nodeMap.has(parentId)) {
        const parent = this.nodeMap.get(parentId);
        parent.children.push(node);
        node.parent = parent;
      } else if (!parentId) {
        root = node;
      }
    }

    // 如果没有找到根节点（parentId 为 null），取第一个没有父节点的
    if (!root && nodes.length > 0) {
      root = nodes.find(n => !n.parent) || nodes[0];
    }

    // 对子节点按 sortOrder 排序
    this._sortChildren(root);

    return root;
  }

  /**
   * 递归排序子节点
   */
  _sortChildren(node) {
    if (!node) return;
    if (node.children.length > 1) {
      node.children.sort((a, b) => {
        const sortA = a.data.sortOrder || 0;
        const sortB = b.data.sortOrder || 0;
        return sortA - sortB;
      });
    }
    for (const child of node.children) {
      this._sortChildren(child);
    }
  }

  /**
   * 获取所有可见的布局节点（考虑折叠状态）
   */
  getAllLayoutNodes() {
    this.layoutNodes = [];
    if (this.nodeMap.size === 0) return this.layoutNodes;

    // 找到根节点
    let root = null;
    for (const node of this.nodeMap.values()) {
      if (!node.parent) {
        root = node;
        break;
      }
    }

    if (root) {
      this._collectVisibleNodes(root);
    }
    return this.layoutNodes;
  }

  /**
   * 递归收集可见节点
   */
  _collectVisibleNodes(node) {
    this.layoutNodes.push(node);
    if (!node.collapsed) {
      for (const child of node.children) {
        this._collectVisibleNodes(child);
      }
    }
  }

  /**
   * 根据 ID 查找节点
   */
  findNodeById(id) {
    return this.nodeMap.get(id) || null;
  }

  /**
   * 展开某节点的所有祖先（确保节点可见）
   */
  expandAncestors(node) {
    let current = node.parent;
    while (current) {
      current.collapsed = false;
      current = current.parent;
    }
  }

  /**
   * 获取节点的可见子节点
   */
  getVisibleChildren(node) {
    if (!node || node.collapsed) return [];
    return node.children;
  }

  /**
   * 获取节点深度
   */
  getDepth(node) {
    let depth = 0;
    let current = node;
    while (current.parent) {
      depth++;
      current = current.parent;
    }
    return depth;
  }

  /**
   * 获取子树中所有节点
   */
  getSubtreeNodes(node) {
    const result = [];
    const queue = [node];
    while (queue.length > 0) {
      const current = queue.shift();
      result.push(current);
      for (const child of current.children) {
        queue.push(child);
      }
    }
    return result;
  }
}

module.exports = TreeBuilder;
