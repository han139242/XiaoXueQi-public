package com.neuedu.ems.controller.factory;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.factory.FactoryStation;
import com.neuedu.ems.service.factory.FactoryStationService;
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
@RequestMapping("/api/factory/station")
public class FactoryStationController {

    @Autowired
    private FactoryStationService factoryStationService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询工位列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Long lineId,
                           @RequestParam(required = false) String stationType,
                           @RequestParam(required = false) String status) {
        Page<FactoryStation> result = factoryStationService.pageStations(
                current, size, keyword, lineId, stationType, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询工位。
     */
    @GetMapping("/{stationId}")
    public ResultData getStation(@PathVariable("stationId") Long id) {
        FactoryStation station = factoryStationService.getStationById(id);
        return ResultData.success(ResultCode.SUCCESS, station);
    }

    /**
     * 新增工位。
     */
    @PostMapping
    public ResultData save(@RequestBody FactoryStation station) {
        boolean flag = factoryStationService.saveStation(station);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新工位。
     */
    @PutMapping
    public ResultData update(@RequestBody FactoryStation station) {
        boolean flag = factoryStationService.updateStation(station);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除工位，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = factoryStationService.removeStationById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出工位 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) Long lineId,
                      @RequestParam(required = false) String stationType,
                      @RequestParam(required = false) String status) {
        List<FactoryStation> stations = factoryStationService.listStations(keyword, lineId, stationType, status);
        excelExportUtil.exportExcel(response, stations, FactoryStation.class, "工位列表", "工位列表");
    }
}
