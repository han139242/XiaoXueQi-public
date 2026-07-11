package com.neuedu.ems.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * 企业级文件工具类（专科版）
 * 核心：生成双层随机路径、唯一文件名、临时转正式、文件校验
 */
@Component
public class FileUtil {

    // 根路径
    @Value("${file.upload.root-path}")
    private String rootPath;

    @Value("${file.upload.temp-root-path}")
    private String tempRootPath;

    // 允许的文件类型
    @Value("${file.upload.allow-types}")
    private String allowTypes;

    // 双层随机路径长度
    @Value("${file.upload.first-level-length}")
    private int firstLevelLength;
    @Value("${file.upload.second-level-length}")
    private int secondLevelLength;



    /**
     * 1. 生成双层随机路径（核心）
     * @return 完整路径（如：）
     */
    public String generateTwoLevelRandomPath() {
        // 第一层随机字符串（2位）
        String firstLevel = getRandomString(firstLevelLength);
        // 第二层随机字符串（3位）
        String secondLevel = getRandomString(secondLevelLength);
        // 拼接完整路径
        String twoPath = firstLevel + "/" + secondLevel + "/";


        return twoPath;
    }

    public String tempUpload(MultipartFile file) {
        try {
            // 1. 基础校验
            if (file.isEmpty()) {
                throw new RuntimeException("文件不能为空！");
            }
            // 2. 校验文件类型
            checkAvatarFileType(file);
            // 3. 生成临时路径（temp前缀+双层随机）  Sc/W7V/
            String twoLevalPath = generateTwoLevelRandomPath();
            // 4. 生成临时唯一文件名（无用户ID）  // asdfjkasdfjalkdfj.pdf
            String tempFileName = generateUniqueFileName(file.getOriginalFilename());
            // D://upload/temp/Sc/W7V/asdfjkasdfjalkdfj.pdf
            String tempFullPath = tempRootPath + twoLevalPath + tempFileName;
            //D://upload/temp/Sc/W7V/
            File pathFile = new File(tempRootPath + twoLevalPath);
            if (!pathFile.exists()) {
            // mkdirs() 会创建所有不存在的父目录，返回boolean确认是否创建成功
            boolean isCreated = pathFile.mkdirs();
            if (!isCreated) {
                throw new RuntimeException("文件夹创建失败：" + tempRootPath + twoLevalPath);
            }
        }
            // 5. 保存到临时路径
            // D://upload/temp/Sc/W7V/asdfjkasdfjalkdfj.pdf
            file.transferTo(new File(tempFullPath));
            // 6. 返回临时路径（仅返回路径，结果封装在Controller）
            // Sc/W7V/asdfjkasdfjalkdfj.pdf
            return twoLevalPath + tempFileName;
        } catch (Exception e) {
            throw new RuntimeException("临时上传失败：" + e.getMessage(), e);
        }
    }

    public String tempToFormalAvatar(String tempPath) {
        try {
            if (tempPath == null || tempPath.isEmpty()) {
                throw new RuntimeException("临时文件路径不能为空！");
            }

            // 2. 临时转正式
            String formalFullPath = moveTempToFormal(tempPath);
            // 3. 仅返回正式路径（用户关联逻辑移到Controller）
            return formalFullPath;
        } catch (Exception e) {
            throw new RuntimeException("临时转正式失败：" + e.getMessage(), e);
        }
    }

