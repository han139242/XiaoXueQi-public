package com.neuedu.ems.service.impl.mfg;

import com.neuedu.ems.entity.factory.FactoryLine;
import com.neuedu.ems.entity.mdm.MdmProduct;
import com.neuedu.ems.entity.mfg.MfgLineProductCapability;
import com.neuedu.ems.entity.process.ProcessRoute;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.factory.FactoryLineMapper;
import com.neuedu.ems.mapper.mdm.MdmProductMapper;
import com.neuedu.ems.mapper.mfg.MfgLineProductCapabilityMapper;
import com.neuedu.ems.mapper.process.ProcessRouteMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MfgLineProductCapabilityServiceImplTest {

    private MfgLineProductCapabilityServiceImpl service;
    private MfgLineProductCapabilityMapper capabilityMapper;
    private FactoryLineMapper lineMapper;
    private MdmProductMapper productMapper;
    private ProcessRouteMapper routeMapper;

    @BeforeEach
    void setUp() {
        service = new MfgLineProductCapabilityServiceImpl();
        capabilityMapper = mock(MfgLineProductCapabilityMapper.class);
        lineMapper = mock(FactoryLineMapper.class);
        productMapper = mock(MdmProductMapper.class);
        routeMapper = mock(ProcessRouteMapper.class);

        ReflectionTestUtils.setField(service, "baseMapper", capabilityMapper);
        ReflectionTestUtils.setField(service, "factoryLineMapper", lineMapper);
        ReflectionTestUtils.setField(service, "mdmProductMapper", productMapper);
        ReflectionTestUtils.setField(service, "processRouteMapper", routeMapper);
    }

    @Test
    void saveAcceptsReleasedRouteForTheSelectedProductAndSanitizesManagedFields() {
        MfgLineProductCapability capability = capability();
        capability.setDeletedFlag(1);
        capability.setCreatedBy(99L);
        capability.setVersion(7);
        stubValidReferences(2L);
        when(capabilityMapper.selectCount(any())).thenReturn(0L);
        when(capabilityMapper.insert(any(MfgLineProductCapability.class))).thenReturn(1);

        assertTrue(service.saveCapability(capability));

        ArgumentCaptor<MfgLineProductCapability> insert = ArgumentCaptor.forClass(MfgLineProductCapability.class);
        verify(capabilityMapper).insert(insert.capture());
        assertEquals("ENABLE", insert.getValue().getStatus());
        assertEquals(0, insert.getValue().getDeletedFlag());
        assertNull(insert.getValue().getCreatedBy());
        assertNull(insert.getValue().getVersion());
    }

    @Test
    void saveRejectsRouteThatBelongsToAnotherProduct() {
        MfgLineProductCapability capability = capability();
        stubValidReferences(3L);

        assertThrows(BusinessException.class, () -> service.saveCapability(capability));
    }

    @Test
    void saveRejectsDuplicateLineProductRouteCombination() {
        MfgLineProductCapability capability = capability();
        stubValidReferences(2L);
        when(capabilityMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.saveCapability(capability));
    }

    @Test
    void disableUpdatesStatusInsteadOfDeletingCapability() {
        MfgLineProductCapability existing = capability();
        existing.setCapabilityId(8L);
        existing.setStatus("ENABLE");
        existing.setVersion(2);
        when(capabilityMapper.selectOne(any())).thenReturn(existing);
        when(capabilityMapper.updateById(any(MfgLineProductCapability.class))).thenReturn(1);

        assertTrue(service.disableCapability(8L));

        ArgumentCaptor<MfgLineProductCapability> update = ArgumentCaptor.forClass(MfgLineProductCapability.class);
        verify(capabilityMapper).updateById(update.capture());
        assertEquals("DISABLE", update.getValue().getStatus());
        assertEquals(2, update.getValue().getVersion());
        assertNull(update.getValue().getDeletedFlag());
    }

    private void stubValidReferences(Long routeProductId) {
        when(lineMapper.selectOne(any())).thenReturn(new FactoryLine());
        when(productMapper.selectOne(any())).thenReturn(new MdmProduct());
        ProcessRoute route = new ProcessRoute();
        route.setProductId(routeProductId);
        route.setStatus("RELEASED");
        when(routeMapper.selectOne(any())).thenReturn(route);
    }

    private MfgLineProductCapability capability() {
        MfgLineProductCapability capability = new MfgLineProductCapability();
        capability.setLineId(1L);
        capability.setProductId(2L);
        capability.setRouteId(3L);
        capability.setPriorityLevel(100);
        capability.setCapacityFactor(BigDecimal.ONE);
        return capability;
    }
}
