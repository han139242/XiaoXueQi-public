package com.neuedu.ems.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuedu.ems.entity.org.OrgShift;
import com.neuedu.ems.entity.process.ProcessOperation;
import com.neuedu.ems.entity.process.ProcessRouteItem;
import com.neuedu.ems.mapper.org.OrgShiftMapper;
import com.neuedu.ems.mapper.process.ProcessOperationMapper;
import com.neuedu.ems.mapper.process.ProcessRouteItemMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class MasterDataTypeIntegrationTest {

    @Autowired
    private OrgShiftMapper orgShiftMapper;

    @Autowired
    private ProcessOperationMapper processOperationMapper;

    @Autowired
    private ProcessRouteItemMapper processRouteItemMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void persistsAndSerializesShiftTimesAndStandardMinutes() throws Exception {
        OrgShift shift = new OrgShift();
        shift.setShiftCode("TYPE-TEST-" + System.nanoTime());
        shift.setShiftName("类型映射测试班次");
        shift.setStartTime(LocalTime.of(8, 0));
        shift.setEndTime(LocalTime.of(16, 0));
        shift.setDeletedFlag(0);
        assertEquals(1, orgShiftMapper.insert(shift));

        OrgShift storedShift = orgShiftMapper.selectById(shift.getShiftId());
        assertEquals(LocalTime.of(8, 0), storedShift.getStartTime());
        assertEquals(LocalTime.of(16, 0), storedShift.getEndTime());
        String shiftJson = objectMapper.writeValueAsString(storedShift);
        assertTrue(shiftJson.contains("\"startTime\":\"08:00:00\""));
        assertTrue(shiftJson.contains("\"endTime\":\"16:00:00\""));

        ProcessOperation operation = new ProcessOperation();
        operation.setOperationCode("TYPE-TEST-" + System.nanoTime());
        operation.setOperationName("类型映射测试工序");
        operation.setOperationType("PRODUCTION");
        operation.setStandardTime(new BigDecimal("0.35"));
        operation.setStatus("ENABLE");
        operation.setDeletedFlag(0);
        assertEquals(1, processOperationMapper.insert(operation));

        ProcessOperation storedOperation = processOperationMapper.selectById(operation.getOperationId());
        assertEquals(0, new BigDecimal("0.35").compareTo(storedOperation.getStandardTime()));

        ProcessRouteItem routeItem = new ProcessRouteItem();
        routeItem.setOperationId(operation.getOperationId());
        routeItem.setSequenceNo(10);
        routeItem.setStandardTime(new BigDecimal("0.45"));
        routeItem.setQcRequired(0);
        routeItem.setDeletedFlag(0);
        assertEquals(1, processRouteItemMapper.insert(routeItem));

        ProcessRouteItem storedRouteItem = processRouteItemMapper.selectById(routeItem.getRouteItemId());
        assertEquals(0, new BigDecimal("0.45").compareTo(storedRouteItem.getStandardTime()));
    }
}
