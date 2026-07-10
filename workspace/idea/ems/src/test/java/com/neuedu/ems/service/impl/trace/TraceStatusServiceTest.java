package com.neuedu.ems.service.impl.trace;

import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.qms.QmsInspectionOrder;
import com.neuedu.ems.entity.qms.QmsInspectionResult;
import com.neuedu.ems.entity.trace.TraceBarcode;
import com.neuedu.ems.entity.trace.TraceProductLot;
import com.neuedu.ems.mapper.qms.QmsInspectionOrderMapper;
import com.neuedu.ems.mapper.qms.QmsInspectionResultMapper;
import com.neuedu.ems.mapper.trace.TraceBarcodeMapper;
import com.neuedu.ems.mapper.trace.TraceMaterialUsageMapper;
import com.neuedu.ems.mapper.trace.TraceProductLotMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TraceStatusServiceTest {

    @Test
    void generatedBarcodeUsesEnableCode() {
        TraceBarcodeServiceImpl service = new TraceBarcodeServiceImpl();
        TraceBarcodeMapper mapper = mock(TraceBarcodeMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);

        List<TraceBarcode> barcodes = service.generateBarcodes("PRODUCT", "TRACE_PRODUCT_LOT", 1L, 1, null);

        assertEquals(1, barcodes.size());
        assertEquals(BusinessStatus.Common.ENABLE, barcodes.get(0).getStatus());
        ArgumentCaptor<TraceBarcode> inserted = ArgumentCaptor.forClass(TraceBarcode.class);
        verify(mapper).insert(inserted.capture());
        assertEquals(BusinessStatus.Common.ENABLE, inserted.getValue().getStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    void productTraceDerivesFailCodeFromInspectionDetails() {
        TraceProductLotServiceImpl service = new TraceProductLotServiceImpl();
        TraceProductLotMapper productLotMapper = mock(TraceProductLotMapper.class);
        TraceMaterialUsageMapper usageMapper = mock(TraceMaterialUsageMapper.class);
        QmsInspectionOrderMapper orderMapper = mock(QmsInspectionOrderMapper.class);
        QmsInspectionResultMapper resultMapper = mock(QmsInspectionResultMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", productLotMapper);
        ReflectionTestUtils.setField(service, "traceMaterialUsageMapper", usageMapper);
        ReflectionTestUtils.setField(service, "qmsInspectionOrderMapper", orderMapper);
        ReflectionTestUtils.setField(service, "qmsInspectionResultMapper", resultMapper);

        TraceProductLot lot = new TraceProductLot();
        lot.setProductLotId(1L);
        lot.setLotNo("LOT-1");
        lot.setWorkOrderId(10L);
        QmsInspectionOrder inspectionOrder = new QmsInspectionOrder();
        inspectionOrder.setInspectionId(20L);
        QmsInspectionResult inspectionResult = new QmsInspectionResult();
        inspectionResult.setResult(BusinessStatus.Quality.FAIL);
        when(productLotMapper.selectOne(any())).thenReturn(lot);
        when(usageMapper.selectList(any())).thenReturn(List.of());
        when(orderMapper.selectList(any())).thenReturn(List.of(inspectionOrder));
        when(resultMapper.selectList(any())).thenReturn(List.of(inspectionResult));

        Map<String, Object> trace = service.getProductLotTraceByLotNo("LOT-1");

        Map<String, Object> inspection = (Map<String, Object>) trace.get("inspection");
        assertEquals(BusinessStatus.Quality.FAIL, inspection.get("inspectionResult"));
    }
}
