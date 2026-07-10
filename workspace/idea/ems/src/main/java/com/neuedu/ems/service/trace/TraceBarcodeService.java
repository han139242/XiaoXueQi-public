package com.neuedu.ems.service.trace;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.trace.TraceBarcode;

import java.util.List;

/**
 * 条码追溯 Service 接口。
 */
public interface TraceBarcodeService extends IService<TraceBarcode> {

    /**
     * 分页查询条码，支持条码值、条码类型、业务类型、业务ID和状态筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字，匹配条码值或生成规则
     * @param barcodeType 条码类型
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @param status 状态
     * @return 分页结果
     */
    Page<TraceBarcode> pageBarcodes(Integer current, Integer size, String keyword, String barcodeType,
                                    String bizType, Long bizId, String status);

    /**
     * 查询条码列表，支持条码值、条码类型、业务类型、业务ID和状态筛选。
     * @param keyword 关键字，匹配条码值或生成规则
     * @param barcodeType 条码类型
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @param status 状态
     * @return 条码列表
     */
    List<TraceBarcode> listBarcodes(String keyword, String barcodeType, String bizType, Long bizId, String status);

    /**
     * 根据条码ID查询未删除的条码详情。
     * @param barcodeId 条码ID
     * @return 条码实体
     */
    TraceBarcode getBarcodeById(Long barcodeId);

    /**
     * 新增条码。
     * @param barcode 条码实体
     * @return 是否成功
     */
    boolean saveBarcode(TraceBarcode barcode);

    /**
     * 更新条码。
     * @param barcode 条码实体，必须包含barcodeId
     * @return 是否成功
     */
    boolean updateBarcode(TraceBarcode barcode);

    /**
     * 根据ID软删除条码。
     * @param barcodeId 条码ID
     * @return 是否成功
     */
    boolean removeBarcodeById(Long barcodeId);

    /**
     * 按生成规则批量生成条码。
     * @param barcodeType 条码类型
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @param quantity 生成数量
     * @param remark 备注
     * @return 新生成的条码列表
     */
    List<TraceBarcode> generateBarcodes(String barcodeType, String bizType, Long bizId, Integer quantity, String remark);

    /**
     * 根据条码值查询条码详情。
     * @param barcodeValue 条码值
     * @return 条码实体
     */
    TraceBarcode scanBarcode(String barcodeValue);
}
