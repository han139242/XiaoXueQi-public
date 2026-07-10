package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.WmsStockMovement;
import com.neuedu.ems.mapper.WmsStockMovementMapper;
import com.neuedu.ems.service.WmsStockMovementService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 库存流水管理 Service 实现类。
 */
@Service
public class WmsStockMovementServiceImpl extends ServiceImpl<WmsStockMovementMapper, WmsStockMovement> implements WmsStockMovementService {

    /**
     * 分页查询库存流水列表，支持流水号、变动类型、来源单据、物料、批次和方向筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字（匹配流水号）
     * @param movementType 变动类型
     * @param sourceOrderType 来源单据类型
     * @param sourceOrderId 来源单据ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @param direction 方向
     * @return 分页结果
     */
    @Override
    public Page<WmsStockMovement> pageStockMovements(Integer current, Integer size, String keyword, String movementType,
                                                     String sourceOrderType, Long sourceOrderId, Long materialId, Long batchId, String direction) {
        Page<WmsStockMovement> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildStockMovementQuery(keyword, movementType, sourceOrderType, sourceOrderId, materialId, batchId, direction));
    }

    /**
     * 查询库存流水列表，支持流水号、变动类型、来源单据、物料、批次和方向筛选。
     * @param keyword 关键字（匹配流水号）
     * @param movementType 变动类型
     * @param sourceOrderType 来源单据类型
     * @param sourceOrderId 来源单据ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @param direction 方向
     * @return 库存流水列表
     */
    @Override
    public List<WmsStockMovement> listStockMovements(String keyword, String movementType, String sourceOrderType,
                                                     Long sourceOrderId, Long materialId, Long batchId, String direction) {
        return baseMapper.selectList(buildStockMovementQuery(keyword, movementType, sourceOrderType, sourceOrderId, materialId, batchId, direction));
    }

    /**
     * 根据库存流水ID查询未删除的库存流水详情。
     * @param movementId 库存流水ID
     * @return 库存流水实体
     */
    @Override
    public WmsStockMovement getStockMovementById(Long movementId) {
        LambdaQueryWrapper<WmsStockMovement> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsStockMovement::getMovementId, movementId)
                .eq(WmsStockMovement::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 构建库存流水查询条件，默认过滤已删除记录。
     * @param keyword 关键字（匹配流水号）
     * @param movementType 变动类型
     * @param sourceOrderType 来源单据类型
     * @param sourceOrderId 来源单据ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @param direction 方向
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<WmsStockMovement> buildStockMovementQuery(String keyword, String movementType,
                                                                         String sourceOrderType, Long sourceOrderId,
                                                                         Long materialId, Long batchId, String direction) {
        LambdaQueryWrapper<WmsStockMovement> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsStockMovement::getDeletedFlag, 0)
                .like(StringUtils.hasText(keyword), WmsStockMovement::getMovementNo, keyword)
                .eq(StringUtils.hasText(movementType), WmsStockMovement::getMovementType, movementType)
                .eq(StringUtils.hasText(sourceOrderType), WmsStockMovement::getSourceOrderType, sourceOrderType)
                .eq(sourceOrderId != null, WmsStockMovement::getSourceOrderId, sourceOrderId)
                .eq(materialId != null, WmsStockMovement::getMaterialId, materialId)
                .eq(batchId != null, WmsStockMovement::getBatchId, batchId)
                .eq(StringUtils.hasText(direction), WmsStockMovement::getDirection, direction)
                .orderByDesc(WmsStockMovement::getOperationTime)
                .orderByDesc(WmsStockMovement::getCreatedTime);
        return queryWrapper;
    }
}
