package com.neuedu.ems.service.impl.wms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.wms.WmsOutboundDetail;
import com.neuedu.ems.entity.wms.WmsOutboundOrder;
import com.neuedu.ems.mapper.wms.WmsOutboundDetailMapper;
import com.neuedu.ems.mapper.wms.WmsOutboundOrderMapper;
import com.neuedu.ems.service.wms.WmsOutboundDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 出库明细管理 Service 实现类。
 */
@Service
public class WmsOutboundDetailServiceImpl extends ServiceImpl<WmsOutboundDetailMapper, WmsOutboundDetail> implements WmsOutboundDetailService {

    private static final String APPROVED_STATUS = "APPROVED";

    @Autowired
    private WmsOutboundOrderMapper wmsOutboundOrderMapper;

    /**
     * 分页查询出库明细，支持出库单、物料、批次和库位筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param outboundId 出库单ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @param locationId 库位ID
     * @return 出库明细分页结果
     */
    @Override
    public Page<WmsOutboundDetail> pageOutboundDetails(Integer current, Integer size, Long outboundId, Long materialId, Long batchId, Long locationId) {
        Page<WmsOutboundDetail> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildOutboundDetailQuery(outboundId, materialId, batchId, locationId));
    }

    /**
     * 查询出库明细列表，支持出库单、物料、批次和库位筛选。
     * @param outboundId 出库单ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @param locationId 库位ID
     * @return 出库明细列表
     */
    @Override
    public List<WmsOutboundDetail> listOutboundDetails(Long outboundId, Long materialId, Long batchId, Long locationId) {
        return baseMapper.selectList(buildOutboundDetailQuery(outboundId, materialId, batchId, locationId));
    }

    /**
     * 根据出库单ID查询未删除的出库明细。
     * @param outboundId 出库单ID
     * @return 出库明细列表
     */
    @Override
    public List<WmsOutboundDetail> listOutboundDetailsByOutboundId(Long outboundId) {
        return baseMapper.selectList(buildOutboundDetailQuery(outboundId, null, null, null));
    }

    /**
     * 根据出库明细ID查询详情。
     * @param detailId 出库明细ID
     * @return 出库明细实体
     */
    @Override
    public WmsOutboundDetail getOutboundDetailById(Long detailId) {
        LambdaQueryWrapper<WmsOutboundDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsOutboundDetail::getDetailId, detailId)
                .eq(WmsOutboundDetail::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 新增出库明细，自动设置删除标记为0。
     * @param outboundDetail 出库明细实体
     * @return 是否成功
     */
    @Override
    public boolean saveOutboundDetail(WmsOutboundDetail outboundDetail) {
        if (!canChangeOutboundOrder(outboundDetail.getOutboundId())) {
            return false;
        }
        if (outboundDetail.getDeletedFlag() == null) {
            outboundDetail.setDeletedFlag(0);
        }
        return baseMapper.insert(outboundDetail) > 0;
    }

    /**
     * 更新出库明细，不允许通过普通更新修改删除标记。
     * @param outboundDetail 出库明细实体
     * @return 是否成功
     */
    @Override
    public boolean updateOutboundDetail(WmsOutboundDetail outboundDetail) {
        if (outboundDetail.getDetailId() == null) {
            return false;
        }
        WmsOutboundDetail existing = getOutboundDetailById(outboundDetail.getDetailId());
        if (existing == null || !canChangeOutboundOrder(existing.getOutboundId())) {
            return false;
        }
        if (outboundDetail.getOutboundId() != null && !canChangeOutboundOrder(outboundDetail.getOutboundId())) {
            return false;
        }
        outboundDetail.setDeletedFlag(null);
        return baseMapper.updateById(outboundDetail) > 0;
    }

    /**
     * 根据ID软删除出库明细。
     * @param detailId 出库明细ID
     * @return 是否成功
     */
    @Override
    public boolean removeOutboundDetailById(Long detailId) {
        WmsOutboundDetail existing = getOutboundDetailById(detailId);
        if (existing == null || !canChangeOutboundOrder(existing.getOutboundId())) {
            return false;
        }
        WmsOutboundDetail outboundDetail = new WmsOutboundDetail();
        outboundDetail.setDetailId(detailId);
        outboundDetail.setDeletedFlag(1);
        return baseMapper.updateById(outboundDetail) > 0;
    }

    /**
     * 构建出库明细查询条件，默认过滤已删除记录。
     * @param outboundId 出库单ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @param locationId 库位ID
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<WmsOutboundDetail> buildOutboundDetailQuery(Long outboundId, Long materialId, Long batchId, Long locationId) {
        LambdaQueryWrapper<WmsOutboundDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsOutboundDetail::getDeletedFlag, 0)
                .eq(outboundId != null, WmsOutboundDetail::getOutboundId, outboundId)
                .eq(materialId != null, WmsOutboundDetail::getMaterialId, materialId)
                .eq(batchId != null, WmsOutboundDetail::getBatchId, batchId)
                .eq(locationId != null, WmsOutboundDetail::getLocationId, locationId)
                .orderByDesc(WmsOutboundDetail::getCreatedTime);
        return queryWrapper;
    }

    /**
     * 校验出库明细所属出库单是否允许修改；已确认出库单不允许再改明细。
     * @param outboundId 出库单ID，可为空
     * @return 是否允许修改明细
     */
    private boolean canChangeOutboundOrder(Long outboundId) {
        if (outboundId == null) {
            return true;
        }
        LambdaQueryWrapper<WmsOutboundOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsOutboundOrder::getOutboundId, outboundId)
                .eq(WmsOutboundOrder::getDeletedFlag, 0);
        WmsOutboundOrder outboundOrder = wmsOutboundOrderMapper.selectOne(queryWrapper);
        return outboundOrder != null && !APPROVED_STATUS.equals(outboundOrder.getAuditStatus());
    }
}
