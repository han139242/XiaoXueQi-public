package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.WmsInventory;
import com.neuedu.ems.entity.WmsReplenishmentSuggestion;
import com.neuedu.ems.mapper.WmsInventoryMapper;
import com.neuedu.ems.mapper.WmsReplenishmentSuggestionMapper;
import com.neuedu.ems.service.WmsReplenishmentSuggestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 补货建议 Service 实现类。
 */
@Service
public class WmsReplenishmentSuggestionServiceImpl extends ServiceImpl<WmsReplenishmentSuggestionMapper, WmsReplenishmentSuggestion> implements WmsReplenishmentSuggestionService {

    private static final String PENDING_STATUS = "SUBMITTED";
    private static final String MANUAL_SOURCE = "MANUAL";
    private static final String INVENTORY_WARNING_SOURCE = "INVENTORY_WARNING";

    @Autowired
    private WmsInventoryMapper wmsInventoryMapper;

    /**
     * 分页查询补货建议，支持建议编号、物料、来源类型和状态筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 建议编号关键字
     * @param materialId 物料ID
     * @param sourceType 来源类型
     * @param status 状态
     * @return 补货建议分页结果
     */
    @Override
    public Page<WmsReplenishmentSuggestion> pageSuggestions(Integer current, Integer size, String keyword, Long materialId, String sourceType, String status) {
        Page<WmsReplenishmentSuggestion> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildSuggestionQuery(keyword, materialId, sourceType, status));
    }

    /**
     * 查询补货建议列表，支持建议编号、物料、来源类型和状态筛选。
     * @param keyword 建议编号关键字
     * @param materialId 物料ID
     * @param sourceType 来源类型
     * @param status 状态
     * @return 补货建议列表
     */
    @Override
    public List<WmsReplenishmentSuggestion> listSuggestions(String keyword, Long materialId, String sourceType, String status) {
        return baseMapper.selectList(buildSuggestionQuery(keyword, materialId, sourceType, status));
    }

    /**
     * 根据ID查询未删除的补货建议。
     * @param suggestionId 补货建议ID
     * @return 补货建议实体
     */
    @Override
    public WmsReplenishmentSuggestion getSuggestionById(Long suggestionId) {
        LambdaQueryWrapper<WmsReplenishmentSuggestion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsReplenishmentSuggestion::getSuggestionId, suggestionId)
                .eq(WmsReplenishmentSuggestion::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 新增补货建议，自动补齐删除标记、建议编号、状态和来源类型。
     * @param suggestion 补货建议实体
     * @return 是否成功
     */
    @Override
    public boolean saveSuggestion(WmsReplenishmentSuggestion suggestion) {
        prepareSuggestion(suggestion);
        return baseMapper.insert(suggestion) > 0;
    }

    /**
     * 修改补货建议，不允许通过普通修改接口修改删除标记。
     * @param suggestion 补货建议实体
     * @return 是否成功
     */
    @Override
    public boolean updateSuggestion(WmsReplenishmentSuggestion suggestion) {
        if (suggestion.getSuggestionId() == null || getSuggestionById(suggestion.getSuggestionId()) == null) {
            return false;
        }
        suggestion.setDeletedFlag(null);
        return baseMapper.updateById(suggestion) > 0;
    }

    /**
     * 根据ID软删除补货建议。
     * @param suggestionId 补货建议ID
     * @return 是否成功
     */
    @Override
    public boolean removeSuggestionById(Long suggestionId) {
        if (getSuggestionById(suggestionId) == null) {
            return false;
        }
        WmsReplenishmentSuggestion suggestion = new WmsReplenishmentSuggestion();
        suggestion.setSuggestionId(suggestionId);
        suggestion.setDeletedFlag(1);
        return baseMapper.updateById(suggestion) > 0;
    }

    /**
     * 根据物料和仓库生成补货建议；schema没有仓库字段，因此仓库只用于计算当前可用库存。
     * @param materialId 物料ID
     * @param warehouseId 仓库ID，可为空
     * @param requiredQuantity 需求数量，可为空
     * @return 补货建议ID
     */
    @Override
    public Long generateSuggestion(Long materialId, Long warehouseId, BigDecimal requiredQuantity) {
        if (materialId == null) {
            return null;
        }
        BigDecimal required = requiredQuantity == null ? BigDecimal.ZERO : requiredQuantity;
        BigDecimal available = sumAvailableQuantity(materialId, warehouseId);
        WmsReplenishmentSuggestion suggestion = new WmsReplenishmentSuggestion();
        suggestion.setMaterialId(materialId);
        suggestion.setRequiredQuantity(required);
        suggestion.setAvailableQuantity(available);
        suggestion.setSuggestedQuantity(required.subtract(available).max(BigDecimal.ZERO));
        suggestion.setSourceType(INVENTORY_WARNING_SOURCE);
        suggestion.setStatus(PENDING_STATUS);
        saveSuggestion(suggestion);
        return suggestion.getSuggestionId();
    }

    /**
     * 构建补货建议查询条件，默认过滤已删除记录。
     * @param keyword 建议编号关键字
     * @param materialId 物料ID
     * @param sourceType 来源类型
     * @param status 状态
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<WmsReplenishmentSuggestion> buildSuggestionQuery(String keyword, Long materialId, String sourceType, String status) {
        LambdaQueryWrapper<WmsReplenishmentSuggestion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsReplenishmentSuggestion::getDeletedFlag, 0)
                .like(StringUtils.hasText(keyword), WmsReplenishmentSuggestion::getSuggestionNo, keyword)
                .eq(materialId != null, WmsReplenishmentSuggestion::getMaterialId, materialId)
                .eq(StringUtils.hasText(sourceType), WmsReplenishmentSuggestion::getSourceType, sourceType)
                .eq(StringUtils.hasText(status), WmsReplenishmentSuggestion::getStatus, status)
                .orderByDesc(WmsReplenishmentSuggestion::getCreatedTime);
        return queryWrapper;
    }

    /**
     * 补齐补货建议的默认字段。
     * @param suggestion 补货建议实体
     */
    private void prepareSuggestion(WmsReplenishmentSuggestion suggestion) {
        if (!StringUtils.hasText(suggestion.getSuggestionNo())) {
            suggestion.setSuggestionNo("RPS" + System.currentTimeMillis());
        }
        if (suggestion.getRequiredQuantity() == null) {
            suggestion.setRequiredQuantity(BigDecimal.ZERO);
        }
        if (suggestion.getAvailableQuantity() == null) {
            suggestion.setAvailableQuantity(BigDecimal.ZERO);
        }
        if (suggestion.getSuggestedQuantity() == null) {
            suggestion.setSuggestedQuantity(suggestion.getRequiredQuantity().subtract(suggestion.getAvailableQuantity()).max(BigDecimal.ZERO));
        }
        if (!StringUtils.hasText(suggestion.getSourceType())) {
            suggestion.setSourceType(MANUAL_SOURCE);
        }
        if (!StringUtils.hasText(suggestion.getStatus())) {
            suggestion.setStatus(PENDING_STATUS);
        }
        if (suggestion.getDeletedFlag() == null) {
            suggestion.setDeletedFlag(0);
        }
    }

    /**
     * 汇总指定物料在指定仓库的未删除可用库存。
     * @param materialId 物料ID
     * @param warehouseId 仓库ID，可为空
     * @return 可用库存合计
     */
    private BigDecimal sumAvailableQuantity(Long materialId, Long warehouseId) {
        LambdaQueryWrapper<WmsInventory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsInventory::getMaterialId, materialId)
                .eq(warehouseId != null, WmsInventory::getWarehouseId, warehouseId)
                .eq(WmsInventory::getDeletedFlag, 0);
        List<WmsInventory> inventories = wmsInventoryMapper.selectList(queryWrapper);
        BigDecimal total = BigDecimal.ZERO;
        for (WmsInventory inventory : inventories) {
            total = total.add(inventory.getAvailableQuantity() == null ? BigDecimal.ZERO : inventory.getAvailableQuantity());
        }
        return total;
    }
}
