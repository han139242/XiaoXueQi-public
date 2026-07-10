package com.neuedu.ems.service.impl.wms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.wms.WmsReturnDetail;
import com.neuedu.ems.entity.wms.WmsReturnOrder;
import com.neuedu.ems.mapper.wms.WmsReturnDetailMapper;
import com.neuedu.ems.mapper.wms.WmsReturnOrderMapper;
import com.neuedu.ems.service.wms.WmsReturnDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 退料明细管理 Service 实现类。
 */
@Service
public class WmsReturnDetailServiceImpl extends ServiceImpl<WmsReturnDetailMapper, WmsReturnDetail> implements WmsReturnDetailService {

    private static final String APPROVED_STATUS = "APPROVED";

    @Autowired
    private WmsReturnOrderMapper wmsReturnOrderMapper;

    /**
     * 分页查询退料明细，支持退料单、物料和批次筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param returnId 退料单ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @return 退料明细分页结果
     */
    @Override
    public Page<WmsReturnDetail> pageReturnDetails(Integer current, Integer size, Long returnId, Long materialId, Long batchId) {
        Page<WmsReturnDetail> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildReturnDetailQuery(returnId, materialId, batchId));
    }

    /**
     * 查询退料明细列表，支持退料单、物料和批次筛选。
     * @param returnId 退料单ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @return 退料明细列表
     */
    @Override
    public List<WmsReturnDetail> listReturnDetails(Long returnId, Long materialId, Long batchId) {
        return baseMapper.selectList(buildReturnDetailQuery(returnId, materialId, batchId));
    }

    /**
     * 根据退料单ID查询未删除的退料明细。
     * @param returnId 退料单ID
     * @return 退料明细列表
     */
    @Override
    public List<WmsReturnDetail> listReturnDetailsByReturnId(Long returnId) {
        return baseMapper.selectList(buildReturnDetailQuery(returnId, null, null));
    }

    /**
     * 根据退料明细ID查询详情。
     * @param detailId 退料明细ID
     * @return 退料明细实体
     */
    @Override
    public WmsReturnDetail getReturnDetailById(Long detailId) {
        LambdaQueryWrapper<WmsReturnDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsReturnDetail::getDetailId, detailId)
                .eq(WmsReturnDetail::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 新增退料明细，自动设置删除标记为0。
     * @param returnDetail 退料明细实体
     * @return 是否成功
     */
    @Override
    public boolean saveReturnDetail(WmsReturnDetail returnDetail) {
        if (!canChangeReturnOrder(returnDetail.getReturnId())) {
            return false;
        }
        if (returnDetail.getDeletedFlag() == null) {
            returnDetail.setDeletedFlag(0);
        }
        return baseMapper.insert(returnDetail) > 0;
    }

    /**
     * 更新退料明细，不允许通过普通更新修改删除标记。
     * @param returnDetail 退料明细实体
     * @return 是否成功
     */
    @Override
    public boolean updateReturnDetail(WmsReturnDetail returnDetail) {
        if (returnDetail.getDetailId() == null) {
            return false;
        }
        WmsReturnDetail existing = getReturnDetailById(returnDetail.getDetailId());
        if (existing == null || !canChangeReturnOrder(existing.getReturnId())) {
            return false;
        }
        if (returnDetail.getReturnId() != null && !canChangeReturnOrder(returnDetail.getReturnId())) {
            return false;
        }
        returnDetail.setDeletedFlag(null);
        return baseMapper.updateById(returnDetail) > 0;
    }

    /**
     * 根据ID软删除退料明细。
     * @param detailId 退料明细ID
     * @return 是否成功
     */
    @Override
    public boolean removeReturnDetailById(Long detailId) {
        WmsReturnDetail existing = getReturnDetailById(detailId);
        if (existing == null || !canChangeReturnOrder(existing.getReturnId())) {
            return false;
        }
        WmsReturnDetail returnDetail = new WmsReturnDetail();
        returnDetail.setDetailId(detailId);
        returnDetail.setDeletedFlag(1);
        return baseMapper.updateById(returnDetail) > 0;
    }

    /**
     * 构建退料明细查询条件，默认过滤已删除记录。
     * @param returnId 退料单ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<WmsReturnDetail> buildReturnDetailQuery(Long returnId, Long materialId, Long batchId) {
        LambdaQueryWrapper<WmsReturnDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsReturnDetail::getDeletedFlag, 0)
                .eq(returnId != null, WmsReturnDetail::getReturnId, returnId)
                .eq(materialId != null, WmsReturnDetail::getMaterialId, materialId)
                .eq(batchId != null, WmsReturnDetail::getBatchId, batchId)
                .orderByDesc(WmsReturnDetail::getCreatedTime);
        return queryWrapper;
    }

    /**
     * 校验退料明细所属退料单是否允许修改；已确认退料单不允许再改明细。
     * @param returnId 退料单ID，可为空
     * @return 是否允许修改明细
     */
    private boolean canChangeReturnOrder(Long returnId) {
        if (returnId == null) {
            return true;
        }
        LambdaQueryWrapper<WmsReturnOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsReturnOrder::getReturnId, returnId)
                .eq(WmsReturnOrder::getDeletedFlag, 0);
        WmsReturnOrder returnOrder = wmsReturnOrderMapper.selectOne(queryWrapper);
        return returnOrder != null && !APPROVED_STATUS.equals(returnOrder.getAuditStatus());
    }
}
