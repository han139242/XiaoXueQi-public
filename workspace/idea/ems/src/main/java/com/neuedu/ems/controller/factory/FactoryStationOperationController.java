package com.neuedu.ems.controller.factory;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.factory.FactoryStationOperation;
import com.neuedu.ems.service.factory.FactoryStationOperationService;
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
@RequestMapping("/api/factory/station-operation")
public class FactoryStationOperationController {

    @Autowired
    private FactoryStationOperationService factoryStationOperationService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询工位工序关联列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) Long stationId,
                           @RequestParam(required = false) Long operationId,
                           @RequestParam(required = false) Integer isDefault,
                           @RequestParam(required = false) String status) {
        Page<FactoryStationOperation> result = factoryStationOperationService.pageStationOperations(
                current, size, stationId, operationId, isDefault, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询工位工序关联。
     */
    @GetMapping("/{id}")
    public ResultData getStationOperation(@PathVariable("id") Long id) {
        FactoryStationOperation stationOperation = factoryStationOperationService.getStationOperationById(id);
        return ResultData.success(ResultCode.SUCCESS, stationOperation);
    }

    /**
     * 根据工位查询可执行工序。
     */
    @GetMapping("/station/{stationId}")
    public ResultData listByStation(@PathVariable("stationId") Long stationId) {
        List<FactoryStationOperation> stationOperations =
                factoryStationOperationService.listOperationsByStationId(stationId);
        return ResultData.success(ResultCode.SUCCESS, stationOperations);
    }

    /**
     * 新增工位工序关联。
     */
    @PostMapping
    public ResultData save(@RequestBody FactoryStationOperation stationOperation) {
        boolean flag = factoryStationOperationService.saveStationOperation(stationOperation);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新工位工序关联。
     */
    @PutMapping
    public ResultData update(@RequestBody FactoryStationOperation stationOperation) {
        boolean flag = factoryStationOperationService.updateStationOperation(stationOperation);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除工位工序关联，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = factoryStationOperationService.removeStationOperationById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出工位工序关联 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) Long stationId,
                      @RequestParam(required = false) Long operationId,
                      @RequestParam(required = false) Integer isDefault,
                      @RequestParam(required = false) String status) {
        List<FactoryStationOperation> stationOperations = factoryStationOperationService.listStationOperations(
                stationId, operationId, isDefault, status);
        excelExportUtil.exportExcel(response, stationOperations, FactoryStationOperation.class,
                "工位工序关联列表", "工位工序关联列表");
    }
}
