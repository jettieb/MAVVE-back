package com.efub.mavve.global.S3Image.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.efub.mavve.global.exception.ExceptionCode;
import com.efub.mavve.global.exception.MavveException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class S3ImageService {
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    private final AmazonS3 amazonS3;
    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "webp");

    //이미지 파일 한 개 저장
    public String uploadFile(MultipartFile file, String dirName){
        validateFileExtension(file.getOriginalFilename());
        String fileName = buildFileName(file, dirName);

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(file.getSize());
        objectMetadata.setContentType(file.getContentType());

        try (InputStream inputStream = file.getInputStream()) {
            amazonS3.putObject(new PutObjectRequest(bucket, fileName, inputStream, objectMetadata));
        } catch (IOException e) {
            throw new MavveException(ExceptionCode.IMAGE_UPLOAD_FAILED);
        }

        //s3 url 전체 경로 반환
        return amazonS3.getUrl(bucket, fileName).toString();
    }

    //이미지 파일 여러 개 저장
    public List<String> uploadFiles(List<MultipartFile> files, String dirName) {
        if (files == null) {
            throw new MavveException(ExceptionCode.NO_FILE_PROVIDED);
        }
        return files.stream()
                .map(file -> uploadFile(file, dirName))
                .collect(Collectors.toList());
    }

    // 이미지 삭제
    public void deleteFile(String fileUrl) {
        String splitStr = ".com/";  //전체 url에서 .com/을 기준으로 파일명 추출함
        String fileName = fileUrl.substring(fileUrl.lastIndexOf(splitStr) + splitStr.length());

        amazonS3.deleteObject(new DeleteObjectRequest(bucket, fileName));
    }

    //확장자 검사
    private void validateFileExtension(String fileName){
        if(fileName == null || !fileName.contains(".")){
            throw new MavveException(ExceptionCode.NO_FILE_PROVIDED);
        }

        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new MavveException(ExceptionCode.INVALID_IMAGE_EXTENSION);
        }
    }

    //유니크 파일명 생성
    private String buildFileName(MultipartFile file, String dirName) {
        return dirName + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
    }
}