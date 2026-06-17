package vn.hoangtung.jobfind.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class FileServiceTest {

    @TempDir
    Path tempDir;

    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileService = new FileService();
        ReflectionTestUtils.setField(fileService, "baseURI", tempDir.toUri().toString());
    }

    @Test
    void storeRejectsTraversalFolder() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cv.pdf",
                "application/pdf",
                "%PDF fake".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalArgumentException.class, () -> fileService.store(file, "../resume"));
    }

    @Test
    void storeSanitizesFileNameAndKeepsFileInsideAllowedFolder() throws Exception {
        fileService.createDirectory("resume");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "..\\evil cv.pdf",
                "application/pdf",
                "%PDF fake".getBytes(StandardCharsets.UTF_8));

        String storedName = fileService.store(file, "resume");

        assertFalse(storedName.contains("\\"));
        assertFalse(storedName.contains("/"));
        assertTrue(Files.exists(tempDir.resolve("resume").resolve(storedName)));
    }
}
