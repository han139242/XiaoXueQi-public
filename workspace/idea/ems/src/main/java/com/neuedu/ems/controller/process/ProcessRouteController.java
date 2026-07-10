package com.neuedu.ems.controller.process;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.process.ProcessRoute;
import com.neuedu.ems.service.process.ProcessRouteService;
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
@RequestMapping("/api/process/route")
public class ProcessRouteController {

    @Autowired
    private ProcessRouteService processRouteService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询工艺路线列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Long productId,
                           @RequestParam(required = false) String status) {
        Page<ProcessRoute> result = processRouteService.pageRoutes(current, size, keyword, productId, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询工艺路线。
     */
    @GetMapping("/{routeId}")
    public ResultData getRoute(@PathVariable("routeId") Long id) {
        ProcessRoute route = processRouteService.getRouteById(id);
        return ResultData.success(ResultCode.SUCCESS, route);
    }

    /**
     * 新增工艺路线。
     */
    @PostMapping
    public ResultData save(@RequestBody ProcessRoute route) {
        boolean flag = processRouteService.saveRoute(route);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新工艺路线。
     */
    @PutMapping
    public ResultData update(@RequestBody ProcessRoute route) {
        boolean flag = processRouteService.updateRoute(route);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除工艺路线，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = processRouteService.removeRouteById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出工艺路线 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) Long productId,
                      @RequestParam(required = false) String status) {
        List<ProcessRoute> routes = processRouteService.listRoutes(keyword, productId, status);
        excelExportUtil.exportExcel(response, routes, ProcessRoute.class, "工艺路线列表", "工艺路线列表");
    }
}
