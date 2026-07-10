package com.neuedu.ems.service.trace;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.trace.TraceProductLot;

import java.util.List;
import java.util.Map;

/**
 * 产品批次追溯 Service 接口。
 */
public interface TraceProductLotService extends IService<TraceProductLot> {

    /**
     * 分页查询产品批次，支持关键字、产品、工单和状态筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字，匹配批次号
     * @param productId 产品ID
     * @param workOrderId 工单ID
     * @param status 状态
     * @return 分页结果
     */
    Page<TraceProductLot> pageProductLots(Integer current, Integer size, String keyword, Long productId,
                                          Long workOrderId, String status);

    /**
     * 查询产品批次列表，支持关键字、产品、工单和状态筛选。
     * @param keyword 关键字，匹配批次号
     * @param productId 产品ID
     * @param workOrderId 工单ID
     * @param status 状态
     * @return 产品批次列表
     */
    List<TraceProductLot> listProductLots(String keyword, Long productId, Long workOrderId, String status);

    /**
     * 根据产品批次ID查询未删除的批次详情。
     * @param productLotId 产品批次ID
     * @return 产品批次实体
     */
    TraceProductLot getProductLotById(Long productLotId);

    /**
     * 新增产品批次。
     * @param productLot 产品批次实体
     * @return 是否成功
     */
    boolean saveProductLot(TraceProductLot productLot);

    /**
     * 更新产品批次。
     * @param productLot 产品批次实体，必须包含productLotId
     * @return 是否成功
     */
    boolean updateProductLot(TraceProductLot productLot);

    /**
     * 根据ID软删除产品批次。
     * @param productLotId 产品批次ID
     * @return 是否成功
     */
    boolean removeProductLotById(Long productLotId);

    /**
     * 根据批次号查询产品追溯详情。
     * @param lotNo 产品批次号
     * @return 产品追溯详情
     */
    Map<String, Object> getProductLotTraceByLotNo(String lotNo);
}
