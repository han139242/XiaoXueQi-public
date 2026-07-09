package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.SysFile;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 附件文件 Service 接口。
 */
public interface SysFileService extends IService<SysFile> {

    Page<SysFile> pageFiles(Integer current, Integer size, String keyword, String bizType, Long bizId, String fileType);

    SysFile getFileById(Long fileId);

    SysFile uploadFile(MultipartFile file, String bizType, Long bizId);

    void downloadFile(Long fileId, HttpServletResponse response) throws Exception;

    boolean saveFile(SysFile file);

    boolean updateFile(SysFile file);

    boolean removeFileById(Long fileId);

    List<SysFile> listFiles(String keyword, String bizType, Long bizId, String fileType);
}
