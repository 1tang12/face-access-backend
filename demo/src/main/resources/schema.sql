CREATE TABLE IF NOT EXISTS `visitor_appointment` (
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
  KEY `idx_valid_end_time` (`valid_end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访客预约表';

CREATE TABLE IF NOT EXISTS `attendance_record` (
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
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤记录表';

CREATE TABLE IF NOT EXISTS `attendance_rule` (
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

ALTER TABLE `access_record`
  ADD COLUMN IF NOT EXISTS `match_distance` DECIMAL(6,3) COMMENT '匹配距离';

ALTER TABLE `face_feature`
  ADD COLUMN IF NOT EXISTS `feature_version` VARCHAR(20) DEFAULT 'v1.0' COMMENT '特征提取模型版本';

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

-- 补充访客演示账号，避免访客混入普通用户后缺少单独数据
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `gender`, `phone`, `email`, `dept_id`, `employee_no`, `status`, `remark`)
SELECT 'guest_demo_01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李悦', 0, '13800138201', 'guest_demo_01@example.com', 3, 'GST201', 1, '访客演示数据'
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'guest_demo_01');
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `gender`, `phone`, `email`, `dept_id`, `employee_no`, `status`, `remark`)
SELECT 'guest_demo_02', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '周航', 1, '13800138202', 'guest_demo_02@example.com', 3, 'GST202', 1, '访客演示数据'
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'guest_demo_02');
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `gender`, `phone`, `email`, `dept_id`, `employee_no`, `status`, `remark`)
SELECT 'guest_demo_03', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '陈曦', 0, '13800138203', 'guest_demo_03@example.com', 3, 'GST203', 1, '访客演示数据'
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'guest_demo_03');
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `gender`, `phone`, `email`, `dept_id`, `employee_no`, `status`, `remark`)
SELECT 'guest_demo_04', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '许宁', 1, '13800138204', 'guest_demo_04@example.com', 3, 'GST204', 1, '访客演示数据'
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'guest_demo_04');
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `gender`, `phone`, `email`, `dept_id`, `employee_no`, `status`, `remark`)
SELECT 'guest_demo_05', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '宋妍', 0, '13800138205', 'guest_demo_05@example.com', 3, 'GST205', 1, '访客演示数据'
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'guest_demo_05');
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `gender`, `phone`, `email`, `dept_id`, `employee_no`, `status`, `remark`)
SELECT 'guest_demo_06', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '韩璐', 0, '13800138206', 'guest_demo_06@example.com', 3, 'GST206', 1, '访客演示数据'
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'guest_demo_06');

INSERT INTO `sys_user_role` (`user_id`, `role_id`)
SELECT u.id, r.id
FROM `sys_user` u
JOIN `sys_role` r ON r.`role_code` = 'ROLE_GUEST'
WHERE u.`username` IN ('guest_demo_01', 'guest_demo_02', 'guest_demo_03', 'guest_demo_04', 'guest_demo_05', 'guest_demo_06')
  AND NOT EXISTS (
    SELECT 1 FROM `sys_user_role` ur
    WHERE ur.`user_id` = u.`id` AND ur.`role_id` = r.`id`
  );

UPDATE `sys_user` u
JOIN `sys_user_role` ur ON ur.`user_id` = u.`id`
JOIN `sys_role` r ON r.`id` = ur.`role_id`
SET u.`employee_no` = NULL
WHERE r.`role_code` = 'ROLE_GUEST';

-- 补充普通用户考勤记录
INSERT INTO `attendance_record` (`user_id`, `attendance_date`, `check_in_time`, `check_out_time`, `work_duration`, `status`, `late_minutes`, `early_minutes`, `remark`)
SELECT u.id, '2026-05-12', '2026-05-12 08:56:00', '2026-05-12 18:08:00', 552, 'normal', 0, 0, '研发日报提交'
FROM `sys_user` u WHERE u.`username` = 'user'
AND NOT EXISTS (SELECT 1 FROM `attendance_record` ar WHERE ar.`user_id` = u.`id` AND ar.`attendance_date` = '2026-05-12');
INSERT INTO `attendance_record` (`user_id`, `attendance_date`, `check_in_time`, `check_out_time`, `work_duration`, `status`, `late_minutes`, `early_minutes`, `remark`)
SELECT u.id, '2026-05-13', '2026-05-13 09:18:00', '2026-05-13 18:16:00', 538, 'late', 18, 0, '地铁晚点'
FROM `sys_user` u WHERE u.`username` = 'user'
AND NOT EXISTS (SELECT 1 FROM `attendance_record` ar WHERE ar.`user_id` = u.`id` AND ar.`attendance_date` = '2026-05-13');
INSERT INTO `attendance_record` (`user_id`, `attendance_date`, `check_in_time`, `check_out_time`, `work_duration`, `status`, `late_minutes`, `early_minutes`, `remark`)
SELECT u.id, '2026-05-14', '2026-05-14 08:51:00', '2026-05-14 17:42:00', 531, 'early', 0, 18, '外出答辩彩排'
FROM `sys_user` u WHERE u.`username` = 'user01'
AND NOT EXISTS (SELECT 1 FROM `attendance_record` ar WHERE ar.`user_id` = u.`id` AND ar.`attendance_date` = '2026-05-14');
INSERT INTO `attendance_record` (`user_id`, `attendance_date`, `check_in_time`, `check_out_time`, `work_duration`, `status`, `late_minutes`, `early_minutes`, `remark`)
SELECT u.id, '2026-05-15', '2026-05-15 09:02:00', '2026-05-15 18:21:00', 559, 'late', 2, 0, '晨会结束后打卡'
FROM `sys_user` u WHERE u.`username` = 'user02'
AND NOT EXISTS (SELECT 1 FROM `attendance_record` ar WHERE ar.`user_id` = u.`id` AND ar.`attendance_date` = '2026-05-15');
INSERT INTO `attendance_record` (`user_id`, `attendance_date`, `check_in_time`, `check_out_time`, `work_duration`, `status`, `late_minutes`, `early_minutes`, `remark`)
SELECT u.id, '2026-05-16', '2026-05-16 08:47:00', '2026-05-16 18:05:00', 558, 'normal', 0, 0, '周例会资料整理'
FROM `sys_user` u WHERE u.`username` = 'user03'
AND NOT EXISTS (SELECT 1 FROM `attendance_record` ar WHERE ar.`user_id` = u.`id` AND ar.`attendance_date` = '2026-05-16');
INSERT INTO `attendance_record` (`user_id`, `attendance_date`, `check_in_time`, `check_out_time`, `work_duration`, `status`, `late_minutes`, `early_minutes`, `remark`)
SELECT u.id, '2026-05-17', '2026-05-17 09:11:00', '2026-05-17 18:03:00', 532, 'late', 11, 0, '设备调试占用早间时间'
FROM `sys_user` u WHERE u.`username` = 'user04'
AND NOT EXISTS (SELECT 1 FROM `attendance_record` ar WHERE ar.`user_id` = u.`id` AND ar.`attendance_date` = '2026-05-17');
INSERT INTO `attendance_record` (`user_id`, `attendance_date`, `check_in_time`, `check_out_time`, `work_duration`, `status`, `late_minutes`, `early_minutes`, `remark`)
SELECT u.id, '2026-05-18', '2026-05-18 08:58:00', '2026-05-18 17:55:00', 537, 'normal', 0, 5, '下午提前去机房巡检'
FROM `sys_user` u WHERE u.`username` = 'user05'
AND NOT EXISTS (SELECT 1 FROM `attendance_record` ar WHERE ar.`user_id` = u.`id` AND ar.`attendance_date` = '2026-05-18');
INSERT INTO `attendance_record` (`user_id`, `attendance_date`, `check_in_time`, `check_out_time`, `work_duration`, `status`, `late_minutes`, `early_minutes`, `remark`)
SELECT u.id, '2026-05-19', '2026-05-19 08:49:00', '2026-05-19 18:12:00', 563, 'normal', 0, 0, '接口联调'
FROM `sys_user` u WHERE u.`username` = 'user06'
AND NOT EXISTS (SELECT 1 FROM `attendance_record` ar WHERE ar.`user_id` = u.`id` AND ar.`attendance_date` = '2026-05-19');
INSERT INTO `attendance_record` (`user_id`, `attendance_date`, `check_in_time`, `check_out_time`, `work_duration`, `status`, `late_minutes`, `early_minutes`, `remark`)
SELECT u.id, '2026-05-20', '2026-05-20 09:06:00', '2026-05-20 18:14:00', 548, 'late', 6, 0, '早高峰拥堵'
FROM `sys_user` u WHERE u.`username` = 'user07'
AND NOT EXISTS (SELECT 1 FROM `attendance_record` ar WHERE ar.`user_id` = u.`id` AND ar.`attendance_date` = '2026-05-20');
INSERT INTO `attendance_record` (`user_id`, `attendance_date`, `check_in_time`, `check_out_time`, `work_duration`, `status`, `late_minutes`, `early_minutes`, `remark`)
SELECT u.id, '2026-05-12', '2026-05-12 08:54:00', '2026-05-12 18:20:00', 566, 'normal', 0, 0, '设备巡检'
FROM `sys_user` u WHERE u.`username` = 'user08'
AND NOT EXISTS (SELECT 1 FROM `attendance_record` ar WHERE ar.`user_id` = u.`id` AND ar.`attendance_date` = '2026-05-12');
INSERT INTO `attendance_record` (`user_id`, `attendance_date`, `check_in_time`, `check_out_time`, `work_duration`, `status`, `late_minutes`, `early_minutes`, `remark`)
SELECT u.id, '2026-05-13', '2026-05-13 09:24:00', NULL, NULL, 'late', 24, 0, '上午外勤未签退'
FROM `sys_user` u WHERE u.`username` = 'user09'
AND NOT EXISTS (SELECT 1 FROM `attendance_record` ar WHERE ar.`user_id` = u.`id` AND ar.`attendance_date` = '2026-05-13');
INSERT INTO `attendance_record` (`user_id`, `attendance_date`, `check_in_time`, `check_out_time`, `work_duration`, `status`, `late_minutes`, `early_minutes`, `remark`)
SELECT u.id, '2026-05-14', '2026-05-14 08:43:00', '2026-05-14 18:01:00', 558, 'normal', 0, 0, '文档归档'
FROM `sys_user` u WHERE u.`username` = 'user10'
AND NOT EXISTS (SELECT 1 FROM `attendance_record` ar WHERE ar.`user_id` = u.`id` AND ar.`attendance_date` = '2026-05-14');
INSERT INTO `attendance_record` (`user_id`, `attendance_date`, `check_in_time`, `check_out_time`, `work_duration`, `status`, `late_minutes`, `early_minutes`, `remark`)
SELECT u.id, '2026-05-15', '2026-05-15 08:57:00', '2026-05-15 17:48:00', 531, 'early', 0, 12, '医院复查提前离岗'
FROM `sys_user` u WHERE u.`username` = 'user11'
AND NOT EXISTS (SELECT 1 FROM `attendance_record` ar WHERE ar.`user_id` = u.`id` AND ar.`attendance_date` = '2026-05-15');

-- 补充访客预约记录
INSERT INTO `visitor_appointment` (`visitor_user_id`, `visitor_name`, `visitor_phone`, `visit_purpose`, `host_name`, `valid_start_time`, `valid_end_time`, `status`, `review_remark`, `reviewer_id`, `reviewer_name`, `reviewed_at`)
SELECT u.id, u.real_name, u.phone, '毕业设计中期沟通', '系统管理员', '2026-05-16 09:30:00', '2026-05-16 11:30:00', 'approved', '已登记到访，允许入场', 1, 'admin', '2026-05-15 18:20:00'
FROM `sys_user` u WHERE u.`username` = 'guest_demo_01'
AND NOT EXISTS (SELECT 1 FROM `visitor_appointment` va WHERE va.`visitor_user_id` = u.`id` AND va.`visit_purpose` = '毕业设计中期沟通');
INSERT INTO `visitor_appointment` (`visitor_user_id`, `visitor_name`, `visitor_phone`, `visit_purpose`, `host_name`, `valid_start_time`, `valid_end_time`, `status`, `review_remark`, `reviewer_id`, `reviewer_name`, `reviewed_at`)
SELECT u.id, u.real_name, u.phone, '门禁设备调研', '张三', '2026-05-17 14:00:00', '2026-05-17 16:00:00', 'approved', '设备参观已确认', 1, 'admin', '2026-05-16 17:50:00'
FROM `sys_user` u WHERE u.`username` = 'guest_demo_02'
AND NOT EXISTS (SELECT 1 FROM `visitor_appointment` va WHERE va.`visitor_user_id` = u.`id` AND va.`visit_purpose` = '门禁设备调研');
INSERT INTO `visitor_appointment` (`visitor_user_id`, `visitor_name`, `visitor_phone`, `visit_purpose`, `host_name`, `valid_start_time`, `valid_end_time`, `status`, `review_remark`, `reviewer_id`, `reviewer_name`, `reviewed_at`)
SELECT u.id, u.real_name, u.phone, '访客系统体验测试', '员工03', '2026-05-18 10:00:00', '2026-05-18 12:00:00', 'rejected', '该时段接待人不在岗', 1, 'admin', '2026-05-17 19:10:00'
FROM `sys_user` u WHERE u.`username` = 'guest_demo_03'
AND NOT EXISTS (SELECT 1 FROM `visitor_appointment` va WHERE va.`visitor_user_id` = u.`id` AND va.`visit_purpose` = '访客系统体验测试');
INSERT INTO `visitor_appointment` (`visitor_user_id`, `visitor_name`, `visitor_phone`, `visit_purpose`, `host_name`, `valid_start_time`, `valid_end_time`, `status`, `review_remark`, `reviewer_id`, `reviewer_name`, `reviewed_at`)
SELECT u.id, u.real_name, u.phone, '论文答辩资料递交', '员工05', '2026-05-19 13:30:00', '2026-05-19 15:00:00', 'approved', '资料递交通过', 1, 'admin', '2026-05-18 18:40:00'
FROM `sys_user` u WHERE u.`username` = 'guest_demo_04'
AND NOT EXISTS (SELECT 1 FROM `visitor_appointment` va WHERE va.`visitor_user_id` = u.`id` AND va.`visit_purpose` = '论文答辩资料递交');
INSERT INTO `visitor_appointment` (`visitor_user_id`, `visitor_name`, `visitor_phone`, `visit_purpose`, `host_name`, `valid_start_time`, `valid_end_time`, `status`, `review_remark`)
SELECT u.id, u.real_name, u.phone, '展厅路线确认', '员工07', '2026-05-21 09:00:00', '2026-05-21 10:30:00', 'pending', '等待接待人确认'
FROM `sys_user` u WHERE u.`username` = 'guest_demo_05'
AND NOT EXISTS (SELECT 1 FROM `visitor_appointment` va WHERE va.`visitor_user_id` = u.`id` AND va.`visit_purpose` = '展厅路线确认');
INSERT INTO `visitor_appointment` (`visitor_user_id`, `visitor_name`, `visitor_phone`, `visit_purpose`, `host_name`, `valid_start_time`, `valid_end_time`, `status`, `review_remark`)
SELECT u.id, u.real_name, u.phone, '门禁识别复测', '员工09', '2026-05-21 14:00:00', '2026-05-21 17:00:00', 'pending', '等待审核'
FROM `sys_user` u WHERE u.`username` = 'guest_demo_06'
AND NOT EXISTS (SELECT 1 FROM `visitor_appointment` va WHERE va.`visitor_user_id` = u.`id` AND va.`visit_purpose` = '门禁识别复测');
