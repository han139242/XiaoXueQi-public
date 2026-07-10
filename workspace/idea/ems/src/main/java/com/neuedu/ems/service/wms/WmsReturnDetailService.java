package com.neuedu.ems.service.wms;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.wms.WmsReturnDetail;

import java.util.List;

/**
 * 退料明细管理 Service 接口。
 */
public interface WmsReturnDetailService extends IService<WmsReturnDetail> {

    /**
     * 分页查询退料明细，支持退料单、物料和批次筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param returnId 退料单ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @return 退料明细分页结果
     */
    Page<WmsReturnDetail> pageReturnDetails(Integer current, Integer size, Long returnId, Long materialId, Long batchId);

    /**
     * 查询退料明细列表，支持退料单、物料和批次筛选。
     * @param returnId 退料单ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @return 退料明细列表
     */
    List<WmsReturnDetail> listReturnDetails(Long returnId, Long materialId, Long batchId);

    /**
     * 根据退料单ID查询未删除的退料明细。
     * @param returnId 退料单ID
     * @return 退料明细列表
     */
    List<WmsReturnDetail> listReturnDetailsByReturnId(Long returnId);

    /**
     * 根据退料明细ID查询详情。
     * @param detailId 退料明细ID
     * @return 退料明细实体
     */
    WmsReturnDetail getReturnDetailById(Long detailId);

    /**
     * 新增退料明细。
     * @param returnDetail 退料明细实体
     * @return 是否成功
     */
    boolean saveReturnDetail(WmsReturnDetail returnDetail);

    /**
     * 更新退料明细。
     * @param returnDetail 退料明细实体
     * @return 是否成功
     */
    boolean updateReturnDetail(WmsReturnDetail returnDetail);

    /**
     * 根据ID软删除退料明细。
     * @param detailId 退料明细ID
     * @return 是否成功
     */
    boolean removeReturnDetailById(Long detailId);
}
