-- ============================================
-- 家族族谱数字化管理平台 - 数据库初始化脚本
-- 数据库: MySQL 8.0+
-- 字符集: utf8mb4
-- ============================================

CREATE DATABASE IF NOT EXISTS family_genealogy
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE family_genealogy;

-- ============================================
-- 1. 系统用户表
-- ============================================
CREATE TABLE sys_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
  openid VARCHAR(64) DEFAULT NULL COMMENT '微信OpenID',
  union_id VARCHAR(64) DEFAULT NULL COMMENT '微信UnionID',
  username VARCHAR(50) DEFAULT NULL COMMENT '用户名（后台登录用）',
  password VARCHAR(255) DEFAULT NULL COMMENT '密码（后台登录用，BCrypt加密）',
  nickname VARCHAR(50) DEFAULT '' COMMENT '昵称',
  avatar_url VARCHAR(500) DEFAULT '' COMMENT '头像URL',
  phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  role ENUM('SUPER_ADMIN', 'ADMIN', 'MEMBER', 'GUEST') NOT NULL DEFAULT 'GUEST' COMMENT '角色',
  member_id BIGINT DEFAULT NULL COMMENT '关联的族谱成员ID',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
  last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_openid (openid),
  UNIQUE KEY uk_username (username),
  KEY idx_role (role),
  KEY idx_member_id (member_id)
) ENGINE=InnoDB COMMENT='系统用户表';

-- ============================================
-- 2. 族谱成员表（邻接表模型）
-- ============================================
CREATE TABLE family_member (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '成员ID',
  name VARCHAR(50) NOT NULL COMMENT '谱名（辈分名，如郑志泉）',
  alias_name VARCHAR(50) DEFAULT '' COMMENT '俗名/书名（户口名，如郑二狗）',
  generation_char VARCHAR(10) DEFAULT '' COMMENT '辈分字（如：志）',
  gender ENUM('M', 'F') NOT NULL DEFAULT 'M' COMMENT '性别：M-男 F-女',
  generation INT NOT NULL DEFAULT 1 COMMENT '世代（第几世）',
  branch VARCHAR(50) DEFAULT '' COMMENT '房支',
  parent_id BIGINT DEFAULT NULL COMMENT '父节点ID',
  spouse_id BIGINT DEFAULT NULL COMMENT '配偶ID',
  birth_date DATE DEFAULT NULL COMMENT '出生日期',
  death_date DATE DEFAULT NULL COMMENT '去世日期',
  birth_place VARCHAR(200) DEFAULT '' COMMENT '出生地',
  biography TEXT COMMENT '生平简介',
  avatar_url VARCHAR(500) DEFAULT '' COMMENT '头像URL',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '同辈排序（排行）',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  created_by BIGINT DEFAULT NULL COMMENT '创建人ID',
  KEY idx_parent_id (parent_id),
  KEY idx_generation (generation),
  KEY idx_branch (branch),
  KEY idx_name (name),
  KEY idx_spouse_id (spouse_id),
  KEY idx_is_deleted (is_deleted)
) ENGINE=InnoDB COMMENT='族谱成员表';

