package com.neuedu.ems.service.wms;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.wms.WmsStocktakeDetail;

import java.util.List;

/**
 * 盘点明细管理 Service 接口。
 */
public interface WmsStocktakeDetailService extends IService<WmsStocktakeDetail> {

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
    Page<WmsStocktakeDetail> pageStocktakeDetails(Integer current, Integer size, Long stocktakeId, Long materialId, Long batchId, Long locationId);

    /**
     * 查询盘点明细列表，支持盘点单、物料、批次和库位筛选。
     * @param stocktakeId 盘点单ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @param locationId 库位ID
     * @return 盘点明细列表
     */
    List<WmsStocktakeDetail> listStocktakeDetails(Long stocktakeId, Long materialId, Long batchId, Long locationId);

    /**
     * 根据盘点单ID查询未删除的盘点明细。
     * @param stocktakeId 盘点单ID
     * @return 盘点明细列表
     */
    List<WmsStocktakeDetail> listStocktakeDetailsByStocktakeId(Long stocktakeId);

    /**
     * 根据盘点明细ID查询详情。
     * @param detailId 盘点明细ID
     * @return 盘点明细实体
     */
    WmsStocktakeDetail getStocktakeDetailById(Long detailId);

    /**
     * 新增盘点明细。
     * @param stocktakeDetail 盘点明细实体
     * @return 是否成功
     */
    boolean saveStocktakeDetail(WmsStocktakeDetail stocktakeDetail);

    /**
     * 更新盘点明细。
     * @param stocktakeDetail 盘点明细实体
     * @return 是否成功
     */
    boolean updateStocktakeDetail(WmsStocktakeDetail stocktakeDetail);

    /**
     * 根据ID软删除盘点明细。
     * @param detailId 盘点明细ID
     * @return 是否成功
     */
    boolean removeStocktakeDetailById(Long detailId);
}
