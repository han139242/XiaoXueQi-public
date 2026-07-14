-- 智能排产数据基础迁移：新增产线产品能力、产线周历、排程方案和排程明细。
-- 使用前请先备份数据库并选择目标库；脚本不修改生产交易数据，可重复执行。

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
SET @scheduling_marker = 'SMART_SCHEDULING_FOUNDATION';
SET @scheduling_time = CURRENT_TIMESTAMP();
SET @scheduling_operator_id = (
  SELECT `user_id`
  FROM `sys_user`
  WHERE `username` = 'admin' AND `deleted_flag` = 0
  ORDER BY `user_id`
  LIMIT 1
);

CREATE TABLE IF NOT EXISTS `mfg_line_product_capability` (
  `capability_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '产线产品能力主键。',
  `line_id` BIGINT UNSIGNED NOT NULL COMMENT '产线 ID。',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '可生产产品 ID。',
  `route_id` BIGINT UNSIGNED NOT NULL COMMENT '该产线采用的已发布工艺路线 ID。',
  `priority_level` INT NOT NULL DEFAULT 100 COMMENT '适配优先级，值越小越优先。',
  `capacity_factor` DECIMAL(5,4) NOT NULL DEFAULT 1.0000 COMMENT '产品在该产线的产能系数，大于 0 且不超过 2。',
  `status` VARCHAR(32) NOT NULL COMMENT 'ENABLE:启用，DISABLE:停用。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`capability_id`),
  UNIQUE KEY `uk_mfg_line_product_capability` (`line_id`, `product_id`, `route_id`),
  KEY `idx_mfg_line_product_capability_product_id` (`product_id`),
  KEY `idx_mfg_line_product_capability_route_id` (`route_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='产线产品能力表';

CREATE TABLE IF NOT EXISTS `mfg_line_calendar_rule` (
  `calendar_rule_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '产线日历规则主键。',
  `line_id` BIGINT UNSIGNED NOT NULL COMMENT '产线 ID。',
  `day_of_week` TINYINT NOT NULL COMMENT '星期，1 周一至 7 周日。',
  `shift_id` BIGINT UNSIGNED NOT NULL COMMENT '班次 ID。',
  `effective_from` DATE NOT NULL COMMENT '生效日期。',
  `effective_to` DATE NULL COMMENT '失效日期，为空表示长期有效。',
  `capacity_factor` DECIMAL(5,4) NOT NULL DEFAULT 1.0000 COMMENT '有效产能系数，大于 0 且不超过 2。',
  `status` VARCHAR(32) NOT NULL COMMENT 'ENABLE:启用，DISABLE:停用。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`calendar_rule_id`),
  UNIQUE KEY `uk_mfg_line_calendar_rule` (`line_id`, `day_of_week`, `shift_id`, `effective_from`),
  KEY `idx_mfg_line_calendar_rule_line_id` (`line_id`),
  KEY `idx_mfg_line_calendar_rule_shift_id` (`shift_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='产线周历规则表';

CREATE TABLE IF NOT EXISTS `mfg_schedule_scenario` (
  `scenario_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '排程方案主键。',
  `scenario_no` VARCHAR(64) NOT NULL COMMENT '排程方案编号。',
  `scenario_name` VARCHAR(128) NOT NULL COMMENT '排程方案名称。',
  `horizon_start` DATETIME NOT NULL COMMENT '排程窗口开始时间。',
  `horizon_end` DATETIME NOT NULL COMMENT '排程窗口结束时间。',
  `algorithm_code` VARCHAR(32) NOT NULL COMMENT '排程算法编码。',
  `status` VARCHAR(32) NOT NULL COMMENT 'DRAFT:草稿，CALCULATED:已计算，CONFIRMED:已确认，CANCELLED:已取消。',
  `order_count` INT NOT NULL DEFAULT 0 COMMENT '参与订单数量。',
  `scheduled_order_count` INT NOT NULL DEFAULT 0 COMMENT '成功排程订单数量。',
  `conflict_order_count` INT NOT NULL DEFAULT 0 COMMENT '冲突订单数量。',
  `late_order_count` INT NOT NULL DEFAULT 0 COMMENT '预计延期订单数量。',
  `total_load_minutes` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '总有效负荷分钟数。',
  `ai_advice` TEXT NULL COMMENT 'AI 排程风险建议，仅供人工决策。',
  `ai_model` VARCHAR(64) NULL COMMENT '生成建议的模型。',
  `ai_generated_time` DATETIME NULL COMMENT 'AI 建议生成时间。',
  `confirmed_by` BIGINT UNSIGNED NULL COMMENT '确认系统用户 ID。',
  `confirmed_time` DATETIME NULL COMMENT '确认时间。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`scenario_id`),
  UNIQUE KEY `uk_mfg_schedule_scenario_scenario_no` (`scenario_no`),
  KEY `idx_mfg_schedule_scenario_status` (`status`),
  KEY `idx_mfg_schedule_scenario_horizon` (`horizon_start`, `horizon_end`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='智能排程方案表';

CREATE TABLE IF NOT EXISTS `mfg_schedule_item` (
  `schedule_item_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '排程明细主键。',
  `scenario_id` BIGINT UNSIGNED NOT NULL COMMENT '排程方案 ID。',
  `production_order_id` BIGINT UNSIGNED NOT NULL COMMENT '生产订单 ID。',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '产品 ID。',
  `route_id` BIGINT UNSIGNED NULL COMMENT '已发布工艺路线 ID。',
  `line_id` BIGINT UNSIGNED NULL COMMENT '分配产线 ID，冲突时为空。',
  `plan_id` BIGINT UNSIGNED NULL COMMENT '确认后生成的生产计划 ID。',
  `sequence_no` INT NOT NULL COMMENT '方案内执行顺序。',
  `planned_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '计划数量。',
  `planned_start_time` DATETIME NULL COMMENT '计划开始时间。',
  `planned_end_time` DATETIME NULL COMMENT '计划结束时间。',
  `estimated_minutes` DECIMAL(18,2) NULL COMMENT '预计有效加工分钟数。',
  `priority_score` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '规则引擎优先级分值。',
  `tardiness_minutes` BIGINT NOT NULL DEFAULT 0 COMMENT '预计延期分钟数。',
  `decision_source` VARCHAR(32) NOT NULL COMMENT 'RULE_ENGINE:规则引擎，MANUAL:人工调整。',
  `status` VARCHAR(32) NOT NULL COMMENT 'SCHEDULED:已排程，CONFLICT:存在冲突，CONFIRMED:已确认。',
  `conflict_reason` VARCHAR(500) NULL COMMENT '冲突原因。',
  `explanation` VARCHAR(500) NULL COMMENT '排程决策说明。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`schedule_item_id`),
  UNIQUE KEY `uk_mfg_schedule_item_order` (`scenario_id`, `production_order_id`),
  KEY `idx_mfg_schedule_item_production_order_id` (`production_order_id`),
  KEY `idx_mfg_schedule_item_product_id` (`product_id`),
  KEY `idx_mfg_schedule_item_route_id` (`route_id`),
  KEY `idx_mfg_schedule_item_line_id` (`line_id`),
  KEY `idx_mfg_schedule_item_plan_id` (`plan_id`),
  KEY `idx_mfg_schedule_item_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='智能排程明细表';

-- 兼容已经执行早期草稿迁移的数据库，按索引名幂等补齐排程明细逻辑外键索引。
SET @index_ddl = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
   AND TABLE_NAME='mfg_schedule_item' AND INDEX_NAME='idx_mfg_schedule_item_production_order_id')=0,
  'ALTER TABLE `mfg_schedule_item` ADD KEY `idx_mfg_schedule_item_production_order_id` (`production_order_id`)',
  'DO 0'
);
PREPARE scheduling_index_stmt FROM @index_ddl;
EXECUTE scheduling_index_stmt;
DEALLOCATE PREPARE scheduling_index_stmt;

SET @index_ddl = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
   AND TABLE_NAME='mfg_schedule_item' AND INDEX_NAME='idx_mfg_schedule_item_product_id')=0,
  'ALTER TABLE `mfg_schedule_item` ADD KEY `idx_mfg_schedule_item_product_id` (`product_id`)',
  'DO 0'
);
PREPARE scheduling_index_stmt FROM @index_ddl;
EXECUTE scheduling_index_stmt;
DEALLOCATE PREPARE scheduling_index_stmt;

