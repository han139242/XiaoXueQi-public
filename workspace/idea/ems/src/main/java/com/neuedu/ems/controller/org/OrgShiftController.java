package com.neuedu.ems.controller.org;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.org.OrgShift;
import com.neuedu.ems.service.org.OrgShiftService;
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
@RequestMapping("/api/org/shift")
public class OrgShiftController {

    @Autowired
    private OrgShiftService orgShiftService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询班次列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword) {
        Page<OrgShift> result = orgShiftService.pageShifts(current, size, keyword);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询班次。
     */
    @GetMapping("/{shiftId}")
    public ResultData getShift(@PathVariable("shiftId") Long id) {
        OrgShift shift = orgShiftService.getShiftById(id);
        return ResultData.success(ResultCode.SUCCESS, shift);
    }

    /**
     * 新增班次。
     */
    @PostMapping
    public ResultData save(@RequestBody OrgShift shift) {
        boolean flag = orgShiftService.saveShift(shift);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新班次。
     */
    @PutMapping
    public ResultData update(@RequestBody OrgShift shift) {
        boolean flag = orgShiftService.updateShift(shift);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除班次，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = orgShiftService.removeShiftById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出班次 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword) {
        List<OrgShift> shifts = orgShiftService.listShifts(keyword);
        excelExportUtil.exportExcel(response, shifts, OrgShift.class, "班次列表", "班次列表");
    }
}
