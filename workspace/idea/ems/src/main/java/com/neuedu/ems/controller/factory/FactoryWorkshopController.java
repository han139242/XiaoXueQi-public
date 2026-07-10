package com.neuedu.ems.controller.factory;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.factory.FactoryWorkshop;
import com.neuedu.ems.service.factory.FactoryWorkshopService;
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
@RequestMapping("/api/factory/workshop")
public class FactoryWorkshopController {

    @Autowired
    private FactoryWorkshopService factoryWorkshopService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询车间列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String status) {
        Page<FactoryWorkshop> result = factoryWorkshopService.pageWorkshops(current, size, keyword, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询车间。
     */
    @GetMapping("/{workshopId}")
    public ResultData getWorkshop(@PathVariable("workshopId") Long id) {
        FactoryWorkshop workshop = factoryWorkshopService.getWorkshopById(id);
        return ResultData.success(ResultCode.SUCCESS, workshop);
    }

    /**
     * 新增车间。
     */
    @PostMapping
    public ResultData save(@RequestBody FactoryWorkshop workshop) {
        boolean flag = factoryWorkshopService.saveWorkshop(workshop);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新车间。
     */
    @PutMapping
    public ResultData update(@RequestBody FactoryWorkshop workshop) {
        boolean flag = factoryWorkshopService.updateWorkshop(workshop);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除车间，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = factoryWorkshopService.removeWorkshopById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出车间 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String status) {
        List<FactoryWorkshop> workshops = factoryWorkshopService.listWorkshops(keyword, status);
        excelExportUtil.exportExcel(response, workshops, FactoryWorkshop.class, "车间列表", "车间列表");
    }
}
