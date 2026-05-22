-- ========================================
-- 人脸识别门禁系统数据库设计
-- ========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS face_access_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE face_access_system;

-- ========================================
-- 1. 用户表
-- ========================================
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(50) NOT NULL COMMENT '登录账号',
  `password` VARCHAR(255) NOT NULL COMMENT '加密密码',
  `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
  `gender` TINYINT DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
  `phone` VARCHAR(20) COMMENT '手机号',
  `email` VARCHAR(100) COMMENT '邮箱',
  `dept_id` BIGINT COMMENT '部门ID',
  `employee_no` VARCHAR(50) COMMENT '工号',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `avatar` VARCHAR(255) COMMENT '头像路径',
  `remark` VARCHAR(500) COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(50) COMMENT '创建人',
  `update_by` VARCHAR(50) COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ========================================
-- 2. 角色表
-- ========================================
CREATE TABLE `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `description` VARCHAR(200) COMMENT '角色描述',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- ========================================
-- 3. 用户角色关联表
-- ========================================
CREATE TABLE `sys_user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`),
  CONSTRAINT `fk_sys_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `fk_sys_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ========================================
-- 4. 权限表
-- ========================================
CREATE TABLE `sys_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `permission_name` VARCHAR(50) NOT NULL COMMENT '权限名称',
  `permission_code` VARCHAR(100) NOT NULL COMMENT '权限编码',
  `type` TINYINT NOT NULL COMMENT '类型：1-菜单，2-按钮，3-API',
  `parent_id` BIGINT DEFAULT NULL COMMENT '父级ID',
  `path` VARCHAR(200) COMMENT '路由路径',
  `component` VARCHAR(200) COMMENT '组件路径',
  `icon` VARCHAR(50) COMMENT '图标',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`),
  KEY `idx_parent_id` (`parent_id`),
  CONSTRAINT `fk_sys_permission_parent` FOREIGN KEY (`parent_id`) REFERENCES `sys_permission` (`id`)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限表';

-- ========================================
-- 5. 角色权限关联表
-- ========================================
CREATE TABLE `sys_role_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `permission_id` BIGINT NOT NULL COMMENT '权限ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_permission_id` (`permission_id`),
  CONSTRAINT `fk_sys_role_permission_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `fk_sys_role_permission_permission` FOREIGN KEY (`permission_id`) REFERENCES `sys_permission` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- ========================================
-- 6. 人脸特征表（核心表）
-- ========================================
CREATE TABLE `face_feature` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `face_image_path` VARCHAR(255) NOT NULL COMMENT '人脸图片路径',
  `feature_vector` TEXT COMMENT '人脸特征向量（JSON格式存储）',
  `feature_version` VARCHAR(20) DEFAULT 'v1.0' COMMENT '特征提取模型版本',
  `quality_score` DECIMAL(5,2) COMMENT '人脸质量分数',
  `register_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-失效，1-有效',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_face_feature_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人脸特征表';

