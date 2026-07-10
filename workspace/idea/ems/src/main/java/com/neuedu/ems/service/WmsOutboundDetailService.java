package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.WmsOutboundDetail;

import java.util.List;

/**
 * 出库明细管理 Service 接口。
 */
public interface WmsOutboundDetailService extends IService<WmsOutboundDetail> {

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
    Page<WmsOutboundDetail> pageOutboundDetails(Integer current, Integer size, Long outboundId, Long materialId, Long batchId, Long locationId);

    /**
     * 查询出库明细列表，支持出库单、物料、批次和库位筛选。
     * @param outboundId 出库单ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @param locationId 库位ID
     * @return 出库明细列表
     */
    List<WmsOutboundDetail> listOutboundDetails(Long outboundId, Long materialId, Long batchId, Long locationId);

    /**
     * 根据出库单ID查询未删除的出库明细。
     * @param outboundId 出库单ID
     * @return 出库明细列表
     */
    List<WmsOutboundDetail> listOutboundDetailsByOutboundId(Long outboundId);

    /**
     * 根据出库明细ID查询详情。
     * @param detailId 出库明细ID
     * @return 出库明细实体
     */
    WmsOutboundDetail getOutboundDetailById(Long detailId);

    /**
     * 新增出库明细。
     * @param outboundDetail 出库明细实体
     * @return 是否成功
     */
    boolean saveOutboundDetail(WmsOutboundDetail outboundDetail);

    /**
     * 更新出库明细。
     * @param outboundDetail 出库明细实体
     * @return 是否成功
     */
    boolean updateOutboundDetail(WmsOutboundDetail outboundDetail);

    /**
     * 根据ID软删除出库明细。
     * @param detailId 出库明细ID
     * @return 是否成功
     */
    boolean removeOutboundDetailById(Long detailId);
}
