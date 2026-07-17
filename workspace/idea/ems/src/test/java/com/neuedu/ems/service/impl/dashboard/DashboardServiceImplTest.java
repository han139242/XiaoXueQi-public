package com.neuedu.ems.service.impl.dashboard;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.eam.EamEquipment;
import com.neuedu.ems.entity.mfg.MfgTask;
import com.neuedu.ems.entity.qms.QmsInspectionOrder;
import com.neuedu.ems.entity.wms.WmsReplenishmentSuggestion;
import com.neuedu.ems.mapper.eam.EamEquipmentMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgTaskMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkReportMapper;
import com.neuedu.ems.mapper.qms.QmsInspectionOrderMapper;
import com.neuedu.ems.mapper.wms.WmsReplenishmentSuggestionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DashboardServiceImplTest {

    private DashboardServiceImpl service;
    private MfgProductionOrderMapper productionOrderMapper;
    private MfgWorkOrderMapper workOrderMapper;
    private MfgTaskMapper taskMapper;
    private MfgWorkReportMapper workReportMapper;
    private WmsReplenishmentSuggestionMapper replenishmentMapper;
    private EamEquipmentMapper equipmentMapper;
    private QmsInspectionOrderMapper inspectionOrderMapper;

    @BeforeEach
    void setUp() {
        initLambdaMetadata(MfgTask.class);
        initLambdaMetadata(WmsReplenishmentSuggestion.class);
        initLambdaMetadata(EamEquipment.class);
        service = new DashboardServiceImpl();
        productionOrderMapper = mock(MfgProductionOrderMapper.class);
        workOrderMapper = mock(MfgWorkOrderMapper.class);
        taskMapper = mock(MfgTaskMapper.class);
        workReportMapper = mock(MfgWorkReportMapper.class);
        replenishmentMapper = mock(WmsReplenishmentSuggestionMapper.class);
        equipmentMapper = mock(EamEquipmentMapper.class);
        inspectionOrderMapper = mock(QmsInspectionOrderMapper.class);
        ReflectionTestUtils.setField(service, "mfgProductionOrderMapper", productionOrderMapper);
        ReflectionTestUtils.setField(service, "mfgWorkOrderMapper", workOrderMapper);
        ReflectionTestUtils.setField(service, "mfgTaskMapper", taskMapper);
        ReflectionTestUtils.setField(service, "mfgWorkReportMapper", workReportMapper);
        ReflectionTestUtils.setField(service, "wmsReplenishmentSuggestionMapper", replenishmentMapper);
        ReflectionTestUtils.setField(service, "eamEquipmentMapper", equipmentMapper);
        ReflectionTestUtils.setField(service, "qmsInspectionOrderMapper", inspectionOrderMapper);
    }

    @Test
    void summaryQueriesEnglishStatusCodes() {
        when(productionOrderMapper.selectCount(any())).thenReturn(1L);
        when(workOrderMapper.selectCount(any())).thenReturn(2L);
        when(taskMapper.selectCount(any())).thenReturn(3L);
        when(workReportMapper.selectList(any())).thenReturn(List.of());
        when(replenishmentMapper.selectCount(any())).thenReturn(4L);
        when(equipmentMapper.selectCount(any())).thenReturn(5L);

        Map<String, Object> result = service.getSummary(LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 10));

        assertEquals(3L, result.get("runningTaskCount"));
        assertEquals(4L, result.get("inventoryWarningCount"));
        assertEquals(5L, result.get("equipmentFaultCount"));

        ArgumentCaptor<Wrapper<MfgTask>> taskQuery = wrapperCaptor();
        ArgumentCaptor<Wrapper<WmsReplenishmentSuggestion>> replenishmentQuery = wrapperCaptor();
        ArgumentCaptor<Wrapper<EamEquipment>> equipmentQuery = wrapperCaptor();
        verify(taskMapper).selectCount(taskQuery.capture());
        verify(replenishmentMapper).selectCount(replenishmentQuery.capture());
        verify(equipmentMapper).selectCount(equipmentQuery.capture());
        assertTrue(values(taskQuery.getValue()).contains(BusinessStatus.Production.RUNNING));
        assertTrue(values(replenishmentQuery.getValue()).contains(BusinessStatus.Document.SUBMITTED));
        assertTrue(values(equipmentQuery.getValue()).contains(BusinessStatus.Equipment.FAULT));
    }

    @Test
    void qualityStatisticsOnlyCountEnglishQualityResults() {
        QmsInspectionOrder pass = inspection(BusinessStatus.Quality.PASS);
        QmsInspectionOrder fail = inspection(BusinessStatus.Quality.FAIL);
        QmsInspectionOrder concession = inspection(BusinessStatus.Quality.CONCESSION);
        QmsInspectionOrder legacyChinese = inspection("合格");
        when(inspectionOrderMapper.selectList(any())).thenReturn(List.of(pass, fail, concession, legacyChinese));

        Map<String, Object> result = service.getQualityStatistics(
                LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 10));

        assertEquals(4L, result.get("inspectionCount"));
        assertEquals(2L, result.get("evaluatedCount"));
        assertEquals(2L, result.get("pendingCount"));
        assertEquals(1L, result.get("qualifiedCount"));
        assertEquals(1L, result.get("unqualifiedCount"));
        assertEquals(new BigDecimal("50.00"), result.get("qualifiedRate"));
    }

    @Test
    void equipmentStatisticsUseEnglishCodesAndAggregateMaintenance() {
        when(equipmentMapper.selectCount(any())).thenReturn(10L, 4L, 2L, 1L, 3L);

        Map<String, Object> result = service.getEquipmentStatus();

        assertEquals(10L, result.get("running"));
        assertEquals(4L, result.get("idle"));
        assertEquals(2L, result.get("stopped"));
        assertEquals(1L, result.get("fault"));
        assertEquals(3L, result.get("maintenance"));
        ArgumentCaptor<Wrapper<EamEquipment>> queries = wrapperCaptor();
        verify(equipmentMapper, times(5)).selectCount(queries.capture());
        assertTrue(values(queries.getAllValues().get(0)).contains(BusinessStatus.Equipment.RUNNING));
        assertTrue(values(queries.getAllValues().get(1)).contains(BusinessStatus.Equipment.IDLE));
        assertTrue(values(queries.getAllValues().get(2)).contains(BusinessStatus.Equipment.STOPPED));
        assertTrue(values(queries.getAllValues().get(3)).contains(BusinessStatus.Equipment.FAULT));
        Collection<Object> maintenanceValues = values(queries.getAllValues().get(4));
        assertTrue(maintenanceValues.contains(BusinessStatus.Equipment.MAINTAINING));
        assertTrue(maintenanceValues.contains(BusinessStatus.Equipment.REPAIRING));
    }

    private QmsInspectionOrder inspection(String result) {
        QmsInspectionOrder order = new QmsInspectionOrder();
        order.setInspectionResult(result);
        return order;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> ArgumentCaptor<Wrapper<T>> wrapperCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Wrapper.class);
    }

    private Collection<Object> values(Wrapper<?> wrapper) {
        wrapper.getSqlSegment();
        return ((AbstractWrapper<?, ?, ?>) wrapper).getParamNameValuePairs().values();
    }

    private void initLambdaMetadata(Class<?> entityClass) {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), entityClass);
    }
}
