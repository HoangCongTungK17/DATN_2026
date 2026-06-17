package vn.hoangtung.jobfind.service;

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
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {

    private static final Set<String> ALLOWED_FOLDERS = Set.of("resume", "company", "avatar", "image", "images",
            "logo");

    @Value("${hoangtung.upload-file.base-uri}")
    private String baseURI;

    public void createDirectory(String folder) throws URISyntaxException {
        try {
            Files.createDirectories(resolveFolder(folder));
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot create upload folder", e);
        }
    }

    public String store(MultipartFile file, String folder) throws URISyntaxException, IOException {
        String originalName = sanitizeFileName(file.getOriginalFilename());
        String finalName = System.currentTimeMillis() + "-" + originalName;
        Path path = resolveFile(folder, finalName);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
        }
        return finalName;
    }

    public long getFileLength(String fileName, String folder) throws URISyntaxException {
        Path path = resolveFile(folder, fileName);
        if (!Files.exists(path) || Files.isDirectory(path)) {
            return 0;
        }
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0;
        }
    }

    public InputStreamResource getResource(String fileName, String folder)
            throws URISyntaxException, FileNotFoundException {
        Path path = resolveFile(folder, fileName);
        return new InputStreamResource(new FileInputStream(path.toFile()));
    }

    private Path resolveFile(String folder, String fileName) throws URISyntaxException {
        String safeFileName = sanitizeFileName(fileName);
        Path folderPath = resolveFolder(folder);
        Path filePath = folderPath.resolve(safeFileName).normalize();
        if (!filePath.startsWith(folderPath)) {
            throw new IllegalArgumentException("Invalid file path");
        }
        return filePath;
    }

    private Path resolveFolder(String folder) throws URISyntaxException {
        String safeFolder = validateFolder(folder);
        Path basePath = Paths.get(new URI(baseURI)).toAbsolutePath().normalize();
        Path folderPath = basePath.resolve(safeFolder).normalize();
        if (!folderPath.startsWith(basePath)) {
            throw new IllegalArgumentException("Invalid upload folder");
        }
        return folderPath;
    }

    private String validateFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            throw new IllegalArgumentException("Upload folder is required");
        }
        String normalized = folder.trim().replace("\\", "/");
        if (normalized.contains("/") || normalized.contains("..") || !ALLOWED_FOLDERS.contains(normalized)) {
            throw new IllegalArgumentException("Upload folder is not allowed");
        }
        return normalized;
    }

    private String sanitizeFileName(String fileName) {
        String safeName = fileName == null || fileName.isBlank() ? "unnamed_file" : fileName;
        safeName = Paths.get(safeName).getFileName().toString();
        safeName = safeName.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (safeName.isBlank() || ".".equals(safeName) || "..".equals(safeName)) {
            throw new IllegalArgumentException("Invalid file name");
        }
        return safeName;
    }
}
