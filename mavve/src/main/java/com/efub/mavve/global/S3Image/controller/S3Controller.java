package com.efub.mavve.global.S3Image.controller;

import com.efub.mavve.global.S3Image.dto.response.ImageUrlResponse;
import com.efub.mavve.global.S3Image.service.S3ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/image")
public class S3Controller {

    private final S3ImageService s3ImageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUrlResponse> uploadImage(@RequestParam("file") MultipartFile file,
                                              @RequestParam("dirName") String dirName) throws IOException {
        String imageUrl = s3ImageService.uploadFile(file, dirName);
        return ResponseEntity.ok(new ImageUrlResponse(imageUrl));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteImage(@RequestParam("fileUrl") String fileUrl) {
        s3ImageService.deleteFile(fileUrl);
        return ResponseEntity.noContent().build();
    }
}
