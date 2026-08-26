-- H2 兼容的建表脚本（本地开发用）

-- 1. 系统用户表
CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  openid VARCHAR(64) DEFAULT NULL,
  union_id VARCHAR(64) DEFAULT NULL,
  username VARCHAR(50) DEFAULT NULL,
  password VARCHAR(255) DEFAULT NULL,
  nickname VARCHAR(50) DEFAULT '',
  avatar_url VARCHAR(500) DEFAULT '',
  phone VARCHAR(20) DEFAULT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'GUEST',
  member_id BIGINT DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  last_login_at TIMESTAMP DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_openid ON sys_user(openid);
CREATE UNIQUE INDEX IF NOT EXISTS uk_username ON sys_user(username);

-- 2. 族谱成员表
CREATE TABLE IF NOT EXISTS family_member (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  alias_name VARCHAR(50) DEFAULT '',
  generation_char VARCHAR(10) DEFAULT '',
  gender VARCHAR(2) NOT NULL DEFAULT 'M',
  generation INT NOT NULL DEFAULT 1,
  branch VARCHAR(50) DEFAULT '',
  parent_id BIGINT DEFAULT NULL,
  spouse_id BIGINT DEFAULT NULL,
  birth_date DATE DEFAULT NULL,
  death_date DATE DEFAULT NULL,
  birth_place VARCHAR(200) DEFAULT '',
  biography CLOB,
  avatar_url VARCHAR(500) DEFAULT '',
  sort_order INT NOT NULL DEFAULT 0,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by BIGINT DEFAULT NULL
);

CREATE INDEX IF NOT EXISTS idx_fm_parent_id ON family_member(parent_id);
CREATE INDEX IF NOT EXISTS idx_fm_generation ON family_member(generation);
CREATE INDEX IF NOT EXISTS idx_fm_name ON family_member(name);

-- 3. 成员闭包表
CREATE TABLE IF NOT EXISTS member_closure (
  ancestor_id BIGINT NOT NULL,
  descendant_id BIGINT NOT NULL,
  depth INT NOT NULL DEFAULT 0,
  PRIMARY KEY (ancestor_id, descendant_id)
);

CREATE INDEX IF NOT EXISTS idx_mc_descendant ON member_closure(descendant_id, depth);
CREATE INDEX IF NOT EXISTS idx_mc_ancestor ON member_closure(ancestor_id, depth);

-- 4. 审核记录表
CREATE TABLE IF NOT EXISTS audit_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  submitter_id BIGINT NOT NULL,
  target_type VARCHAR(50) NOT NULL,
  target_id BIGINT DEFAULT NULL,
  action VARCHAR(20) NOT NULL,
  before_data CLOB DEFAULT NULL,
  after_data CLOB NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  reviewer_id BIGINT DEFAULT NULL,
  review_comment CLOB,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reviewed_at TIMESTAMP DEFAULT NULL
);

CREATE INDEX IF NOT EXISTS idx_ar_status ON audit_record(status);
CREATE INDEX IF NOT EXISTS idx_ar_submitter ON audit_record(submitter_id);

-- 5. 地方志表
CREATE TABLE IF NOT EXISTS local_chronicle (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  category VARCHAR(50) NOT NULL DEFAULT '其他',
  content CLOB,
  cover_url VARCHAR(500) DEFAULT '',
  sort_order INT NOT NULL DEFAULT 0,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  created_by BIGINT DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. 任职履历表
CREATE TABLE IF NOT EXISTS career_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  member_id BIGINT NOT NULL,
  position VARCHAR(100) NOT NULL,
  organization VARCHAR(200) DEFAULT '',
  start_date DATE DEFAULT NULL,
  end_date DATE DEFAULT NULL,
  description CLOB,
  sort_order INT NOT NULL DEFAULT 0,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 7. 操作日志表
CREATE TABLE IF NOT EXISTS operation_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  user_name VARCHAR(50) DEFAULT '',
  module VARCHAR(50) NOT NULL,
  action VARCHAR(50) NOT NULL,
  target_type VARCHAR(50) DEFAULT NULL,
  target_id BIGINT DEFAULT NULL,
  description VARCHAR(500) DEFAULT '',
  request_ip VARCHAR(50) DEFAULT '',
  request_data CLOB DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 8. 系统配置表
CREATE TABLE IF NOT EXISTS sys_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  config_key VARCHAR(100) NOT NULL,
  config_value CLOB,
  description VARCHAR(200) DEFAULT '',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_config_key ON sys_config(config_key);

-- 9. 消息通知表
CREATE TABLE IF NOT EXISTS message_notification (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  content VARCHAR(500) DEFAULT '',
  type VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
  related_id BIGINT DEFAULT NULL,
  is_read TINYINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 10. 附件表
CREATE TABLE IF NOT EXISTS attachment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  file_name VARCHAR(200) NOT NULL,
  file_url VARCHAR(500) NOT NULL,
  file_size BIGINT DEFAULT 0,
  file_type VARCHAR(50) DEFAULT '',
  mime_type VARCHAR(100) DEFAULT '',
  related_type VARCHAR(50) DEFAULT NULL,
  related_id BIGINT DEFAULT NULL,
  uploaded_by BIGINT DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 11. 字辈诗/字辈歌表
CREATE TABLE IF NOT EXISTS sys_generation_poetry (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  surname VARCHAR(50) NOT NULL COMMENT '姓氏',
  branch_name VARCHAR(100) DEFAULT NULL COMMENT '分支名称',
  title VARCHAR(200) NOT NULL COMMENT '字辈诗标题',
  generation_sequence TEXT NOT NULL COMMENT '字辈序列',
  interpretation TEXT COMMENT '字辈释义',
  start_generation INT DEFAULT 1 COMMENT '起始世代',
  enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
  creator_id BIGINT DEFAULT NULL COMMENT '创建人ID',
  remark TEXT COMMENT '备注',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
);
