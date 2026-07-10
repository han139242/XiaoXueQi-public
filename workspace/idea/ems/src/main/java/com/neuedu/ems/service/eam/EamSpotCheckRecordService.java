package com.neuedu.ems.service.eam;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.eam.EamSpotCheckRecord;

import java.util.List;

/**
 * 点检记录 Service 接口。
 */
public interface EamSpotCheckRecordService extends IService<EamSpotCheckRecord> {

    /**
     * 分页查询点检记录，支持计划、设备、点检人和点检结果筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param planId 点检计划ID
     * @param equipmentId 设备ID
     * @param checkerId 点检人ID
     * @param checkResult 点检结果
     * @return 分页结果
     */
    Page<EamSpotCheckRecord> pageSpotCheckRecords(Integer current, Integer size, Long planId, Long equipmentId,
                                                  Long checkerId, String checkResult);

    /**
     * 查询点检记录列表，支持计划、设备、点检人和点检结果筛选。
     * @param planId 点检计划ID
     * @param equipmentId 设备ID
     * @param checkerId 点检人ID
     * @param checkResult 点检结果
     * @return 点检记录列表
     */
    List<EamSpotCheckRecord> listSpotCheckRecords(Long planId, Long equipmentId, Long checkerId, String checkResult);

    /**
     * 根据点检记录ID查询未删除的记录详情。
     * @param recordId 点检记录ID
     * @return 点检记录实体
     */
    EamSpotCheckRecord getSpotCheckRecordById(Long recordId);

    /**
     * 新增点检记录。
     * @param spotCheckRecord 点检记录实体
     * @return 是否成功
     */
    boolean saveSpotCheckRecord(EamSpotCheckRecord spotCheckRecord);

    /**
     * 更新点检记录。
     * @param spotCheckRecord 点检记录实体，必须包含recordId
     * @return 是否成功
     */
    boolean updateSpotCheckRecord(EamSpotCheckRecord spotCheckRecord);

    /**
     * 根据ID软删除点检记录。
     * @param recordId 点检记录ID
     * @return 是否成功
     */
    boolean removeSpotCheckRecordById(Long recordId);
}