SET @index_ddl = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
   AND TABLE_NAME='mfg_schedule_item' AND INDEX_NAME='idx_mfg_schedule_item_route_id')=0,
  'ALTER TABLE `mfg_schedule_item` ADD KEY `idx_mfg_schedule_item_route_id` (`route_id`)',
  'DO 0'
);
PREPARE scheduling_index_stmt FROM @index_ddl;
EXECUTE scheduling_index_stmt;
DEALLOCATE PREPARE scheduling_index_stmt;

START TRANSACTION;

INSERT INTO `mfg_line_product_capability`
  (`line_id`, `product_id`, `route_id`, `priority_level`, `capacity_factor`, `status`,
   `remark`, `created_by`, `created_time`)
SELECT line.`line_id`, product.`product_id`, route.`route_id`, 10, 1.0000, 'ENABLE',
       CONCAT(@scheduling_marker, ': 专用产线产品路线映射'), @scheduling_operator_id, @scheduling_time
FROM (
  SELECT 'LINE-TRAIN-01' AS `line_code`, 'PROD-LNX-T300-76' AS `product_code`,
         'ROUTE-LNX-T300-76-V1' AS `route_code`
  UNION ALL SELECT 'LINE-TRAIN-01', 'PROD-LNX-T300-77', 'ROUTE-LNX-T300-77-V1'
  UNION ALL SELECT 'LINE-TRAIN-01', 'PROD-OEM-XY-C500-77', 'ROUTE-OEM-XY-C500-77-V1'
  UNION ALL SELECT 'LINE-MATCH-01', 'PROD-LNX-M700-77', 'ROUTE-LNX-M700-77-V1'
  UNION ALL SELECT 'LINE-MATCH-01', 'PROD-LNX-M700-78', 'ROUTE-LNX-M700-78-V1'
) seed
JOIN `factory_line` line ON line.`line_code` = seed.`line_code` AND line.`deleted_flag` = 0
JOIN `mdm_product` product ON product.`product_code` = seed.`product_code` AND product.`deleted_flag` = 0
JOIN `process_route` route ON route.`route_code` = seed.`route_code`
  AND route.`product_id` = product.`product_id` AND route.`status` = 'RELEASED' AND route.`deleted_flag` = 0
