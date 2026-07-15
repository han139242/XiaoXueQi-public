package com.neuedu.ems.service.mfg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.mfg.MfgReportMaterial;
import com.neuedu.ems.entity.mfg.MfgWorkReport;

import java.util.List;

/**
 * 生产报工 Service 接口。
 */
public interface MfgWorkReportService extends IService<MfgWorkReport> {

    /**
     * 分页查询生产报工，支持编号、任务、人员、工位和审核状态筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 报工编号关键字
     * @param taskId 任务ID
     * @param operatorId 操作工ID
     * @param stationId 工位ID
     * @param auditStatus 审核状态
     * @return 报工分页结果
     */
    Page<MfgWorkReport> pageWorkReports(Integer current, Integer size, String keyword, Long taskId, Long operatorId, Long stationId, String auditStatus);

    /**
     * 查询生产报工列表，支持编号、任务、人员、工位和审核状态筛选。
     * @param keyword 报工编号关键字
     * @param taskId 任务ID
     * @param operatorId 操作工ID
     * @param stationId 工位ID
     * @param auditStatus 审核状态
     * @return 报工列表
     */
    List<MfgWorkReport> listWorkReports(String keyword, Long taskId, Long operatorId, Long stationId, String auditStatus);

    /**
     * 根据ID查询未删除的生产报工。
     * @param reportId 报工ID
     * @return 报工实体
     */
    MfgWorkReport getWorkReportById(Long reportId);

    /**
     * 新增生产报工。
     * @param workReport 报工实体
     * @return 是否成功
     */
    boolean saveWorkReport(MfgWorkReport workReport);

    /**
     * 新增报工主表和用料明细。
     * @param workReport 报工实体
     * @param materials 报工用料明细列表
     * @return 报工ID
     */
    Long saveWorkReportFull(MfgWorkReport workReport, List<MfgReportMaterial> materials);

    /**
     * 修改生产报工。
     * @param workReport 报工实体
     * @return 是否成功
     */
    boolean updateWorkReport(MfgWorkReport workReport);

    /**
     * 修改报工主表并替换用料明细。
     * @param workReport 报工实体
     * @param materials 报工用料明细列表
     * @return 是否成功
     */
    boolean updateWorkReportFull(MfgWorkReport workReport, List<MfgReportMaterial> materials);

    /**
     * 根据ID软删除生产报工及用料明细。
     * @param reportId 报工ID
     * @return 是否成功
     */
    boolean removeWorkReportById(Long reportId);

    /**
     * 提交生产报工并保存用料明细。
     * @param workReport 报工实体
     * @param materials 报工用料明细列表
     * @return 报工ID
     */
    Long submitWorkReport(MfgWorkReport workReport, List<MfgReportMaterial> materials, Long userId);

    void withdrawWorkReport(Long reportId, Long userId, String remark);

    /**
     * 审核生产报工。
     * @param reportId 报工ID
     * @param auditResult 审核结果
     * @param auditComment 审核备注
     * @param operatorId 操作人ID
     * @return 是否成功
     */
    boolean auditWorkReport(Long reportId, String auditResult, String auditComment, Long operatorId);
}
