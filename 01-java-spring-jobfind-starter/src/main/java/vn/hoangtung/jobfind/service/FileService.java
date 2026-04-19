package vn.hoangtung.jobfind.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {

    @Value("${hoangtung.upload-file.base-uri}")
    private String baseURI;

    // Tạo thư mục lưu trữ file
    public void createDirectory(String folder) throws URISyntaxException {
        URI uri = new URI(folder);
        Path path = Paths.get(uri);
        File tmpDir = new File(path.toString());
        if (!tmpDir.isDirectory()) {
            try {
                Files.createDirectory(tmpDir.toPath());
                System.out.println(">>> CREATE NEW DIRECTORY SUCCESSFUL, PATH = " + tmpDir.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println(">>> SKIP MAKING DIRECTORY, ALREADY EXISTS");
        }

    }

    // Lưu file người dùng upload lên thư mục chỉ định, tạo tên mới để tránh trùng.
    public String store(MultipartFile file, String folder) throws URISyntaxException, IOException {
        // Sanitize file name: loại bỏ path components nguy hiểm
        String originalName = file.getOriginalFilename();
        if (originalName != null) {
            // Chỉ lấy phần tên file, loại bỏ mọi đường dẫn (chống path traversal)
            originalName = Paths.get(originalName).getFileName().toString();
            // Loại bỏ các ký tự đặc biệt nguy hiểm
            originalName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        } else {
            originalName = "unnamed_file";
        }
        String finalName = System.currentTimeMillis() + "-" + originalName;

        URI uri = new URI(baseURI + folder + "/" + finalName);
        Path path = Paths.get(uri);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, path,
                    StandardCopyOption.REPLACE_EXISTING);
        }
        return finalName;
    }

    // Trả về dung lượng file (byte).
    public long getFileLength(String fileName, String folder) throws URISyntaxException {
        URI uri = new URI(baseURI + folder + "/" + fileName);
        Path path = Paths.get(uri);

        File tmpDir = new File(path.toString());

        // file không tồn tại, hoặc file là 1 director => return 0
        if (!tmpDir.exists() || tmpDir.isDirectory())
            return 0;
        return tmpDir.length();
    }

    // Tạo một InputStreamResource từ file vật lý để gửi về client qua Controller
    public InputStreamResource getResource(String fileName, String folder)
            throws URISyntaxException, FileNotFoundException {
        URI uri = new URI(baseURI + folder + "/" + fileName);
        Path path = Paths.get(uri);

        File file = new File(path.toString());
        return new InputStreamResource(new FileInputStream(file));
    }

}