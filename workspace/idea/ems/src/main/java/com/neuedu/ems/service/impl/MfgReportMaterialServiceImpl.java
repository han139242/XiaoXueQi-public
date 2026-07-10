package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.MfgReportMaterial;
import com.neuedu.ems.entity.MfgWorkReport;
import com.neuedu.ems.mapper.MfgReportMaterialMapper;
import com.neuedu.ems.mapper.MfgWorkReportMapper;
import com.neuedu.ems.service.MfgReportMaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 报工用料消耗 Service 实现类。
 */
@Service
public class MfgReportMaterialServiceImpl extends ServiceImpl<MfgReportMaterialMapper, MfgReportMaterial> implements MfgReportMaterialService {

    private static final String APPROVED_STATUS = "APPROVED";

    @Autowired
    private MfgWorkReportMapper mfgWorkReportMapper;

    /**
     * 分页查询报工用料消耗，支持报工、物料和批次筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param reportId 报工ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @return 报工用料分页结果
     */
    @Override
    public Page<MfgReportMaterial> pageReportMaterials(Integer current, Integer size, Long reportId, Long materialId, Long batchId) {
        Page<MfgReportMaterial> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildReportMaterialQuery(reportId, materialId, batchId));
    }

    /**
     * 查询报工用料消耗列表，支持报工、物料和批次筛选。
     * @param reportId 报工ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @return 报工用料列表
     */
    @Override
    public List<MfgReportMaterial> listReportMaterials(Long reportId, Long materialId, Long batchId) {
        return baseMapper.selectList(buildReportMaterialQuery(reportId, materialId, batchId));
    }

    /**
     * 根据报工ID查询未删除的用料明细。
     * @param reportId 报工ID
     * @return 用料明细列表
     */
    @Override
    public List<MfgReportMaterial> listReportMaterialsByReportId(Long reportId) {
        return baseMapper.selectList(buildReportMaterialQuery(reportId, null, null));
    }

    /**
     * 根据ID查询未删除的报工用料。
     * @param id 报工用料ID
     * @return 报工用料实体
     */
    @Override
    public MfgReportMaterial getReportMaterialById(Long id) {
        LambdaQueryWrapper<MfgReportMaterial> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MfgReportMaterial::getId, id)
                .eq(MfgReportMaterial::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 新增报工用料，自动补齐数量和删除标记。
     * @param reportMaterial 报工用料实体
     * @return 是否成功
     */
    @Override
    public boolean saveReportMaterial(MfgReportMaterial reportMaterial) {
        if (!canChangeReport(reportMaterial.getReportId())) {
            return false;
        }
        prepareReportMaterial(reportMaterial);
        return baseMapper.insert(reportMaterial) > 0;
    }

    /**
     * 修改报工用料，已审核报工不允许修改明细。
     * @param reportMaterial 报工用料实体
     * @return 是否成功
     */
    @Override
    public boolean updateReportMaterial(MfgReportMaterial reportMaterial) {
        if (reportMaterial.getId() == null) {
            return false;
        }
        MfgReportMaterial existing = getReportMaterialById(reportMaterial.getId());
        if (existing == null || !canChangeReport(existing.getReportId())) {
            return false;
        }
        if (reportMaterial.getReportId() != null && !canChangeReport(reportMaterial.getReportId())) {
            return false;
        }
        fillMissingQuantities(reportMaterial, existing);
        prepareReportMaterial(reportMaterial);
        reportMaterial.setDeletedFlag(null);
        return baseMapper.updateById(reportMaterial) > 0;
    }

    /**
     * 根据ID软删除报工用料，已审核报工不允许删除明细。
     * @param id 报工用料ID
     * @return 是否成功
     */
    @Override
    public boolean removeReportMaterialById(Long id) {
        MfgReportMaterial existing = getReportMaterialById(id);
        if (existing == null || !canChangeReport(existing.getReportId())) {
            return false;
        }
        MfgReportMaterial reportMaterial = new MfgReportMaterial();
        reportMaterial.setId(id);
        reportMaterial.setDeletedFlag(1);
        return baseMapper.updateById(reportMaterial) > 0;
    }

    /**
     * 构建报工用料查询条件，默认过滤已删除记录。
     * @param reportId 报工ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<MfgReportMaterial> buildReportMaterialQuery(Long reportId, Long materialId, Long batchId) {
        LambdaQueryWrapper<MfgReportMaterial> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MfgReportMaterial::getDeletedFlag, 0)
                .eq(reportId != null, MfgReportMaterial::getReportId, reportId)
                .eq(materialId != null, MfgReportMaterial::getMaterialId, materialId)
                .eq(batchId != null, MfgReportMaterial::getBatchId, batchId)
                .orderByDesc(MfgReportMaterial::getCreatedTime);
        return queryWrapper;
    }

    /**
     * 校验报工单是否允许修改用料明细。
     * @param reportId 报工ID，可为空
     * @return 是否允许修改
     */
    private boolean canChangeReport(Long reportId) {
        if (reportId == null) {
            return true;
        }
        LambdaQueryWrapper<MfgWorkReport> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MfgWorkReport::getReportId, reportId)
                .eq(MfgWorkReport::getDeletedFlag, 0);
        MfgWorkReport workReport = mfgWorkReportMapper.selectOne(queryWrapper);
        return workReport != null && !APPROVED_STATUS.equals(workReport.getAuditStatus());
    }

    /**
     * 补齐报工用料数量和删除标记默认值。
     * @param reportMaterial 报工用料实体
     */
    private void prepareReportMaterial(MfgReportMaterial reportMaterial) {
        if (reportMaterial.getUsedQuantity() == null) {
            reportMaterial.setUsedQuantity(BigDecimal.ZERO);
        }
        if (reportMaterial.getScrapQuantity() == null) {
            reportMaterial.setScrapQuantity(BigDecimal.ZERO);
        }
        if (reportMaterial.getDeletedFlag() == null) {
            reportMaterial.setDeletedFlag(0);
        }
    }

    /**
     * 部分更新报工用料时，用已有数量回填未传字段。
     * @param reportMaterial 请求中的报工用料实体
     * @param existing 数据库中已有的报工用料实体
     */
    private void fillMissingQuantities(MfgReportMaterial reportMaterial, MfgReportMaterial existing) {
        if (reportMaterial.getUsedQuantity() == null) {
            reportMaterial.setUsedQuantity(existing.getUsedQuantity());
        }
        if (reportMaterial.getScrapQuantity() == null) {
            reportMaterial.setScrapQuantity(existing.getScrapQuantity());
        }
    }
}
