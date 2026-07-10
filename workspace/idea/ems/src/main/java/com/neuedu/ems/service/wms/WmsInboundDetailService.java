package com.neuedu.ems.service.wms;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.wms.WmsInboundDetail;

import java.util.List;

/**
 * 入库明细管理 Service 接口。
 */
public interface WmsInboundDetailService extends IService<WmsInboundDetail> {

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
    Page<WmsInboundDetail> pageInboundDetails(Integer current, Integer size, Long inboundId, Long materialId, Long batchId, Long locationId);

    /**
     * 查询入库明细列表，支持入库单、物料、批次和库位筛选。
     * @param inboundId 入库单ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @param locationId 库位ID
     * @return 入库明细列表
     */
    List<WmsInboundDetail> listInboundDetails(Long inboundId, Long materialId, Long batchId, Long locationId);

    /**
     * 根据入库单ID查询入库明细列表。
     * @param inboundId 入库单ID
     * @return 入库明细列表
     */
    List<WmsInboundDetail> listInboundDetailsByInboundId(Long inboundId);

    /**
     * 根据入库明细ID查询详情。
     * @param detailId 入库明细ID
     * @return 入库明细实体
     */
    WmsInboundDetail getInboundDetailById(Long detailId);

    /**
     * 新增入库明细。
     * @param inboundDetail 入库明细实体
     * @return 是否成功
     */
    boolean saveInboundDetail(WmsInboundDetail inboundDetail);

    /**
     * 更新入库明细信息。
     * @param inboundDetail 入库明细实体
     * @return 是否成功
     */
    boolean updateInboundDetail(WmsInboundDetail inboundDetail);

    /**
     * 根据ID删除入库明细（软删除）。
     * @param detailId 入库明细ID
     * @return 是否成功
     */
    boolean removeInboundDetailById(Long detailId);
}
