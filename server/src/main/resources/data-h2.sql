-- 初始数据（H2 本地开发用）

-- 超级管理员（密码: admin123）
INSERT INTO sys_user (username, password, nickname, role, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '超级管理员', 'SUPER_ADMIN', 1);

-- 系统配置
INSERT INTO sys_config (config_key, config_value, description) VALUES
('family_name', '张氏家族', '家族名称'),
('hall_name', '百忍堂', '堂号'),
('generation_chars', '仁义礼智信温良恭俭让', '字辈排列'),
('announcement', '', '系统公告');

-- 示例族谱数据
INSERT INTO family_member (id, name, gender, generation, branch, parent_id, birth_date, biography, sort_order) VALUES
(1, '张始祖', 'M', 1, '总支', NULL, '1800-01-01', '家族始祖，开基立业', 1);

INSERT INTO member_closure (ancestor_id, descendant_id, depth) VALUES
(1, 1, 0);

INSERT INTO family_member (id, name, gender, generation, branch, parent_id, birth_date, sort_order) VALUES
(2, '张大房', 'M', 2, '大房', 1, '1825-03-15', 1),
(3, '张二房', 'M', 2, '二房', 1, '1828-07-20', 2);

INSERT INTO member_closure (ancestor_id, descendant_id, depth) VALUES
(2, 2, 0), (1, 2, 1),
(3, 3, 0), (1, 3, 1);

INSERT INTO family_member (id, name, gender, generation, branch, parent_id, birth_date, sort_order) VALUES
(4, '张仁一', 'M', 3, '大房', 2, '1850-05-10', 1),
(5, '张仁二', 'M', 3, '大房', 2, '1853-09-22', 2),
(6, '张仁三', 'M', 3, '二房', 3, '1855-11-08', 1);

INSERT INTO member_closure (ancestor_id, descendant_id, depth) VALUES
(4, 4, 0), (2, 4, 1), (1, 4, 2),
(5, 5, 0), (2, 5, 1), (1, 5, 2),
(6, 6, 0), (3, 6, 1), (1, 6, 2);
