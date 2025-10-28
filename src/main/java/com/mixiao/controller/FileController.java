package com.mixiao.controller;

import com.mixiao.resp.CommonResp;
import com.mixiao.util.SnowFlake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/file")
public class FileController {

    private static final Logger LOG = LoggerFactory.getLogger(FileController.class);

    @Value("${file.upload.path}")
    private String fileUploadPath;

    @Resource
    private SnowFlake snowFlake;

    @PostMapping("/upload")
    public CommonResp<List<String>> upload(@RequestParam("file") MultipartFile[] files) {
        CommonResp<List<String>> resp = new CommonResp<>();
        if (files == null || files.length == 0) {
            resp.setSuccess(false);
            resp.setMessage("未选择上传文件");
            return resp;
        }

        List<String> urls = new ArrayList<>();
        try {
            Path uploadRoot = Paths.get(fileUploadPath);
            Files.createDirectories(uploadRoot);
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                String originalFilename = file.getOriginalFilename();
                String ext = "";
                if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
                    ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
                }
                String fileName = snowFlake.nextId() + ext;
                Path destination = uploadRoot.resolve(fileName).normalize();
                try (InputStream inputStream = file.getInputStream()) {
                    Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
                }
                urls.add("/upload/" + fileName);
                LOG.info("图片上传成功，文件名：{}", fileName);
            }
            if (urls.isEmpty()) {
                resp.setSuccess(false);
                resp.setMessage("未检测到有效的图片文件");
            } else {
                resp.setContent(urls);
            }
        } catch (IOException e) {
            LOG.error("图片上传失败", e);
            resp.setSuccess(false);
            resp.setMessage("图片上传失败，请稍后再试");
        }
        return resp;
    }
}