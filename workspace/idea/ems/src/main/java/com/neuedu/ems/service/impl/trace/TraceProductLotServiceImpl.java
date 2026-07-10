package com.neuedu.ems.service.impl.trace;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.qms.QmsInspectionOrder;
import com.neuedu.ems.entity.qms.QmsInspectionResult;
import com.neuedu.ems.entity.trace.TraceMaterialUsage;
import com.neuedu.ems.entity.trace.TraceProductLot;
import com.neuedu.ems.mapper.qms.QmsInspectionOrderMapper;
import com.neuedu.ems.mapper.qms.QmsInspectionResultMapper;
import com.neuedu.ems.mapper.trace.TraceMaterialUsageMapper;
import com.neuedu.ems.mapper.trace.TraceProductLotMapper;
import com.neuedu.ems.service.trace.TraceProductLotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 产品批次追溯 Service 实现类。
 */
@Service
public class TraceProductLotServiceImpl extends ServiceImpl<TraceProductLotMapper, TraceProductLot>
        implements TraceProductLotService {

    @Autowired
    private TraceMaterialUsageMapper traceMaterialUsageMapper;

    @Autowired
    private QmsInspectionOrderMapper qmsInspectionOrderMapper;

    @Autowired
    private QmsInspectionResultMapper qmsInspectionResultMapper;

    /**
     * 分页查询产品批次，默认过滤已删除记录。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字，匹配批次号
     * @param productId 产品ID
     * @param workOrderId 工单ID
     * @param status 状态
     * @return 分页结果
     */
    @Override
    public Page<TraceProductLot> pageProductLots(Integer current, Integer size, String keyword, Long productId,
                                                 Long workOrderId, String status) {
        Page<TraceProductLot> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildProductLotQuery(keyword, productId, workOrderId, status));
    }

    /**
     * 查询产品批次列表，默认过滤已删除记录。
     * @param keyword 关键字，匹配批次号
     * @param productId 产品ID
     * @param workOrderId 工单ID
     * @param status 状态
     * @return 产品批次列表
     */
    @Override
    public List<TraceProductLot> listProductLots(String keyword, Long productId, Long workOrderId, String status) {
        return baseMapper.selectList(buildProductLotQuery(keyword, productId, workOrderId, status));
    }

    /**
     * 根据产品批次ID查询未删除的批次详情。
     * @param productLotId 产品批次ID
     * @return 产品批次实体
     */
    @Override
    public TraceProductLot getProductLotById(Long productLotId) {
        LambdaQueryWrapper<TraceProductLot> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TraceProductLot::getProductLotId, productLotId)
                .eq(TraceProductLot::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 新增产品批次，自动设置删除标志为0。
     * @param productLot 产品批次实体
     * @return 是否成功
     */
    @Override
    public boolean saveProductLot(TraceProductLot productLot) {
        if (!StringUtils.hasText(productLot.getStatus())) {
            productLot.setStatus(BusinessStatus.Production.RUNNING);
        } else {
            String normalizedStatus = BusinessStatus.normalizeCode(productLot.getStatus());
            if (normalizedStatus == null) {
                return false;
            }
            productLot.setStatus(normalizedStatus);
        }
        if (productLot.getCreatedTime() == null) {
            productLot.setCreatedTime(java.time.LocalDateTime.now());
        }
        productLot.setUpdatedTime(productLot.getCreatedTime());
        if (productLot.getDeletedFlag() == null) {
            productLot.setDeletedFlag(0);
        }
        return baseMapper.insert(productLot) > 0;
    }

    /**
     * 更新产品批次，不允许通过普通更新修改删除标志。
     * @param productLot 产品批次实体
     * @return 是否成功
     */
    @Override
    public boolean updateProductLot(TraceProductLot productLot) {
        if (productLot.getProductLotId() == null) {
            return false;
        }
        if (StringUtils.hasText(productLot.getStatus())) {
            String normalizedStatus = BusinessStatus.normalizeCode(productLot.getStatus());
            if (normalizedStatus == null) {
                return false;
            }
            productLot.setStatus(normalizedStatus);
        }
        productLot.setUpdatedTime(java.time.LocalDateTime.now());
        productLot.setDeletedFlag(null);
        return baseMapper.updateById(productLot) > 0;
    }

    /**
     * 根据ID软删除产品批次。
     * @param productLotId 产品批次ID
     * @return 是否成功
     */
    @Override
    public boolean removeProductLotById(Long productLotId) {
        TraceProductLot productLot = new TraceProductLot();
        productLot.setProductLotId(productLotId);
        productLot.setDeletedFlag(1);
        return baseMapper.updateById(productLot) > 0;
    }

    /**
     * 根据批次号查询产品追溯详情。
     * @param lotNo 产品批次号
     * @return 产品追溯详情
     */
    @Override
    public Map<String, Object> getProductLotTraceByLotNo(String lotNo) {
        LambdaQueryWrapper<TraceProductLot> lotQuery = new LambdaQueryWrapper<>();
        lotQuery.eq(TraceProductLot::getLotNo, lotNo)
                .eq(TraceProductLot::getDeletedFlag, 0);
        TraceProductLot productLot = baseMapper.selectOne(lotQuery);
        if (productLot == null) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("productLotId", productLot.getProductLotId());
        result.put("lotNo", productLot.getLotNo());
        result.put("productId", productLot.getProductId());
        result.put("workOrderId", productLot.getWorkOrderId());
        result.put("batchQuantity", productLot.getBatchQuantity());
        result.put("qualifiedQuantity", productLot.getQualifiedQuantity());
        result.put("defectQuantity", productLot.getDefectQuantity());
        result.put("status", productLot.getStatus());
        result.put("remark", productLot.getRemark());

        List<Map<String, Object>> materials = new ArrayList<>();
        LambdaQueryWrapper<TraceMaterialUsage> materialQuery = new LambdaQueryWrapper<>();
        materialQuery.eq(TraceMaterialUsage::getProductLotId, productLot.getProductLotId())
                .eq(TraceMaterialUsage::getDeletedFlag, 0)
                .orderByAsc(TraceMaterialUsage::getUsageId);
        for (TraceMaterialUsage usage : traceMaterialUsageMapper.selectList(materialQuery)) {
            Map<String, Object> material = new LinkedHashMap<>();
            material.put("materialId", usage.getMaterialId());
            material.put("batchId", usage.getBatchId());
            material.put("usedQuantity", usage.getUsedQuantity());
            materials.add(material);
        }
        result.put("materials", materials);
        result.put("inspection", buildInspectionSummary(productLot));
        return result;
    }

    /**
     * 构建产品批次查询条件。
     * @param keyword 关键字
     * @param productId 产品ID
     * @param workOrderId 工单ID
     * @param status 状态
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<TraceProductLot> buildProductLotQuery(String keyword, Long productId, Long workOrderId,
                                                                     String status) {
        LambdaQueryWrapper<TraceProductLot> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TraceProductLot::getDeletedFlag, 0)
                .like(StringUtils.hasText(keyword), TraceProductLot::getLotNo, keyword)
                .eq(productId != null, TraceProductLot::getProductId, productId)
                .eq(workOrderId != null, TraceProductLot::getWorkOrderId, workOrderId)
                .eq(StringUtils.hasText(status), TraceProductLot::getStatus, status)
                .orderByDesc(TraceProductLot::getProductLotId);
        return queryWrapper;
    }

    /**
     * 组装产品批次对应的检验摘要。
     * @param productLot 产品批次实体
     * @return 检验摘要，未找到则返回null
     */
    private Map<String, Object> buildInspectionSummary(TraceProductLot productLot) {
        LambdaQueryWrapper<QmsInspectionOrder> orderQuery = new LambdaQueryWrapper<>();
        orderQuery.eq(QmsInspectionOrder::getDeletedFlag, 0)
                .eq(productLot.getWorkOrderId() != null, QmsInspectionOrder::getWorkOrderId, productLot.getWorkOrderId())
                .orderByDesc(QmsInspectionOrder::getInspectionId);
        List<QmsInspectionOrder> inspectionOrders = qmsInspectionOrderMapper.selectList(orderQuery);
        QmsInspectionOrder inspectionOrder = inspectionOrders.isEmpty() ? null : inspectionOrders.get(0);
        if (inspectionOrder == null) {
            return null;
        }
        Map<String, Object> inspection = new LinkedHashMap<>();
        inspection.put("inspectionId", inspectionOrder.getInspectionId());
        inspection.put("inspectionResult", StringUtils.hasText(inspectionOrder.getInspectionResult())
                ? inspectionOrder.getInspectionResult()
                : deriveInspectionResult(inspectionOrder.getInspectionId()));
        return inspection;
    }

    /**
     * 从检验结果明细推导检验结论。
     * @param inspectionId 检验单ID
     * @return 检验结论，未找到则返回null
     */
    private String deriveInspectionResult(Long inspectionId) {
        LambdaQueryWrapper<QmsInspectionResult> resultQuery = new LambdaQueryWrapper<>();
        resultQuery.eq(QmsInspectionResult::getInspectionId, inspectionId)
                .eq(QmsInspectionResult::getDeletedFlag, 0);
        List<QmsInspectionResult> results = qmsInspectionResultMapper.selectList(resultQuery);
        if (results.isEmpty()) {
            return null;
        }
        boolean hasUnqualified = results.stream()
                .anyMatch(item -> BusinessStatus.Quality.FAIL.equals(item.getResult()));
        return hasUnqualified ? BusinessStatus.Quality.FAIL : BusinessStatus.Quality.PASS;
    }
}
