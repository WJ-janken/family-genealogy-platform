-- 字辈诗/字辈歌表
CREATE TABLE IF NOT EXISTS sys_generation_poetry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    surname VARCHAR(50) NOT NULL COMMENT '姓氏',
    branch_name VARCHAR(100) DEFAULT NULL COMMENT '分支名称',
    title VARCHAR(200) NOT NULL COMMENT '字辈诗标题',
    generation_sequence TEXT NOT NULL COMMENT '字辈序列',
    interpretation TEXT COMMENT '字辈释义',
    start_generation INT DEFAULT 1 COMMENT '起始世代',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    creator_id BIGINT DEFAULT NULL COMMENT '创建人ID',
    remark TEXT COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_surname (surname),
    INDEX idx_branch (branch_name),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字辈诗/字辈歌表';