package com.neuedu.ems.controller.sys;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.sys.SysDictType;
import com.neuedu.ems.service.sys.SysDictTypeService;
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
@RequestMapping("/api/sys/dict-type")
public class SysDictTypeController {

    @Autowired
    private SysDictTypeService sysDictTypeService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询字典类型列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String status) {
        Page<SysDictType> result = sysDictTypeService.pageDictTypes(current, size, keyword, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询字典类型。
     */
    @GetMapping("/{dictTypeId}")
    public ResultData getDictType(@PathVariable("dictTypeId") Long id) {
        SysDictType dictType = sysDictTypeService.getDictTypeById(id);
        return ResultData.success(ResultCode.SUCCESS, dictType);
    }

    /**
     * 新增字典类型。
     */
    @PostMapping
    public ResultData save(@RequestBody SysDictType dictType) {
        boolean flag = sysDictTypeService.saveDictType(dictType);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新字典类型。
     */
    @PutMapping
    public ResultData update(@RequestBody SysDictType dictType) {
        boolean flag = sysDictTypeService.updateDictType(dictType);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除字典类型，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = sysDictTypeService.removeDictTypeById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出字典类型 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String status) {
        List<SysDictType> dictTypes = sysDictTypeService.listDictTypes(keyword, status);
        excelExportUtil.exportExcel(response, dictTypes, SysDictType.class, "字典类型列表", "字典类型列表");
    }
}