    /**
     * 2. 生成唯一文件名（防重复）
     * @param originalFileName 原文件名
     * @return 唯一文件名（如：6_1234567890abcdef.jpg）
     */
    public String generateUniqueFileName(String originalFileName) {
        // 获取文件后缀
        String suffix = "";
        if (originalFileName != null && originalFileName.lastIndexOf(".") > 0) {
            suffix = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        // 生成UUID（去掉横线）
        String uuid = UUID.randomUUID().toString().replace("-", "");
        // 有用户ID则拼接（方便识别），无则仅UUID
        return  uuid + suffix;
    }

    /**
     * 3. 校验文件类型（头像仅允许图片）图片和PDF
     */
    public void checkAvatarFileType(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.lastIndexOf(".") <= 0) {
            throw new RuntimeException("文件必须包含有效扩展名！");
        }
        String suffix = "";
        if (originalFileName.lastIndexOf(".") > 0) {
            // 转小写，避免JPG/jpg区分
            suffix = originalFileName.substring(originalFileName.lastIndexOf(".") + 1).toLowerCase();
        }
        // 校验是否在允许列表
        boolean allowed = java.util.Arrays.stream(allowTypes.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .anyMatch(suffix::equals);
        if (!allowed) {
            throw new RuntimeException("仅允许上传：" + allowTypes + "类型的文件！");
        }
    }

    public String uploadAvatar(MultipartFile file) {
        try {
            // 1. 基础校验（用户存在性校验移到Controller/用户服务）

            if (file.isEmpty()) {
                throw new RuntimeException("文件不能为空！");
            }
            // 2. 校验文件类型
            checkAvatarFileType(file);
            // 3. 生成正式路径（avatar前缀+双层随机）
            String twoPath = generateTwoLevelRandomPath();
            // 4. 生成正式文件名
            String formalFileName = generateUniqueFileName(file.getOriginalFilename());
            String formalFullPath = twoPath + formalFileName;
            File pathFile = new File(rootPath + twoPath);
            if (!pathFile.exists()) {
                // mkdirs() 会创建所有不存在的父目录，返回boolean确认是否创建成功
                boolean isCreated = pathFile.mkdirs();
                if (!isCreated) {
                    throw new RuntimeException("文件夹创建失败：" + rootPath + twoPath);
                }
            }
            // 5. 直接保存到正式路径
            file.transferTo(new File(rootPath + formalFullPath));
            // 6. 返回正式路径
            return formalFullPath;
        } catch (Exception e) {
            throw new RuntimeException("头像上传失败：" + e.getMessage(), e);
        }
    }

    public String uploadFile(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("文件不能为空！");
            }
            checkAvatarFileType(file);
            String twoPath = generateTwoLevelRandomPath();
            String formalFileName = generateUniqueFileName(file.getOriginalFilename());
            String formalFullPath = twoPath + formalFileName;
            File pathFile = new File(rootPath + twoPath);
            if (!pathFile.exists()) {
                boolean isCreated = pathFile.mkdirs();
                if (!isCreated) {
                    throw new RuntimeException("文件夹创建失败：" + rootPath + twoPath);
                }
            }
            file.transferTo(new File(rootPath + formalFullPath));
            return formalFullPath;
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败：" + e.getMessage(), e);
        }
    }

    /**
     * 4. 临时文件转正式路径（核心：剪切粘贴）
     * @param tempPath 临时文件完整路径
     * @return 正式路径
     */
    public String moveTempToFormal(String tempPath) {
        try {
            Path tempRoot = Path.of(tempRootPath).toAbsolutePath().normalize();
            Path tempFilePath = resolveWithinRoot(tempRoot, tempPath);
            // 校验临时文件是否存在
            if (!Files.isRegularFile(tempFilePath)) {
                throw new RuntimeException("临时文件不存在：" + tempPath);
            }
            // 生成正式路径（avatar前缀+双层随机）- 该方法已包含文件夹创建逻辑
            String formalPath = generateTwoLevelRandomPath();
            // 生成正式文件名（带用户ID）
            String formalFileName = generateUniqueFileName(tempFilePath.getFileName().toString());
            String formalFullPath = formalPath + formalFileName;
            Path formalRoot = Path.of(rootPath).toAbsolutePath().normalize();
            Path formalFilePath = resolveWithinRoot(formalRoot, formalFullPath);
            Files.createDirectories(formalFilePath.getParent());
            Files.move(tempFilePath, formalFilePath);
            return formalFullPath;
        } catch (Exception e) {
            throw new RuntimeException("临时转正式失败：" + e.getMessage(), e);
        }
    }

    private Path resolveWithinRoot(Path root, String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new RuntimeException("文件路径不能为空");
        }
        Path resolvedPath = root.resolve(relativePath.replace('\\', '/')).normalize();
        if (!resolvedPath.startsWith(root)) {
            throw new RuntimeException("文件路径超出允许的存储目录");
        }
        return resolvedPath;
    }

    /**
     * 私有工具：生成随机字符串
     */
    private String getRandomString(int length) {
        String chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";  //62
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());   // 0--61  30
            sb.append(chars.charAt(index));   //sC
        }
        return sb.toString();
    }

    /**
     * 5. 删除文件（兜底用）
     */
    public void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
    }

    // 新增：通用文件夹创建方法（供外部/其他方法调用）
    public void createDirIfNotExists(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            boolean isCreated = dir.mkdirs();
            if (!isCreated) {
                throw new RuntimeException("文件夹创建失败：" + dirPath);
            }
        }
    }

}
