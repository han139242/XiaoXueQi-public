package com.neuedu.ems.controller.factory;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.factory.FactoryLine;
import com.neuedu.ems.service.factory.FactoryLineService;
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
@RequestMapping("/api/factory/line")
public class FactoryLineController {

    @Autowired
    private FactoryLineService factoryLineService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询产线列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Long workshopId,
                           @RequestParam(required = false) String status) {
        Page<FactoryLine> result = factoryLineService.pageLines(current, size, keyword, workshopId, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询产线。
     */
    @GetMapping("/{lineId}")
    public ResultData getLine(@PathVariable("lineId") Long id) {
        FactoryLine line = factoryLineService.getLineById(id);
        return ResultData.success(ResultCode.SUCCESS, line);
    }

    /**
     * 新增产线。
     */
    @PostMapping
    public ResultData save(@RequestBody FactoryLine line) {
        boolean flag = factoryLineService.saveLine(line);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新产线。
     */
    @PutMapping
    public ResultData update(@RequestBody FactoryLine line) {
        boolean flag = factoryLineService.updateLine(line);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除产线，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = factoryLineService.removeLineById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出产线 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) Long workshopId,
                      @RequestParam(required = false) String status) {
        List<FactoryLine> lines = factoryLineService.listLines(keyword, workshopId, status);
        excelExportUtil.exportExcel(response, lines, FactoryLine.class, "产线列表", "产线列表");
    }
}
