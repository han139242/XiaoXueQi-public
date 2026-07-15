package com.neuedu.ems.service.impl.wms;

import com.neuedu.ems.entity.wms.WmsMaterialBatch;
import com.neuedu.ems.mapper.wms.WmsMaterialBatchMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WmsMaterialBatchServiceImplTest {

    private WmsMaterialBatchServiceImpl service;
    private WmsMaterialBatchMapper batchMapper;

    @BeforeEach
    void setUp() {
        service = new WmsMaterialBatchServiceImpl();
        batchMapper = mock(WmsMaterialBatchMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", batchMapper);
    }

    @Test
    void createAlwaysStartsBatchInInspectionQuarantine() {
        when(batchMapper.insert(any(WmsMaterialBatch.class))).thenReturn(1);
        WmsMaterialBatch request = new WmsMaterialBatch();
        request.setBatchNo("P0-BATCH-001");
        request.setMaterialId(6L);
        request.setQualityStatus("PASS");
        request.setStatus("AVAILABLE");

        assertTrue(service.saveMaterialBatch(request));

        ArgumentCaptor<WmsMaterialBatch> inserted = ArgumentCaptor.forClass(WmsMaterialBatch.class);
        verify(batchMapper).insert(inserted.capture());
        assertEquals("WAIT_INSPECTION", inserted.getValue().getQualityStatus());
        assertEquals("FROZEN", inserted.getValue().getStatus());
    }

    @Test
    void ordinaryUpdateCannotBypassInspectionResultOrBatchStatus() {
        WmsMaterialBatch existing = new WmsMaterialBatch();
        existing.setBatchId(7L);
        existing.setVersion(3);
        existing.setQualityStatus("WAIT_INSPECTION");
        existing.setStatus("FROZEN");
        when(batchMapper.selectOne(any())).thenReturn(existing);
        when(batchMapper.updateById(any(WmsMaterialBatch.class))).thenReturn(1);
        WmsMaterialBatch request = new WmsMaterialBatch();
        request.setBatchId(7L);
        request.setQualityStatus("PASS");
        request.setStatus("AVAILABLE");

        assertTrue(service.updateMaterialBatch(request));

        ArgumentCaptor<WmsMaterialBatch> updated = ArgumentCaptor.forClass(WmsMaterialBatch.class);
        verify(batchMapper).updateById(updated.capture());
        assertNull(updated.getValue().getQualityStatus());
        assertNull(updated.getValue().getStatus());
        assertEquals(3, updated.getValue().getVersion());
    }
}
