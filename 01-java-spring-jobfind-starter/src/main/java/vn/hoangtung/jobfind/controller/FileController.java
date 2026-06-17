package vn.hoangtung.jobfind.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import vn.hoangtung.jobfind.domain.response.file.ResUploadFileDTO;
import vn.hoangtung.jobfind.service.FileService;
import vn.hoangtung.jobfind.util.annotation.ApiMessage;
import vn.hoangtung.jobfind.util.error.StorageException;

@RestController
@RequestMapping("/api/v1")
public class FileController {

    private static final long MAX_UPLOAD_SIZE_BYTES = 10 * 1024 * 1024;
    private static final List<String> ALLOWED_EXTENSIONS = List.of(".pdf", ".jpg", ".jpeg", ".png");

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/files")
    @ApiMessage("Upload single file")
    public ResponseEntity<ResUploadFileDTO> upload(
            @RequestParam(name = "file", required = false) MultipartFile file,
            @RequestParam("folder") String folder)
            throws URISyntaxException, IOException, StorageException {
        // skip validate
        if (file == null || file.isEmpty()) {
            throw new StorageException("File is empty. Please upload a file.");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new StorageException("Invalid file name.");
        }
        if (file.getSize() > MAX_UPLOAD_SIZE_BYTES) {
            throw new StorageException("File is too large. Maximum size is 10MB.");
        }

        String normalizedFileName = fileName.toLowerCase(Locale.ROOT);
        boolean isValid = ALLOWED_EXTENSIONS.stream().anyMatch(normalizedFileName::endsWith);
        if (!isValid) {
            throw new StorageException("Invalid file extension. Only allows " + ALLOWED_EXTENSIONS);
        }
        validateFileSignature(file, normalizedFileName);

        this.fileService.createDirectory(folder);
        String uploadFile = this.fileService.store(file, folder);

        ResUploadFileDTO res = new ResUploadFileDTO(uploadFile, Instant.now());

        return ResponseEntity.ok().body(res);
    }

    @GetMapping("/files")
    @ApiMessage("Download a file")
    public ResponseEntity<Resource> download(
            @RequestParam(name = "fileName", required = false) String fileName,
            @RequestParam(name = "folder", required = false) String folder)
            throws StorageException, URISyntaxException, FileNotFoundException {
        if (fileName == null || folder == null) {
            throw new StorageException("Missing required params : (fileName or folder) in query params.");
        }

        // check file exist (and not a directory)
        long fileLength = this.fileService.getFileLength(fileName, folder);
        if (fileLength == 0) {
            throw new StorageException("File with name = " + fileName + " not found.");
        }

        // download a file
        InputStreamResource resource = this.fileService.getResource(fileName, folder);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentLength(fileLength)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    private void validateFileSignature(MultipartFile file, String normalizedFileName)
            throws IOException, StorageException {
        byte[] header = new byte[8];
        int bytesRead;
        try (InputStream inputStream = file.getInputStream()) {
            bytesRead = inputStream.read(header);
        }

        if (normalizedFileName.endsWith(".pdf") && !hasPdfSignature(header, bytesRead)) {
            throw new StorageException("Invalid PDF file signature.");
        }
        if (normalizedFileName.endsWith(".png") && !hasPngSignature(header, bytesRead)) {
            throw new StorageException("Invalid PNG file signature.");
        }
        if ((normalizedFileName.endsWith(".jpg") || normalizedFileName.endsWith(".jpeg"))
                && !hasJpegSignature(header, bytesRead)) {
            throw new StorageException("Invalid JPEG file signature.");
        }
    }

    private boolean hasPdfSignature(byte[] header, int bytesRead) {
        return bytesRead >= 4 && header[0] == '%' && header[1] == 'P' && header[2] == 'D' && header[3] == 'F';
    }

    private boolean hasPngSignature(byte[] header, int bytesRead) {
        return bytesRead >= 8
                && header[0] == (byte) 0x89
                && header[1] == 'P'
                && header[2] == 'N'
                && header[3] == 'G'
                && header[4] == 0x0D
                && header[5] == 0x0A
                && header[6] == 0x1A
                && header[7] == 0x0A;
    }

    private boolean hasJpegSignature(byte[] header, int bytesRead) {
        return bytesRead >= 3
                && header[0] == (byte) 0xFF
                && header[1] == (byte) 0xD8
                && header[2] == (byte) 0xFF;
    }

}