WHERE NOT EXISTS (
  SELECT 1 FROM `mfg_line_product_capability` existing
  WHERE existing.`line_id` = line.`line_id`
    AND existing.`product_id` = product.`product_id`
    AND existing.`route_id` = route.`route_id`
);

INSERT INTO `mfg_line_calendar_rule`
  (`line_id`, `day_of_week`, `shift_id`, `effective_from`, `effective_to`, `capacity_factor`, `status`,
   `remark`, `created_by`, `created_time`)
SELECT line.`line_id`, weekdays.`day_of_week`, shift.`shift_id`, '2026-01-01', NULL, 1.0000, 'ENABLE',
       CONCAT(@scheduling_marker, ': 周一至周六白班'), @scheduling_operator_id, @scheduling_time
FROM (
  SELECT 1 AS `day_of_week` UNION ALL SELECT 2 UNION ALL SELECT 3
  UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6
) weekdays
JOIN `factory_line` line ON line.`line_code` IN ('LINE-TRAIN-01', 'LINE-MATCH-01') AND line.`deleted_flag` = 0
JOIN `org_shift` shift ON shift.`shift_code` = 'SHIFT-DAY' AND shift.`deleted_flag` = 0
WHERE NOT EXISTS (
  SELECT 1 FROM `mfg_line_calendar_rule` existing
  WHERE existing.`line_id` = line.`line_id`
    AND existing.`day_of_week` = weekdays.`day_of_week`
    AND existing.`shift_id` = shift.`shift_id`
    AND existing.`effective_from` = '2026-01-01'
);

-- 兼容本模块早期草稿已经创建的同一组周历，仅规范化基础标识和缺失审计字段。
UPDATE `mfg_line_calendar_rule` calendar_rule
JOIN `factory_line` line ON line.`line_id` = calendar_rule.`line_id`
  AND line.`line_code` IN ('LINE-TRAIN-01', 'LINE-MATCH-01') AND line.`deleted_flag` = 0
JOIN `org_shift` shift_record ON shift_record.`shift_id` = calendar_rule.`shift_id`
  AND shift_record.`shift_code` = 'SHIFT-DAY' AND shift_record.`deleted_flag` = 0
SET calendar_rule.`remark` = CONCAT(@scheduling_marker, ': 周一至周六白班'),
    calendar_rule.`created_by` = COALESCE(calendar_rule.`created_by`, @scheduling_operator_id),
    calendar_rule.`created_time` = COALESCE(calendar_rule.`created_time`, @scheduling_time)
WHERE calendar_rule.`deleted_flag` = 0
  AND calendar_rule.`day_of_week` BETWEEN 1 AND 6
  AND calendar_rule.`effective_from` = '2026-01-01'
  AND calendar_rule.`effective_to` IS NULL
  AND (NOT (calendar_rule.`remark` <=> CONCAT(@scheduling_marker, ': 周一至周六白班'))
    OR calendar_rule.`created_by` IS NULL OR calendar_rule.`created_time` IS NULL);

COMMIT;

SELECT TABLE_NAME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('mfg_line_product_capability', 'mfg_line_calendar_rule',
                     'mfg_schedule_scenario', 'mfg_schedule_item')
ORDER BY TABLE_NAME;

SELECT COUNT(*) AS `foundation_line_capabilities`
FROM `mfg_line_product_capability`
WHERE `deleted_flag` = 0 AND `remark` LIKE CONCAT(@scheduling_marker, ':%');

SELECT COUNT(*) AS `foundation_calendar_rules`
FROM `mfg_line_calendar_rule`
WHERE `deleted_flag` = 0 AND `remark` LIKE CONCAT(@scheduling_marker, ':%');
