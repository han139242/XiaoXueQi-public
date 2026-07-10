package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.WmsReplenishmentSuggestion;
import com.neuedu.ems.service.WmsReplenishmentSuggestionService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 补货建议管理控制器。
 */
@RestController
@RequestMapping("/api/wms/replenishment-suggestion")
public class WmsReplenishmentSuggestionController {

    @Autowired
    private WmsReplenishmentSuggestionService wmsReplenishmentSuggestionService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询补货建议列表，支持建议编号、物料、来源类型和状态筛选。
     * @param current 当前页码，默认1
     * @param size 每页记录数，默认10
     * @param keyword 建议编号关键字，可选
     * @param materialId 物料ID，可选
     * @param sourceType 来源类型，可选
     * @param status 状态，可选
     * @return 补货建议分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Long materialId,
                           @RequestParam(required = false) String sourceType,
                           @RequestParam(required = false) String status) {
        Page<WmsReplenishmentSuggestion> result = wmsReplenishmentSuggestionService.pageSuggestions(current, size, keyword, materialId, sourceType, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据补货建议ID查询详情。
     * @param suggestionId 补货建议ID
     * @return 补货建议实体
     */
    @GetMapping("/{suggestionId}")
    public ResultData getSuggestion(@PathVariable("suggestionId") Long suggestionId) {
        WmsReplenishmentSuggestion suggestion = wmsReplenishmentSuggestionService.getSuggestionById(suggestionId);
        return ResultData.success(ResultCode.SUCCESS, suggestion);
    }

    /**
     * 新增补货建议。
     * @param suggestion 补货建议实体，JSON格式
     * @return 操作结果或补货建议ID
     */
    @PostMapping
    public ResultData save(@RequestBody WmsReplenishmentSuggestion suggestion) {
        boolean flag = wmsReplenishmentSuggestionService.saveSuggestion(suggestion);
        return flag ? ResultData.success(ResultCode.SUCCESS, Map.of("suggestionId", suggestion.getSuggestionId())) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 根据物料和仓库生成补货建议，warehouseId只用于计算库存，建议表不保存仓库字段。
     * @param request 请求体，支持materialId、warehouseId、requiredQuantity参数
     * @return 补货建议ID
     */
    @PostMapping("/generate")
    public ResultData generate(@RequestBody Map<String, Object> request) {
        Long suggestionId = wmsReplenishmentSuggestionService.generateSuggestion(
                getLong(request, "materialId"), getLong(request, "warehouseId"), getBigDecimal(request, "requiredQuantity"));
        return suggestionId == null ? ResultData.fail(ResultCode.FAILED) : ResultData.success(ResultCode.SUCCESS, Map.of("suggestionId", suggestionId));
    }

    /**
     * 修改补货建议。
     * @param suggestion 补货建议实体，JSON格式，必须包含suggestionId
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody WmsReplenishmentSuggestion suggestion) {
        boolean flag = wmsReplenishmentSuggestionService.updateSuggestion(suggestion);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 软删除补货建议。
     * @param suggestionId 补货建议ID
     * @return 操作结果
     */
    @DeleteMapping("/{suggestionId}")
    public ResultData delete(@PathVariable("suggestionId") Long suggestionId) {
        boolean flag = wmsReplenishmentSuggestionService.removeSuggestionById(suggestionId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出补货建议列表为Excel文件。
     * @param response HTTP响应对象
     * @param keyword 建议编号关键字，可选
     * @param materialId 物料ID，可选
     * @param sourceType 来源类型，可选
     * @param status 状态，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) Long materialId,
                      @RequestParam(required = false) String sourceType,
                      @RequestParam(required = false) String status) {
        List<WmsReplenishmentSuggestion> suggestions = wmsReplenishmentSuggestionService.listSuggestions(keyword, materialId, sourceType, status);
        excelExportUtil.exportExcel(response, suggestions, WmsReplenishmentSuggestion.class, "补货建议列表", "补货建议列表");
    }

    /**
     * 从请求体中读取Long类型参数。
     * @param request 请求体Map
     * @param key 参数名
     * @return Long类型参数值
     */
    private Long getLong(Map<String, Object> request, String key) {
        Object value = request == null ? null : request.get(key);
        return value == null ? null : Long.valueOf(value.toString());
    }

    /**
     * 从请求体中读取BigDecimal类型参数。
     * @param request 请求体Map
     * @param key 参数名
     * @return BigDecimal类型参数值
     */
    private BigDecimal getBigDecimal(Map<String, Object> request, String key) {
        Object value = request == null ? null : request.get(key);
        return value == null ? null : new BigDecimal(value.toString());
    }
}