-- ============================================
-- 3. 成员闭包表（用于高效祖先/后代查询）
-- ============================================
CREATE TABLE member_closure (
  ancestor_id BIGINT NOT NULL COMMENT '祖先ID',
  descendant_id BIGINT NOT NULL COMMENT '后代ID',
  depth INT NOT NULL DEFAULT 0 COMMENT '深度（代数差）',
  PRIMARY KEY (ancestor_id, descendant_id),
  KEY idx_descendant_depth (descendant_id, depth),
  KEY idx_ancestor_depth (ancestor_id, depth),
  CONSTRAINT fk_closure_ancestor FOREIGN KEY (ancestor_id) REFERENCES family_member(id) ON DELETE CASCADE,
  CONSTRAINT fk_closure_descendant FOREIGN KEY (descendant_id) REFERENCES family_member(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='成员闭包表（祖先-后代关系）';

-- ============================================
-- 4. 审核记录表
-- ============================================
CREATE TABLE audit_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '审核记录ID',
  submitter_id BIGINT NOT NULL COMMENT '提交人用户ID',
  target_type VARCHAR(50) NOT NULL COMMENT '目标类型：MEMBER/CHRONICLE/CAREER',
  target_id BIGINT DEFAULT NULL COMMENT '目标ID（修改/删除时有值）',
  action ENUM('CREATE', 'UPDATE', 'DELETE') NOT NULL COMMENT '操作类型',
  before_data JSON DEFAULT NULL COMMENT '修改前数据（JSON）',
  after_data JSON NOT NULL COMMENT '提交的数据（JSON）',
  status ENUM('DRAFT', 'PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING' COMMENT '审核状态',
  reviewer_id BIGINT DEFAULT NULL COMMENT '审核人用户ID',
  review_comment TEXT COMMENT '审核意见',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  reviewed_at DATETIME DEFAULT NULL COMMENT '审核时间',
  KEY idx_submitter (submitter_id),
  KEY idx_status (status),
  KEY idx_target (target_type, target_id),
  KEY idx_reviewer (reviewer_id),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='审核记录表';

-- ============================================
-- 5. 地方志表
-- ============================================
CREATE TABLE local_chronicle (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '地方志ID',
  title VARCHAR(200) NOT NULL COMMENT '标题',
  category VARCHAR(50) NOT NULL DEFAULT '其他' COMMENT '分类：祠堂/祖坟/风俗/历史/地理/其他',
  content LONGTEXT COMMENT '内容（富文本HTML）',
  cover_url VARCHAR(500) DEFAULT '' COMMENT '封面图URL',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  created_by BIGINT DEFAULT NULL COMMENT '创建人ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_category (category),
  KEY idx_is_deleted (is_deleted)
) ENGINE=InnoDB COMMENT='地方志表';

-- ============================================
-- 6. 任职履历表
-- ============================================
CREATE TABLE career_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '履历ID',
  member_id BIGINT NOT NULL COMMENT '成员ID',
  position VARCHAR(100) NOT NULL COMMENT '职务/功名',
  organization VARCHAR(200) DEFAULT '' COMMENT '任职机构',
  start_date DATE DEFAULT NULL COMMENT '开始日期',
  end_date DATE DEFAULT NULL COMMENT '结束日期',
  description TEXT COMMENT '描述/备注',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_member_id (member_id),
  CONSTRAINT fk_career_member FOREIGN KEY (member_id) REFERENCES family_member(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='任职履历表';

-- ============================================
-- 7. 操作日志表
-- ============================================
CREATE TABLE operation_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
  user_id BIGINT NOT NULL COMMENT '操作人ID',
  user_name VARCHAR(50) DEFAULT '' COMMENT '操作人姓名',
  module VARCHAR(50) NOT NULL COMMENT '模块：MEMBER/CHRONICLE/CAREER/USER/SYSTEM',
  action VARCHAR(50) NOT NULL COMMENT '操作：CREATE/UPDATE/DELETE/IMPORT/EXPORT/LOGIN',
  target_type VARCHAR(50) DEFAULT NULL COMMENT '目标类型',
  target_id BIGINT DEFAULT NULL COMMENT '目标ID',
  description VARCHAR(500) DEFAULT '' COMMENT '操作描述',
  request_ip VARCHAR(50) DEFAULT '' COMMENT '请求IP',
  request_data JSON DEFAULT NULL COMMENT '请求参数（脱敏）',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  KEY idx_user_id (user_id),
  KEY idx_module (module),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='操作日志表';

-- ============================================
-- 8. 系统配置表
-- ============================================
CREATE TABLE sys_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '配置ID',
  config_key VARCHAR(100) NOT NULL COMMENT '配置键',
  config_value TEXT COMMENT '配置值',
  description VARCHAR(200) DEFAULT '' COMMENT '描述',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB COMMENT='系统配置表';

-- ============================================
-- 9. 消息通知表
-- ============================================
CREATE TABLE message_notification (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
  user_id BIGINT NOT NULL COMMENT '接收人ID',
  title VARCHAR(200) NOT NULL COMMENT '标题',
  content VARCHAR(500) DEFAULT '' COMMENT '内容',
  type VARCHAR(50) NOT NULL DEFAULT 'SYSTEM' COMMENT '类型：AUDIT_RESULT/TREE_UPDATE/SYSTEM',
  related_id BIGINT DEFAULT NULL COMMENT '关联业务ID',
  is_read TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读 1-已读',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_user_read (user_id, is_read),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='消息通知表';

-- ============================================
-- 10. 附件表
-- ============================================
CREATE TABLE attachment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '附件ID',
  file_name VARCHAR(200) NOT NULL COMMENT '原始文件名',
  file_url VARCHAR(500) NOT NULL COMMENT '文件URL',
  file_size BIGINT DEFAULT 0 COMMENT '文件大小（字节）',
  file_type VARCHAR(50) DEFAULT '' COMMENT '文件类型：image/document/other',
  mime_type VARCHAR(100) DEFAULT '' COMMENT 'MIME类型',
  related_type VARCHAR(50) DEFAULT NULL COMMENT '关联类型：MEMBER/CHRONICLE',
  related_id BIGINT DEFAULT NULL COMMENT '关联ID',
  uploaded_by BIGINT DEFAULT NULL COMMENT '上传人ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  KEY idx_related (related_type, related_id)
) ENGINE=InnoDB COMMENT='附件表';

-- ============================================
-- 初始数据
-- ============================================

-- 插入超级管理员（密码: admin123，BCrypt加密）
INSERT INTO sys_user (username, password, nickname, role, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '超级管理员', 'SUPER_ADMIN', 1);

-- 插入默认系统配置
INSERT INTO sys_config (config_key, config_value, description) VALUES
('family_name', '张氏家族', '家族名称'),
('hall_name', '百忍堂', '堂号'),
('generation_chars', '仁义礼智信温良恭俭让', '字辈排列'),
('announcement', '', '系统公告');

-- 插入示例族谱数据（始祖）
INSERT INTO family_member (id, name, gender, generation, branch, parent_id, birth_date, biography, sort_order) VALUES
(1, '张始祖', 'M', 1, '总支', NULL, '1800-01-01', '家族始祖，开基立业', 1);

-- 始祖的闭包表记录（自身到自身，深度0）
INSERT INTO member_closure (ancestor_id, descendant_id, depth) VALUES
(1, 1, 0);

-- 插入二世祖示例
INSERT INTO family_member (id, name, gender, generation, branch, parent_id, birth_date, sort_order) VALUES
(2, '张大房', 'M', 2, '大房', 1, '1825-03-15', 1),
(3, '张二房', 'M', 2, '二房', 1, '1828-07-20', 2);

-- 二世祖闭包表
INSERT INTO member_closure (ancestor_id, descendant_id, depth) VALUES
(2, 2, 0), (1, 2, 1),
(3, 3, 0), (1, 3, 1);

-- 插入三世示例
INSERT INTO family_member (id, name, gender, generation, branch, parent_id, birth_date, sort_order) VALUES
(4, '张仁一', 'M', 3, '大房', 2, '1850-05-10', 1),
(5, '张仁二', 'M', 3, '大房', 2, '1853-09-22', 2),
(6, '张仁三', 'M', 3, '二房', 3, '1855-11-08', 1);

-- 三世闭包表
INSERT INTO member_closure (ancestor_id, descendant_id, depth) VALUES
(4, 4, 0), (2, 4, 1), (1, 4, 2),
(5, 5, 0), (2, 5, 1), (1, 5, 2),
(6, 6, 0), (3, 6, 1), (1, 6, 2);
