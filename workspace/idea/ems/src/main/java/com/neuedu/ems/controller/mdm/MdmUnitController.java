package com.neuedu.ems.controller.mdm;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.mdm.MdmUnit;
import com.neuedu.ems.service.mdm.MdmUnitService;
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
@RequestMapping("/api/mdm/unit")
public class MdmUnitController {

    @Autowired
    private MdmUnitService mdmUnitService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询计量单位列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String status) {
        Page<MdmUnit> result = mdmUnitService.pageUnits(current, size, keyword, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询计量单位。
     */
    @GetMapping("/{unitId}")
    public ResultData getUnit(@PathVariable("unitId") Long id) {
        MdmUnit unit = mdmUnitService.getUnitById(id);
        return ResultData.success(ResultCode.SUCCESS, unit);
    }

    /**
     * 新增计量单位。
     */
    @PostMapping
    public ResultData save(@RequestBody MdmUnit unit) {
        boolean flag = mdmUnitService.saveUnit(unit);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新计量单位。
     */
    @PutMapping
    public ResultData update(@RequestBody MdmUnit unit) {
        boolean flag = mdmUnitService.updateUnit(unit);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除计量单位，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = mdmUnitService.removeUnitById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出计量单位 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String status) {
        List<MdmUnit> units = mdmUnitService.listUnits(keyword, status);
        excelExportUtil.exportExcel(response, units, MdmUnit.class, "计量单位列表", "计量单位列表");
    }
}
