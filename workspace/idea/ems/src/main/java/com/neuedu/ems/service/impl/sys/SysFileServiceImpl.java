package com.neuedu.ems.service.impl.sys;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.sys.SysFile;
import com.neuedu.ems.mapper.sys.SysFileMapper;
import com.neuedu.ems.service.sys.SysFileService;
import com.neuedu.ems.util.FileDownloadUtil;
import com.neuedu.ems.util.FileUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 附件文件 Service 实现类。
 */
@Service
public class SysFileServiceImpl extends ServiceImpl<SysFileMapper, SysFile> implements SysFileService {

    private static final String FILE_URL_PREFIX = "/upload/";

    @Autowired
    private FileUtil fileUtil;

    @Value("${file.upload.root-path}")
    private String rootPath;

    @Override
    public Page<SysFile> pageFiles(Integer current, Integer size, String keyword, String bizType, Long bizId, String fileType) {
        Page<SysFile> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildFileQuery(keyword, bizType, bizId, fileType));
    }

    @Override
    public SysFile getFileById(Long fileId) {
        QueryWrapper<SysFile> wrapper = new QueryWrapper<>();
        wrapper.eq("file_id", fileId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public SysFile uploadFile(MultipartFile file, String bizType, Long bizId) {
        String formalPath = fileUtil.uploadFile(file);
        SysFile sysFile = new SysFile();
        sysFile.setFileName(file.getOriginalFilename());
        sysFile.setFileUrl(FILE_URL_PREFIX + formalPath);
        sysFile.setFileType(getFileType(file.getOriginalFilename()));
        sysFile.setBizType(bizType);
        sysFile.setBizId(bizId);
        sysFile.setDeletedFlag(0);
        sysFile.setCreatedTime(LocalDateTime.now());
        baseMapper.insert(sysFile);
        return sysFile;
    }

    @Override
    public void downloadFile(Long fileId, HttpServletResponse response) throws Exception {
        SysFile sysFile = getFileById(fileId);
        if (sysFile == null) {
            throw new RuntimeException("文件不存在：" + fileId);
        }
        FileDownloadUtil.download(response, resolveStoragePath(sysFile.getFileUrl()), sysFile.getFileName());
    }

    @Override
    public boolean saveFile(SysFile file) {
        if (file.getDeletedFlag() == null) {
            file.setDeletedFlag(0);
        }
        return baseMapper.insert(file) > 0;
    }

    @Override
    public boolean updateFile(SysFile file) {
        if (file.getFileId() == null) {
            return false;
        }
        file.setDeletedFlag(null);
        return baseMapper.updateById(file) > 0;
    }

    @Override
    public boolean removeFileById(Long fileId) {
        SysFile file = new SysFile();
        file.setFileId(fileId);
        file.setDeletedFlag(1);
        return baseMapper.updateById(file) > 0;
    }

    @Override
    public List<SysFile> listFiles(String keyword, String bizType, Long bizId, String fileType) {
        return baseMapper.selectList(buildFileQuery(keyword, bizType, bizId, fileType));
    }

    private QueryWrapper<SysFile> buildFileQuery(String keyword, String bizType, Long bizId, String fileType) {
        QueryWrapper<SysFile> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.like("file_name", keyword);
        }
        if (StringUtils.hasText(bizType)) {
            wrapper.eq("biz_type", bizType);
        }
        if (bizId != null) {
            wrapper.eq("biz_id", bizId);
        }
        if (StringUtils.hasText(fileType)) {
            wrapper.eq("file_type", fileType);
        }
        wrapper.orderByDesc("file_id");
        return wrapper;
    }

    private String getFileType(String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        return extension == null ? null : extension.toLowerCase();
    }

    private String resolveStoragePath(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            throw new RuntimeException("文件地址不能为空");
        }
        File directFile = new File(fileUrl);
        if (directFile.isAbsolute()) {
            return directFile.getPath();
        }
        String relativePath = fileUrl;
        if (relativePath.startsWith(FILE_URL_PREFIX)) {
            relativePath = relativePath.substring(FILE_URL_PREFIX.length());
        } else if (relativePath.startsWith("upload/")) {
            relativePath = relativePath.substring("upload/".length());
        }
        if (relativePath.startsWith("file/")) {
            relativePath = relativePath.substring("file/".length());
        }
        return rootPath + relativePath;
    }
}
