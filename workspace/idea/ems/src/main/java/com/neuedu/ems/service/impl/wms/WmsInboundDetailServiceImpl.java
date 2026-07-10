package com.neuedu.ems.service.impl.wms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.wms.WmsInboundDetail;
import com.neuedu.ems.mapper.wms.WmsInboundDetailMapper;
import com.neuedu.ems.service.wms.WmsInboundDetailService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 入库明细管理 Service 实现类。
 */
@Service
public class WmsInboundDetailServiceImpl extends ServiceImpl<WmsInboundDetailMapper, WmsInboundDetail> implements WmsInboundDetailService {

    /**
     * 分页查询入库明细列表，支持入库单、物料、批次和库位筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param inboundId 入库单ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @param locationId 库位ID
     * @return 分页结果
     */
    @Override
    public Page<WmsInboundDetail> pageInboundDetails(Integer current, Integer size, Long inboundId, Long materialId, Long batchId, Long locationId) {
        Page<WmsInboundDetail> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildInboundDetailQuery(inboundId, materialId, batchId, locationId));
    }

    /**
     * 查询入库明细列表，支持入库单、物料、批次和库位筛选。
     * @param inboundId 入库单ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @param locationId 库位ID
     * @return 入库明细列表
     */
    @Override
    public List<WmsInboundDetail> listInboundDetails(Long inboundId, Long materialId, Long batchId, Long locationId) {
        return baseMapper.selectList(buildInboundDetailQuery(inboundId, materialId, batchId, locationId));
    }

    /**
     * 根据入库单ID查询未删除的入库明细列表。
     * @param inboundId 入库单ID
     * @return 入库明细列表
     */
    @Override
    public List<WmsInboundDetail> listInboundDetailsByInboundId(Long inboundId) {
        return baseMapper.selectList(buildInboundDetailQuery(inboundId, null, null, null));
    }

    /**
     * 根据入库明细ID查询未删除的入库明细详情。
     * @param detailId 入库明细ID
     * @return 入库明细实体
     */
    @Override
    public WmsInboundDetail getInboundDetailById(Long detailId) {
        LambdaQueryWrapper<WmsInboundDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsInboundDetail::getDetailId, detailId)
                .eq(WmsInboundDetail::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 新增入库明细，自动设置删除标志为0。
     * @param inboundDetail 入库明细实体
     * @return 是否成功
     */
    @Override
    public boolean saveInboundDetail(WmsInboundDetail inboundDetail) {
        if (inboundDetail.getDeletedFlag() == null) {
            inboundDetail.setDeletedFlag(0);
        }
        return baseMapper.insert(inboundDetail) > 0;
    }

    /**
     * 更新入库明细信息，不允许修改删除标志。
     * @param inboundDetail 入库明细实体
     * @return 是否成功
     */
    @Override
    public boolean updateInboundDetail(WmsInboundDetail inboundDetail) {
        if (inboundDetail.getDetailId() == null) {
            return false;
        }
        inboundDetail.setDeletedFlag(null);
        return baseMapper.updateById(inboundDetail) > 0;
    }

    /**
     * 根据ID删除入库明细（软删除，将删除标志设为1）。
     * @param detailId 入库明细ID
     * @return 是否成功
     */
    @Override
    public boolean removeInboundDetailById(Long detailId) {
        WmsInboundDetail inboundDetail = new WmsInboundDetail();
        inboundDetail.setDetailId(detailId);
        inboundDetail.setDeletedFlag(1);
        return baseMapper.updateById(inboundDetail) > 0;
    }

    /**
     * 构建入库明细查询条件，默认过滤已删除记录。
     * @param inboundId 入库单ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @param locationId 库位ID
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<WmsInboundDetail> buildInboundDetailQuery(Long inboundId, Long materialId, Long batchId, Long locationId) {
        LambdaQueryWrapper<WmsInboundDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsInboundDetail::getDeletedFlag, 0)
                .eq(inboundId != null, WmsInboundDetail::getInboundId, inboundId)
                .eq(materialId != null, WmsInboundDetail::getMaterialId, materialId)
                .eq(batchId != null, WmsInboundDetail::getBatchId, batchId)
                .eq(locationId != null, WmsInboundDetail::getLocationId, locationId)
                .orderByDesc(WmsInboundDetail::getCreatedTime);
        return queryWrapper;
    }
}
