package com.neuedu.ems.service.impl.wms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.wms.WmsStocktakeDetail;
import com.neuedu.ems.entity.wms.WmsStocktakeOrder;
import com.neuedu.ems.mapper.wms.WmsStocktakeDetailMapper;
import com.neuedu.ems.mapper.wms.WmsStocktakeOrderMapper;
import com.neuedu.ems.service.wms.WmsStocktakeDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 盘点明细管理 Service 实现类。
 */
@Service
public class WmsStocktakeDetailServiceImpl extends ServiceImpl<WmsStocktakeDetailMapper, WmsStocktakeDetail> implements WmsStocktakeDetailService {

    private static final String FINISHED_STATUS = "FINISHED";

    @Autowired
    private WmsStocktakeOrderMapper wmsStocktakeOrderMapper;

    /**
     * 分页查询盘点明细，支持盘点单、物料、批次和库位筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param stocktakeId 盘点单ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @param locationId 库位ID
     * @return 盘点明细分页结果
     */
    @Override
    public Page<WmsStocktakeDetail> pageStocktakeDetails(Integer current, Integer size, Long stocktakeId, Long materialId, Long batchId, Long locationId) {
        Page<WmsStocktakeDetail> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildStocktakeDetailQuery(stocktakeId, materialId, batchId, locationId));
    }

    /**
     * 查询盘点明细列表，支持盘点单、物料、批次和库位筛选。
     * @param stocktakeId 盘点单ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @param locationId 库位ID
     * @return 盘点明细列表
     */
    @Override
    public List<WmsStocktakeDetail> listStocktakeDetails(Long stocktakeId, Long materialId, Long batchId, Long locationId) {
        return baseMapper.selectList(buildStocktakeDetailQuery(stocktakeId, materialId, batchId, locationId));
    }

    /**
     * 根据盘点单ID查询未删除的盘点明细。
     * @param stocktakeId 盘点单ID
     * @return 盘点明细列表
     */
    @Override
    public List<WmsStocktakeDetail> listStocktakeDetailsByStocktakeId(Long stocktakeId) {
        return baseMapper.selectList(buildStocktakeDetailQuery(stocktakeId, null, null, null));
    }

    /**
     * 根据盘点明细ID查询详情。
     * @param detailId 盘点明细ID
     * @return 盘点明细实体
     */
    @Override
    public WmsStocktakeDetail getStocktakeDetailById(Long detailId) {
        LambdaQueryWrapper<WmsStocktakeDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsStocktakeDetail::getDetailId, detailId)
                .eq(WmsStocktakeDetail::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 新增盘点明细，自动设置删除标记为0。
     * @param stocktakeDetail 盘点明细实体
     * @return 是否成功
     */
    @Override
    public boolean saveStocktakeDetail(WmsStocktakeDetail stocktakeDetail) {
        if (!canChangeStocktakeOrder(stocktakeDetail.getStocktakeId())) {
            return false;
        }
        setDefaultStocktakeQuantities(stocktakeDetail);
        if (stocktakeDetail.getDeletedFlag() == null) {
            stocktakeDetail.setDeletedFlag(0);
        }
        return baseMapper.insert(stocktakeDetail) > 0;
    }

    /**
     * 更新盘点明细，不允许通过普通更新修改删除标记。
     * @param stocktakeDetail 盘点明细实体
     * @return 是否成功
     */
    @Override
    public boolean updateStocktakeDetail(WmsStocktakeDetail stocktakeDetail) {
        if (stocktakeDetail.getDetailId() == null) {
            return false;
        }
        WmsStocktakeDetail existing = getStocktakeDetailById(stocktakeDetail.getDetailId());
        if (existing == null || !canChangeStocktakeOrder(existing.getStocktakeId())) {
            return false;
        }
        if (stocktakeDetail.getStocktakeId() != null && !canChangeStocktakeOrder(stocktakeDetail.getStocktakeId())) {
            return false;
        }
        fillMissingQuantities(stocktakeDetail, existing);
        setDefaultStocktakeQuantities(stocktakeDetail);
        stocktakeDetail.setDeletedFlag(null);
        return baseMapper.updateById(stocktakeDetail) > 0;
    }

    /**
     * 根据ID软删除盘点明细。
     * @param detailId 盘点明细ID
     * @return 是否成功
     */
    @Override
    public boolean removeStocktakeDetailById(Long detailId) {
        WmsStocktakeDetail existing = getStocktakeDetailById(detailId);
        if (existing == null || !canChangeStocktakeOrder(existing.getStocktakeId())) {
            return false;
        }
        WmsStocktakeDetail stocktakeDetail = new WmsStocktakeDetail();
        stocktakeDetail.setDetailId(detailId);
        stocktakeDetail.setDeletedFlag(1);
        return baseMapper.updateById(stocktakeDetail) > 0;
    }

    /**
     * 构建盘点明细查询条件，默认过滤已删除记录。
     * @param stocktakeId 盘点单ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @param locationId 库位ID
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<WmsStocktakeDetail> buildStocktakeDetailQuery(Long stocktakeId, Long materialId, Long batchId, Long locationId) {
        LambdaQueryWrapper<WmsStocktakeDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsStocktakeDetail::getDeletedFlag, 0)
                .eq(stocktakeId != null, WmsStocktakeDetail::getStocktakeId, stocktakeId)
                .eq(materialId != null, WmsStocktakeDetail::getMaterialId, materialId)
                .eq(batchId != null, WmsStocktakeDetail::getBatchId, batchId)
                .eq(locationId != null, WmsStocktakeDetail::getLocationId, locationId)
                .orderByDesc(WmsStocktakeDetail::getCreatedTime);
        return queryWrapper;
    }

    /**
     * 校验盘点明细所属盘点单是否允许修改；已完成盘点单不允许再改明细。
     * @param stocktakeId 盘点单ID，可为空
     * @return 是否允许修改明细
     */
    private boolean canChangeStocktakeOrder(Long stocktakeId) {
        if (stocktakeId == null) {
            return true;
        }
        LambdaQueryWrapper<WmsStocktakeOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsStocktakeOrder::getStocktakeId, stocktakeId)
                .eq(WmsStocktakeOrder::getDeletedFlag, 0);
        WmsStocktakeOrder stocktakeOrder = wmsStocktakeOrderMapper.selectOne(queryWrapper);
        return stocktakeOrder != null && !FINISHED_STATUS.equals(stocktakeOrder.getStocktakeStatus());
    }

    /**
     * 设置盘点明细数量默认值并计算差异数量，差异=实盘数量-账面数量。
     * @param detail 盘点明细实体
     */
    private void setDefaultStocktakeQuantities(WmsStocktakeDetail detail) {
        BigDecimal bookQuantity = detail.getBookQuantity() == null ? BigDecimal.ZERO : detail.getBookQuantity();
        BigDecimal actualQuantity = detail.getActualQuantity() == null ? BigDecimal.ZERO : detail.getActualQuantity();
        detail.setBookQuantity(bookQuantity);
        detail.setActualQuantity(actualQuantity);
        detail.setDifferenceQuantity(actualQuantity.subtract(bookQuantity));
    }

    /**
     * 部分更新盘点明细时，用已有账面数量和实盘数量回填未传字段，避免差异被错误重算。
     * @param detail 请求中的盘点明细实体
     * @param existing 数据库中已有的盘点明细实体
     */
    private void fillMissingQuantities(WmsStocktakeDetail detail, WmsStocktakeDetail existing) {
        if (detail.getBookQuantity() == null) {
            detail.setBookQuantity(existing.getBookQuantity());
        }
        if (detail.getActualQuantity() == null) {
            detail.setActualQuantity(existing.getActualQuantity());
        }
    }
}
