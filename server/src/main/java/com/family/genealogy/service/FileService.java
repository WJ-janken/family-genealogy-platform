package com.family.genealogy.service;

import com.family.genealogy.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传服务
 * 注：实际使用时需配置腾讯云 COS SDK，此处为简化实现
 */
@Slf4j
@Service
public class FileService {

    @Value("${cos.base-url}")
    private String baseUrl;

    /**
     * 上传文件
     */
    public Map<String, String> upload(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        String originalName = file.getOriginalFilename();
        String ext = originalName != null ? originalName.substring(originalName.lastIndexOf(".")) : "";
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        String filePath = "uploads/" + fileName;

        // TODO: 实际上传到腾讯云 COS
        // 此处模拟返回 URL
        String fileUrl = baseUrl + "/" + filePath;

        log.info("文件上传成功: {} -> {}", originalName, fileUrl);

        Map<String, String> result = new HashMap<>();
        result.put("url", fileUrl);
        result.put("fileName", originalName);
        result.put("fileSize", String.valueOf(file.getSize()));
        return result;
    }
}
