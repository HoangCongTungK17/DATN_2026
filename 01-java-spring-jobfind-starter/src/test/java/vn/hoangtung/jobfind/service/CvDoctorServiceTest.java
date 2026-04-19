package vn.hoangtung.jobfind.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.InputStream;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class CvDoctorServiceTest {

    @Autowired
    private CvDoctorService cvDoctorService;

    @Test
    void testExtractTextFromPdf_withValidPdf() throws Exception {
        // Đọc file PDF test từ resources
        InputStream pdfStream = getClass().getResourceAsStream("/test-cv.pdf");

        if (pdfStream == null) {
            System.out.println("⚠️ Chưa có file test-cv.pdf trong src/test/resources/");
            System.out.println("   Đặt 1 file CV PDF vào đó rồi chạy lại test");
            return;
        }

        MockMultipartFile file = new MockMultipartFile(
                "file", // tên param
                "test-cv.pdf", // tên file gốc
                "application/pdf", // content type
                pdfStream // nội dung
        );

        String text = cvDoctorService.extractTextFromPdf(file);

        // Kiểm tra kết quả
        assertNotNull(text);
        assertFalse(text.isEmpty());
        assertTrue(text.length() >= 100);

        System.out.println("========== KẾT QUẢ TRÍCH XUẤT CV ==========");
        System.out.println("Độ dài: " + text.length() + " ký tự");
        System.out.println("--- Nội dung (200 ký tự đầu) ---");
        System.out.println(text.substring(0, Math.min(200, text.length())));
        System.out.println("==============================================");
    }

    @Test
    void testExtractTextFromPdf_withEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> {
            cvDoctorService.extractTextFromPdf(file);
        });
    }

    @Test
    void testExtractTextFromPdf_withNonPdfFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "document.docx", "application/msword", "fake content".getBytes());

        assertThrows(IllegalArgumentException.class, () -> {
            cvDoctorService.extractTextFromPdf(file);
        });
    }

    @Test
    void testExtractTextFromPdf_withTooLargeFile() {
        // Tạo file giả > 5MB
        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.pdf", "application/pdf", largeContent);

        assertThrows(IllegalArgumentException.class, () -> {
            cvDoctorService.extractTextFromPdf(file);
        });
    }
}
