package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.WmsLocation;
import com.neuedu.ems.service.WmsLocationService;
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
@RequestMapping("/api/wms/location")
public class WmsLocationController {

    @Autowired
    private WmsLocationService wmsLocationService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询库位列表，支持关键字搜索和状态筛选。
     * @param current 当前页码，默认为1
     * @param size 每页记录数，默认为10
     * @param keyword 关键字（匹配库位编码或库位名称），可选
     * @param status 库位状态，可选
     * @return 分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String status) {
        Page<WmsLocation> result = wmsLocationService.pageLocations(current, size, keyword, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据库位ID查询库位详情。
     * @param id 库位ID
     * @return 库位实体信息
     */
    @GetMapping("/{locationId}")
    public ResultData getLocation(@PathVariable("locationId") Long id) {
        WmsLocation location = wmsLocationService.getLocationById(id);
        return ResultData.success(ResultCode.SUCCESS, location);
    }

    /**
     * 新增库位。
     * @param location 库位实体（JSON格式）
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody WmsLocation location) {
        boolean flag = wmsLocationService.saveLocation(location);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新库位信息。
     * @param location 库位实体（JSON格式，必须包含locationId）
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody WmsLocation location) {
        boolean flag = wmsLocationService.updateLocation(location);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除库位（软删除，将删除标志设为1）。
     * @param id 库位ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = wmsLocationService.removeLocationById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出库位列表为Excel文件。
     * @param response HTTP响应对象
     * @param keyword 关键字（匹配库位编码或库位名称），可选
     * @param status 库位状态，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String status) {
        List<WmsLocation> locations = wmsLocationService.listLocations(keyword, status);
        excelExportUtil.exportExcel(response, locations, WmsLocation.class, "库位列表", "库位列表");
    }
}
