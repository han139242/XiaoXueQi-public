package com.neuedu.ems.controller.sys;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.sys.SysFile;
import com.neuedu.ems.service.sys.SysFileService;
import com.neuedu.ems.util.ExcelExportUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/sys/file")
public class SysFileController {

    @Autowired
    private SysFileService sysFileService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询附件文件列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String bizType,
                           @RequestParam(required = false) Long bizId,
                           @RequestParam(required = false) String fileType) {
        Page<SysFile> result = sysFileService.pageFiles(current, size, keyword, bizType, bizId, fileType);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 上传附件文件。
     */
    @PostMapping("/upload")
    public ResultData upload(@RequestParam("file") MultipartFile file,
                             @RequestParam(required = false) String bizType,
                             @RequestParam(required = false) Long bizId) {
        SysFile result = sysFileService.uploadFile(file, bizType, bizId);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 下载附件文件。
     */
    @GetMapping("/download/{fileId}")
    public void download(@PathVariable("fileId") Long fileId, HttpServletResponse response) throws Exception {
        sysFileService.downloadFile(fileId, response);
    }

    /**
     * 根据 ID 查询附件文件。
     */
    @GetMapping("/{fileId}")
    public ResultData getFile(@PathVariable("fileId") Long id) {
        SysFile file = sysFileService.getFileById(id);
        return ResultData.success(ResultCode.SUCCESS, file);
    }

    /**
     * 新增附件文件记录。
     */
    @PostMapping
    public ResultData save(@RequestBody SysFile file) {
        boolean flag = sysFileService.saveFile(file);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新附件文件记录。
     */
    @PutMapping
    public ResultData update(@RequestBody SysFile file) {
        boolean flag = sysFileService.updateFile(file);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除附件文件记录，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = sysFileService.removeFileById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出附件文件 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String bizType,
                      @RequestParam(required = false) Long bizId,
                      @RequestParam(required = false) String fileType) {
        List<SysFile> files = sysFileService.listFiles(keyword, bizType, bizId, fileType);
        excelExportUtil.exportExcel(response, files, SysFile.class, "附件文件列表", "附件文件列表");
    }
}
