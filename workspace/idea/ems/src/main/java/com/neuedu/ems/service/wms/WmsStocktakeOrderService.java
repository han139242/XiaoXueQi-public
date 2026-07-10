package com.neuedu.ems.service.wms;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.wms.WmsStocktakeDetail;
import com.neuedu.ems.entity.wms.WmsStocktakeOrder;

import java.util.List;

/**
 * 盘点单管理 Service 接口。
 */
public interface WmsStocktakeOrderService extends IService<WmsStocktakeOrder> {

    /**
     * 分页查询盘点单，支持单号、仓库、盘点类型和盘点状态筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字，匹配盘点单号
     * @param warehouseId 仓库ID
     * @param stocktakeType 盘点类型
     * @param stocktakeStatus 盘点状态
     * @return 盘点单分页结果
     */
    Page<WmsStocktakeOrder> pageStocktakeOrders(Integer current, Integer size, String keyword, Long warehouseId,
                                                String stocktakeType, String stocktakeStatus);

    /**
     * 查询盘点单列表，支持单号、仓库、盘点类型和盘点状态筛选。
     * @param keyword 关键字，匹配盘点单号
     * @param warehouseId 仓库ID
     * @param stocktakeType 盘点类型
     * @param stocktakeStatus 盘点状态
     * @return 盘点单列表
     */
    List<WmsStocktakeOrder> listStocktakeOrders(String keyword, Long warehouseId, String stocktakeType, String stocktakeStatus);

    /**
     * 根据盘点单ID查询详情。
     * @param stocktakeId 盘点单ID
     * @return 盘点单实体
     */
    WmsStocktakeOrder getStocktakeOrderById(Long stocktakeId);

    /**
     * 新增盘点单。
     * @param stocktakeOrder 盘点单实体
     * @return 是否成功
     */
    boolean saveStocktakeOrder(WmsStocktakeOrder stocktakeOrder);

    /**
     * 新增盘点单并保存明细。
     * @param stocktakeOrder 盘点单实体
     * @param details 盘点明细列表
     * @return 盘点单ID
     */
    Long saveStocktakeOrderFull(WmsStocktakeOrder stocktakeOrder, List<WmsStocktakeDetail> details);

    /**
     * 更新盘点单。
     * @param stocktakeOrder 盘点单实体
     * @return 是否成功
     */
    boolean updateStocktakeOrder(WmsStocktakeOrder stocktakeOrder);

    /**
     * 更新盘点单并替换明细。
     * @param stocktakeOrder 盘点单实体
     * @param details 盘点明细列表
     * @return 是否成功
     */
    boolean updateStocktakeOrderFull(WmsStocktakeOrder stocktakeOrder, List<WmsStocktakeDetail> details);

    /**
     * 根据ID软删除盘点单和明细。
     * @param stocktakeId 盘点单ID
     * @return 是否成功
     */
    boolean removeStocktakeOrderById(Long stocktakeId);

    /**
     * 完成盘点，根据差异调整库存并写入库存流水。
     * @param stocktakeId 盘点单ID
     * @param operatorId 操作人ID
     * @param remark 备注
     * @return 是否成功
     */
    boolean finishStocktakeOrder(Long stocktakeId, Long operatorId, String remark);
}
