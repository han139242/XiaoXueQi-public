package com.neuedu.ems.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessStatusTest {

    @Test
    void normalizesEnglishCodesAndRejectsChineseStatus() {
        assertEquals("WAIT_START", BusinessStatus.normalizeCode(" wait_start "));
        assertNull(BusinessStatus.normalizeCode("生产中"));
        assertTrue(BusinessStatus.isInspectionResult(BusinessStatus.Quality.PASS));
        assertTrue(BusinessStatus.isApprovalResult(BusinessStatus.Document.REJECTED));
    }
}