-- ========================================
-- 7. 门禁设备表
-- ========================================
CREATE TABLE `access_device` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_name` VARCHAR(100) NOT NULL COMMENT '设备名称',
  `device_code` VARCHAR(50) NOT NULL COMMENT '设备编码',
  `location` VARCHAR(200) COMMENT '设备位置',
  `device_type` VARCHAR(50) DEFAULT 'virtual' COMMENT '设备类型：virtual-虚拟设备，physical-物理设备',
  `ip_address` VARCHAR(50) COMMENT 'IP地址',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-离线，1-在线',
  `description` VARCHAR(500) COMMENT '描述',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_code` (`device_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门禁设备表';

-- ========================================
-- 8. 访问记录表（核心业务表）
-- ========================================
CREATE TABLE `access_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT COMMENT '用户ID（识别成功时有值）',
  `device_id` BIGINT NOT NULL COMMENT '设备ID',
  `access_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
  `result` VARCHAR(20) NOT NULL COMMENT '识别结果：success-成功，fail-失败',
  `similarity_score` DECIMAL(5,2) COMMENT '相似度分数',
  `match_distance` DECIMAL(6,3) COMMENT '匹配距离',
  `threshold` DECIMAL(5,2) DEFAULT 0.80 COMMENT '识别阈值',
  `snapshot_path` VARCHAR(255) COMMENT '现场抓拍图片路径',
  `fail_reason` VARCHAR(200) COMMENT '失败原因',
  `temperature` DECIMAL(4,1) COMMENT '体温（可选功能）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_access_time` (`access_time`),
  KEY `idx_result` (`result`),
  CONSTRAINT `fk_access_record_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
    ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT `fk_access_record_device` FOREIGN KEY (`device_id`) REFERENCES `access_device` (`id`)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访问记录表';

-- ========================================
-- 9. 考勤记录表
-- ========================================
CREATE TABLE `attendance_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `attendance_date` DATE NOT NULL COMMENT '考勤日期',
  `check_in_time` DATETIME COMMENT '签到时间',
  `check_out_time` DATETIME COMMENT '签退时间',
  `work_duration` INT COMMENT '工作时长（分钟）',
  `status` VARCHAR(20) DEFAULT 'normal' COMMENT '状态：normal-正常，late-迟到，early-早退，absent-缺勤',
  `late_minutes` INT DEFAULT 0 COMMENT '迟到分钟数',
  `early_minutes` INT DEFAULT 0 COMMENT '早退分钟数',
  `remark` VARCHAR(500) COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`, `attendance_date`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_attendance_date` (`attendance_date`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_attendance_record_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤记录表';

-- ========================================
-- 9.1 考勤规则表
-- ========================================
CREATE TABLE `attendance_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `attendance_date` DATE NOT NULL COMMENT '考勤日期',
  `need_attendance` TINYINT NOT NULL DEFAULT 1 COMMENT '是否需要考勤：1-需要，0-不需要',
  `check_in_start_time` TIME NOT NULL DEFAULT '07:00:00' COMMENT '签到开始时间',
  `check_in_end_time` TIME NOT NULL DEFAULT '10:00:00' COMMENT '签到结束时间',
  `work_start_time` TIME NOT NULL DEFAULT '09:00:00' COMMENT '上班时间',
  `work_end_time` TIME NOT NULL DEFAULT '18:00:00' COMMENT '下班时间',
  `check_out_start_time` TIME NOT NULL DEFAULT '17:00:00' COMMENT '签退开始时间',
  `check_out_end_time` TIME NOT NULL DEFAULT '20:00:00' COMMENT '签退结束时间',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '规则状态：1-启用，0-停用',
  `remark` VARCHAR(200) COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_attendance_rule_date` (`attendance_date`),
  KEY `idx_attendance_rule_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤规则表';

-- ========================================
-- 10. 操作日志表
-- ========================================
CREATE TABLE `operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT COMMENT '操作用户ID',
  `username` VARCHAR(50) COMMENT '操作用户名',
  `operation_type` VARCHAR(50) NOT NULL COMMENT '操作类型',
  `module_name` VARCHAR(50) COMMENT '模块名称',
  `operation_content` VARCHAR(500) COMMENT '操作内容',
  `request_method` VARCHAR(10) COMMENT '请求方法',
  `request_url` VARCHAR(200) COMMENT '请求URL',
  `request_params` TEXT COMMENT '请求参数',
  `ip_address` VARCHAR(50) COMMENT 'IP地址',
  `location` VARCHAR(100) COMMENT '操作地点',
  `browser` VARCHAR(100) COMMENT '浏览器',
  `os` VARCHAR(100) COMMENT '操作系统',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-失败，1-成功',
  `error_msg` TEXT COMMENT '错误信息',
  `operate_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_operate_time` (`operate_time`),
  KEY `idx_module_name` (`module_name`),
  CONSTRAINT `fk_operation_log_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ========================================
-- 11. 访客预约表
-- ========================================
CREATE TABLE `visitor_appointment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `visitor_user_id` BIGINT NOT NULL COMMENT '访客用户ID',
  `visitor_name` VARCHAR(50) NOT NULL COMMENT '访客姓名',
  `visitor_phone` VARCHAR(20) COMMENT '访客手机号',
  `visit_purpose` VARCHAR(255) NOT NULL COMMENT '来访事由',
  `host_name` VARCHAR(50) COMMENT '接待人',
  `valid_start_time` DATETIME NOT NULL COMMENT '有效开始时间',
  `valid_end_time` DATETIME NOT NULL COMMENT '有效结束时间',
  `status` VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending-待审核，approved-已通过，rejected-已拒绝',
  `review_remark` VARCHAR(255) COMMENT '审核备注',
  `reviewer_id` BIGINT COMMENT '审核人ID',
  `reviewer_name` VARCHAR(50) COMMENT '审核人姓名',
  `reviewed_at` DATETIME COMMENT '审核时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_visitor_user_id` (`visitor_user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_valid_start_time` (`valid_start_time`),
  KEY `idx_valid_end_time` (`valid_end_time`),
  CONSTRAINT `fk_visitor_appointment_visitor` FOREIGN KEY (`visitor_user_id`) REFERENCES `sys_user` (`id`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `fk_visitor_appointment_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `sys_user` (`id`)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访客预约表';

-- ========================================
-- 初始化数据
-- ========================================

-- 插入默认角色
INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `description`, `status`) VALUES
(1, '超级管理员', 'ROLE_ADMIN', '拥有系统所有权限', 1),
(2, '普通用户', 'ROLE_USER', '普通员工用户', 1),
(3, '访客', 'ROLE_GUEST', '访客用户', 1);

-- 插入默认用户（密码：123456，需要BCrypt加密）
-- 注意：实际使用时需要用BCrypt加密密码
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `gender`, `phone`, `email`, `dept_id`, `employee_no`, `status`) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 1, '13800138000', 'admin@example.com', 1, 'EMP001', 1),
(2, 'user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张三', 1, '13800138001', 'zhangsan@example.com', 2, 'EMP002', 1),
(3, 'guest', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '访客', 0, '13800138002', 'guest@example.com', 3, 'EMP003', 1);

-- 批量演示用户（密码统一为123456）
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `gender`, `phone`, `email`, `dept_id`, `employee_no`, `status`, `remark`) VALUES
('user01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '员工01', 1, '13800138101', 'user01@example.com', 2, 'EMP101', 1, '批量初始化普通用户'),
('user02', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '员工02', 1, '13800138102', 'user02@example.com', 2, 'EMP102', 1, '批量初始化普通用户'),
('user03', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '员工03', 1, '13800138103', 'user03@example.com', 2, 'EMP103', 1, '批量初始化普通用户'),
('user04', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '员工04', 1, '13800138104', 'user04@example.com', 2, 'EMP104', 1, '批量初始化普通用户'),
('user05', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '员工05', 1, '13800138105', 'user05@example.com', 2, 'EMP105', 1, '批量初始化普通用户'),
('user06', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '员工06', 1, '13800138106', 'user06@example.com', 2, 'EMP106', 1, '批量初始化普通用户'),
('user07', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '员工07', 1, '13800138107', 'user07@example.com', 2, 'EMP107', 1, '批量初始化普通用户'),
('user08', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '员工08', 1, '13800138108', 'user08@example.com', 2, 'EMP108', 1, '批量初始化普通用户'),
('user09', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '员工09', 1, '13800138109', 'user09@example.com', 2, 'EMP109', 1, '批量初始化普通用户'),
('user10', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '员工10', 1, '13800138110', 'user10@example.com', 2, 'EMP110', 1, '批量初始化普通用户'),
('user11', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '员工11', 1, '13800138111', 'user11@example.com', 2, 'EMP111', 1, '批量初始化普通用户'),
('user12', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '员工12', 1, '13800138112', 'user12@example.com', 2, 'EMP112', 1, '批量初始化普通用户'),
('user13', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '员工13', 1, '13800138113', 'user13@example.com', 2, 'EMP113', 1, '批量初始化普通用户'),
('user14', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '员工14', 1, '13800138114', 'user14@example.com', 2, 'EMP114', 1, '批量初始化普通用户'),
('user15', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '员工15', 1, '13800138115', 'user15@example.com', 2, 'EMP115', 1, '批量初始化普通用户'),
('user16', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '员工16', 1, '13800138116', 'user16@example.com', 2, 'EMP116', 1, '批量初始化普通用户'),
('user17', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '员工17', 1, '13800138117', 'user17@example.com', 2, 'EMP117', 1, '批量初始化普通用户'),
('user18', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '员工18', 1, '13800138118', 'user18@example.com', 2, 'EMP118', 1, '批量初始化普通用户'),
('user19', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '员工19', 1, '13800138119', 'user19@example.com', 2, 'EMP119', 1, '批量初始化普通用户'),
('user20', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '员工20', 1, '13800138120', 'user20@example.com', 2, 'EMP120', 1, '批量初始化普通用户');

-- 插入用户角色关联
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1),
(2, 2),
(3, 3);

INSERT INTO `sys_user_role` (`user_id`, `role_id`)
SELECT id, 2 FROM `sys_user` WHERE username IN (
'user01','user02','user03','user04','user05','user06','user07','user08','user09','user10',
'user11','user12','user13','user14','user15','user16','user17','user18','user19','user20'
);

-- 插入默认权限
INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `status`) VALUES
-- 一级菜单
(1, '系统管理', 'system', 1, NULL, '/system', NULL, 'Setting', 1, 1),
(2, '人脸管理', 'face', 1, NULL, '/face', NULL, 'User', 2, 1),
(3, '门禁管理', 'access', 1, NULL, '/access', NULL, 'Lock', 3, 1),
(4, '考勤管理', 'attendance', 1, NULL, '/attendance', NULL, 'Calendar', 4, 1),
(5, '日志管理', 'log', 1, NULL, '/log', NULL, 'Document', 5, 1),

-- 系统管理子菜单
(11, '用户管理', 'system:user', 1, 1, '/system/user', 'system/user/UserList', 'User', 1, 1),
(12, '角色管理', 'system:role', 1, 1, '/system/role', 'system/role/RoleList', 'UserFilled', 2, 1),
(13, '权限管理', 'system:permission', 1, 1, '/system/permission', 'system/permission/PermissionList', 'Key', 3, 1),
(14, '部门管理', 'system:dept', 1, 1, '/system/dept', 'system/dept/DeptList', 'OfficeBuilding', 4, 1),

-- 人脸管理子菜单
(21, '人脸注册', 'face:register', 1, 2, '/face/register', 'face/FaceRegister', 'Camera', 1, 1),
(22, '人脸列表', 'face:list', 1, 2, '/face/list', 'face/FaceList', 'Picture', 2, 1),

-- 门禁管理子菜单
(31, '门禁识别', 'access:verify', 1, 3, '/access/verify', 'access/AccessVerify', 'VideoCamera', 1, 1),
(32, '访问记录', 'access:record', 1, 3, '/access/record', 'access/AccessRecord', 'List', 2, 1),
(33, '设备管理', 'access:device', 1, 3, '/access/device', 'access/DeviceList', 'Monitor', 3, 1),

-- 考勤管理子菜单
(41, '考勤统计', 'attendance:stats', 1, 4, '/attendance/stats', 'attendance/AttendanceStats', 'DataAnalysis', 1, 1),
(42, '考勤记录', 'attendance:record', 1, 4, '/attendance/record', 'attendance/AttendanceRecord', 'Tickets', 2, 1),

-- 日志管理子菜单
(51, '操作日志', 'log:operation', 1, 5, '/log/operation', 'log/OperationLog', 'Edit', 1, 1),
(52, '登录日志', 'log:login', 1, 5, '/log/login', 'log/LoginLog', 'Connection', 2, 1);

-- 插入角色权限关联（管理员拥有所有权限）
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) 
SELECT 1, id FROM sys_permission;

-- 插入默认门禁设备
INSERT INTO `access_device` (`id`, `device_name`, `device_code`, `location`, `device_type`, `status`, `description`) VALUES
(1, '主入口门禁', 'DEVICE_001', '公司大门', 'virtual', 1, '公司主入口虚拟门禁设备'),
(2, '技术部门禁', 'DEVICE_002', '技术部办公区', 'virtual', 1, '技术部虚拟门禁设备'),
(3, '会议室门禁', 'DEVICE_003', '一楼会议室', 'virtual', 1, '会议室虚拟门禁设备');

-- 回填历史用户的联系方式，确保现有用户均包含手机号与邮箱
UPDATE `sys_user`
SET
  `phone` = CASE
    WHEN `phone` IS NULL OR TRIM(`phone`) = '' THEN CONCAT('139', LPAD(`id`, 8, '0'))
    ELSE `phone`
  END,
  `email` = CASE
    WHEN `email` IS NULL OR TRIM(`email`) = '' THEN CONCAT(`username`, '@example.com')
    ELSE `email`
  END;

-- ========================================
-- 完成提示
-- ========================================
SELECT '数据库初始化完成！' AS message;
SELECT '默认管理员账号：admin，密码：123456' AS info;
