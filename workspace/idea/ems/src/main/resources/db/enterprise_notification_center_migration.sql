-- 企业通知中心核心表与个人收件箱权限。脚本可重复执行。
SET @notification_time = CURRENT_TIMESTAMP;
SET @notification_admin_user_id = (
  SELECT `user_id` FROM `sys_user`
  WHERE `username` = 'admin' AND `deleted_flag` = 0
  ORDER BY `user_id` LIMIT 1
);

CREATE TABLE IF NOT EXISTS `sys_notification` (
  `notification_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '通知事件主键。',
  `event_key` VARCHAR(191) NOT NULL COMMENT '全局唯一事件幂等键。',
  `event_type` VARCHAR(64) NOT NULL COMMENT '稳定事件类型编码。',
  `category` VARCHAR(32) NOT NULL COMMENT 'SYSTEM、MFG、PLAN、QMS、WMS、EAM。',
  `severity` VARCHAR(32) NOT NULL COMMENT 'INFO、WARNING、HIGH、CRITICAL。',
  `title` VARCHAR(128) NOT NULL COMMENT '通知标题，纯文本。',
  `summary` VARCHAR(500) NULL COMMENT '通知摘要，纯文本。',
  `biz_type` VARCHAR(64) NULL COMMENT '关联业务类型。',
  `biz_id` BIGINT UNSIGNED NULL COMMENT '关联业务主键。',
  `biz_no` VARCHAR(64) NULL COMMENT '关联业务编号快照。',
  `route_path` VARCHAR(255) NULL COMMENT '后端白名单生成的前端站内路由。',
  `source_user_id` BIGINT UNSIGNED NULL COMMENT '事件操作用户。',
  `source_employee_id` BIGINT UNSIGNED NULL COMMENT '事件操作员工。',
  `occurred_time` DATETIME NOT NULL COMMENT '事件发生时间。',
  `expire_time` DATETIME NULL COMMENT '通知过期时间。',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE、CANCELLED、EXPIRED。',
  `payload_json` TEXT NULL COMMENT '仅供展示的非敏感 JSON 快照。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`notification_id`),
  UNIQUE KEY `uk_sys_notification_event_key` (`event_key`),
  KEY `idx_sys_notification_biz` (`biz_type`, `biz_id`),
  KEY `idx_sys_notification_occurred_time` (`occurred_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知事件表';

CREATE TABLE IF NOT EXISTS `sys_notification_recipient` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '通知接收记录主键。',
  `notification_id` BIGINT UNSIGNED NOT NULL COMMENT '通知事件主键。',
  `recipient_user_id` BIGINT UNSIGNED NOT NULL COMMENT '接收账号。',
  `recipient_employee_id` BIGINT UNSIGNED NULL COMMENT '接收员工快照。',
  `recipient_reason` VARCHAR(32) NOT NULL COMMENT 'ASSIGNEE、REQUESTER、TEAM_LEADER、RESOURCE_OWNER、SUBSCRIPTION、ESCALATION。',
  `read_status` VARCHAR(32) NOT NULL DEFAULT 'UNREAD' COMMENT 'UNREAD、READ。',
  `read_time` DATETIME NULL COMMENT '已读时间。',
  `ack_status` VARCHAR(32) NOT NULL DEFAULT 'NOT_REQUIRED' COMMENT 'NOT_REQUIRED、PENDING、ACKNOWLEDGED。',
  `ack_time` DATETIME NULL COMMENT '确认时间。',
  `delivery_status` VARCHAR(32) NOT NULL DEFAULT 'DELIVERED' COMMENT 'DELIVERED、FAILED。',
  `delivered_time` DATETIME NULL COMMENT '站内投递时间。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_notification_recipient` (`notification_id`, `recipient_user_id`),
  KEY `idx_sys_notification_recipient_inbox` (`recipient_user_id`, `read_status`, `created_time`),
  KEY `idx_sys_notification_recipient_notification` (`notification_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知接收记录表';

CREATE TABLE IF NOT EXISTS `sys_event_outbox` (
  `outbox_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Outbox 主键。',
  `event_key` VARCHAR(191) NOT NULL COMMENT '全局唯一事件幂等键。',
  `aggregate_type` VARCHAR(64) NOT NULL COMMENT '业务聚合类型。',
  `aggregate_id` BIGINT UNSIGNED NULL COMMENT '业务聚合主键。',
  `event_type` VARCHAR(64) NOT NULL COMMENT '事件类型编码。',
  `payload_json` TEXT NOT NULL COMMENT '接收人解析所需的非敏感事件数据。',
  `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING、PROCESSING、PROCESSED、FAILED、CANCELLED。',
  `retry_count` INT NOT NULL DEFAULT 0 COMMENT '处理失败重试次数。',
  `next_retry_time` DATETIME NULL COMMENT '下次允许重试时间。',
  `last_error` VARCHAR(500) NULL COMMENT '最近一次处理错误摘要。',
  `occurred_time` DATETIME NOT NULL COMMENT '业务事件发生时间。',
  `processed_time` DATETIME NULL COMMENT '处理完成时间。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`outbox_id`),
  UNIQUE KEY `uk_sys_event_outbox_event_key` (`event_key`),
  KEY `idx_sys_event_outbox_pending` (`status`, `next_retry_time`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='业务事件 Outbox 表';

START TRANSACTION;

INSERT INTO `sys_menu`
  (`parent_id`, `menu_name`, `menu_type`, `permission_code`, `route_path`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT directory.`menu_id`, '消息中心', 'API', 'sys:notification:view', '/notifications',
       'ENTERPRISE_NOTIFICATION_V1: 个人消息中心', 0,
       @notification_admin_user_id, @notification_time, @notification_admin_user_id, @notification_time, 0
FROM `sys_menu` directory
WHERE directory.`permission_code` = 'module:sys:view' AND directory.`deleted_flag` = 0
LIMIT 1
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`), `menu_name` = VALUES(`menu_name`), `menu_type` = 'API',
  `route_path` = VALUES(`route_path`), `remark` = VALUES(`remark`),
  `updated_by` = @notification_admin_user_id, `updated_time` = @notification_time, `deleted_flag` = 0;

DROP TEMPORARY TABLE IF EXISTS `tmp_notification_permission`;
CREATE TEMPORARY TABLE `tmp_notification_permission` (
  `permission_code` VARCHAR(64) PRIMARY KEY,
  `permission_name` VARCHAR(128) NOT NULL
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
INSERT INTO `tmp_notification_permission` VALUES
  ('sys:notification:read','标记本人通知已读'),
  ('sys:notification:ack','确认本人通知'),
  ('sys:notification:audit','查看通知投递审计');

INSERT INTO `sys_menu`
  (`parent_id`, `menu_name`, `menu_type`, `permission_code`, `route_path`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT parent.`menu_id`, permission.`permission_name`, 'BUTTON', permission.`permission_code`, NULL,
       'ENTERPRISE_NOTIFICATION_V1: 个人收件箱与投递审计', 0,
       @notification_admin_user_id, @notification_time, @notification_admin_user_id, @notification_time, 0
FROM `tmp_notification_permission` permission
JOIN `sys_menu` parent
  ON parent.`permission_code` = 'sys:notification:view' AND parent.`deleted_flag` = 0
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`), `menu_name` = VALUES(`menu_name`), `menu_type` = 'BUTTON',
  `remark` = VALUES(`remark`), `updated_by` = @notification_admin_user_id,
  `updated_time` = @notification_time, `deleted_flag` = 0;

DROP TEMPORARY TABLE IF EXISTS `tmp_notification_role_permission`;
CREATE TEMPORARY TABLE `tmp_notification_role_permission` (
  `role_code` VARCHAR(64) NOT NULL,
  `permission_code` VARCHAR(64) NOT NULL,
  PRIMARY KEY (`role_code`, `permission_code`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

INSERT INTO `tmp_notification_role_permission`
SELECT role.`role_code`, permission.`permission_code`
FROM `sys_role` role
CROSS JOIN (
  SELECT 'sys:notification:view' AS `permission_code`
  UNION ALL SELECT 'sys:notification:read'
  UNION ALL SELECT 'sys:notification:ack'
) permission
WHERE role.`role_code` IN (
  'SUPER_ADMIN','MES_MANAGER','PLAN_SCHEDULER','PRODUCTION_SUPERVISOR',
  'PRODUCTION_OPERATOR','QUALITY_ENGINEER','WAREHOUSE_MANAGER','EQUIPMENT_ENGINEER'
) AND role.`deleted_flag` = 0;

INSERT INTO `tmp_notification_role_permission` VALUES ('SUPER_ADMIN','sys:notification:audit')
ON DUPLICATE KEY UPDATE `permission_code` = VALUES(`permission_code`);

INSERT INTO `sys_role_menu`
  (`role_id`, `menu_id`, `created_by`, `created_time`, `deleted_flag`)
SELECT role.`role_id`, menu.`menu_id`, @notification_admin_user_id, @notification_time, 0
FROM `tmp_notification_role_permission` relation
JOIN `sys_role` role
  ON role.`role_code` = relation.`role_code`
 AND role.`status` = 'ENABLE' AND role.`deleted_flag` = 0
JOIN `sys_menu` menu
  ON menu.`permission_code` = relation.`permission_code`
 AND menu.`deleted_flag` = 0
ON DUPLICATE KEY UPDATE `deleted_flag` = 0;

DROP TEMPORARY TABLE IF EXISTS `tmp_notification_role_permission`;
DROP TEMPORARY TABLE IF EXISTS `tmp_notification_permission`;

COMMIT;

SELECT `table_name`
FROM `information_schema`.`tables`
WHERE `table_schema` = DATABASE()
  AND `table_name` IN ('sys_notification','sys_notification_recipient','sys_event_outbox')
ORDER BY `table_name`;

SELECT permission.`permission_code`, COUNT(DISTINCT relation.`role_id`) AS `granted_role_count`
FROM `sys_menu` permission
LEFT JOIN `sys_role_menu` relation
  ON relation.`menu_id` = permission.`menu_id` AND relation.`deleted_flag` = 0
WHERE permission.`permission_code` IN (
  'sys:notification:view','sys:notification:read','sys:notification:ack','sys:notification:audit'
) AND permission.`deleted_flag` = 0
GROUP BY permission.`permission_code`
ORDER BY permission.`permission_code`;
