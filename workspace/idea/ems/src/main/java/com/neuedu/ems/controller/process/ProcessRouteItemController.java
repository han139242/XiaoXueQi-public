package com.neuedu.ems.controller.process;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.process.ProcessRouteItem;
import com.neuedu.ems.service.process.ProcessRouteItemService;
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
@RequestMapping("/api/process/route-item")
public class ProcessRouteItemController {

    @Autowired
    private ProcessRouteItemService processRouteItemService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询工艺路线明细列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) Long routeId,
                           @RequestParam(required = false) Long operationId) {
        Page<ProcessRouteItem> result = processRouteItemService.pageRouteItems(current, size, routeId, operationId);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询工艺路线明细。
     */
    @GetMapping("/{routeItemId}")
    public ResultData getRouteItem(@PathVariable("routeItemId") Long id) {
        ProcessRouteItem item = processRouteItemService.getRouteItemById(id);
        return ResultData.success(ResultCode.SUCCESS, item);
    }

    /**
     * 新增工艺路线明细。
     */
    @PostMapping
    public ResultData save(@RequestBody ProcessRouteItem item) {
        boolean flag = processRouteItemService.saveRouteItem(item);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新工艺路线明细。
     */
    @PutMapping
    public ResultData update(@RequestBody ProcessRouteItem item) {
        boolean flag = processRouteItemService.updateRouteItem(item);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除工艺路线明细，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = processRouteItemService.removeRouteItemById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出工艺路线明细 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) Long routeId,
                      @RequestParam(required = false) Long operationId) {
        List<ProcessRouteItem> items = processRouteItemService.listRouteItems(routeId, operationId);
        excelExportUtil.exportExcel(response, items, ProcessRouteItem.class, "工艺路线明细列表", "工艺路线明细列表");
    }
}
