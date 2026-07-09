package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.SysDictData;
import com.neuedu.ems.service.SysDictDataService;
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

import java.util.List;

@RestController
@RequestMapping("/api/sys/dict-data")
public class SysDictDataController {

    @Autowired
    private SysDictDataService sysDictDataService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询字典数据列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String dictTypeCode) {
        Page<SysDictData> result = sysDictDataService.pageDictData(current, size, keyword, dictTypeCode);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据字典类型查询字典数据。
     */
    @GetMapping("/type/{dictTypeCode}")
    public ResultData listByType(@PathVariable("dictTypeCode") String dictTypeCode) {
        List<SysDictData> result = sysDictDataService.listDictDataByTypeCode(dictTypeCode);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询字典数据。
     */
    @GetMapping("/{dictDataId}")
    public ResultData getDictData(@PathVariable("dictDataId") Long id) {
        SysDictData dictData = sysDictDataService.getDictDataById(id);
        return ResultData.success(ResultCode.SUCCESS, dictData);
    }

    /**
     * 新增字典数据。
     */
    @PostMapping
    public ResultData save(@RequestBody SysDictData dictData) {
        boolean flag = sysDictDataService.saveDictData(dictData);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新字典数据。
     */
    @PutMapping
    public ResultData update(@RequestBody SysDictData dictData) {
        boolean flag = sysDictDataService.updateDictData(dictData);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除字典数据，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = sysDictDataService.removeDictDataById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出字典数据 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String dictTypeCode) {
        List<SysDictData> dictData = sysDictDataService.listDictData(keyword, dictTypeCode);
        excelExportUtil.exportExcel(response, dictData, SysDictData.class, "字典数据列表", "字典数据列表");
    }
}
