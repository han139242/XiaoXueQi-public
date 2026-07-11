package com.neuedu.ems.service.impl.sys;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SysFileServiceImplTest {

    private SysFileServiceImpl fileService;
    private Path storageRoot;

    @BeforeEach
    void setUp() {
        fileService = new SysFileServiceImpl();
        storageRoot = Path.of("D:/upload/file").toAbsolutePath().normalize();
        ReflectionTestUtils.setField(fileService, "rootPath", storageRoot.toString());
    }

    @Test
    void resolvesUploadUrlInsideStorageRoot() {
        String resolved = ReflectionTestUtils.invokeMethod(fileService, "resolveStoragePath", "/upload/a/b.pdf");

        assertEquals(storageRoot.resolve("a/b.pdf").toString(), resolved);
    }

    @Test
    void rejectsPathOutsideStorageRoot() {
        assertThrows(RuntimeException.class,
                () -> ReflectionTestUtils.invokeMethod(fileService, "resolveStoragePath", "/upload/../../secret.txt"));
    }
}
