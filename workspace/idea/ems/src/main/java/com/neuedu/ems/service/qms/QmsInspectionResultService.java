package com.neuedu.ems.service.qms;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.qms.QmsInspectionResult;

import java.util.List;

/**
 * 检验结果 Service 接口。
 */
public interface QmsInspectionResultService extends IService<QmsInspectionResult> {

    /**
     * 分页查询检验结果，支持检验单、检验项目、结果和图片筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param inspectionId 检验单ID
     * @param itemId 检验项目ID
     * @param result 检验结果
     * @param photoFileId 图片附件ID
     * @return 检验结果分页结果
     */
    Page<QmsInspectionResult> pageInspectionResults(Integer current, Integer size, Long inspectionId, Long itemId, String result, Long photoFileId);

    /**
     * 查询检验结果列表，支持检验单、检验项目、结果和图片筛选。
     * @param inspectionId 检验单ID
     * @param itemId 检验项目ID
     * @param result 检验结果
     * @param photoFileId 图片附件ID
     * @return 检验结果列表
     */
    List<QmsInspectionResult> listInspectionResults(Long inspectionId, Long itemId, String result, Long photoFileId);

    /**
     * 根据检验单ID查询未删除的检验结果。
     * @param inspectionId 检验单ID
     * @return 检验结果列表
     */
    List<QmsInspectionResult> listInspectionResultsByInspectionId(Long inspectionId);

    /**
     * 根据ID查询未删除的检验结果。
     * @param resultId 检验结果ID
     * @return 检验结果实体
     */
    QmsInspectionResult getInspectionResultById(Long resultId);

    /**
     * 新增检验结果。
     * @param inspectionResult 检验结果实体
     * @return 是否成功
     */
    boolean saveInspectionResult(QmsInspectionResult inspectionResult);

    /**
     * 批量保存检验结果。
     * @param inspectionId 检验单ID
     * @param results 检验结果列表
     * @return 是否成功
     */
    boolean saveInspectionResultsBatch(Long inspectionId, List<QmsInspectionResult> results);

    /**
     * 修改检验结果。
     * @param inspectionResult 检验结果实体
     * @return 是否成功
     */
    boolean updateInspectionResult(QmsInspectionResult inspectionResult);

    /**
     * 根据ID软删除检验结果。
     * @param resultId 检验结果ID
     * @return 是否成功
     */
    boolean removeInspectionResultById(Long resultId);
}
