package com.neuedu.ems.controller.mdm;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.mdm.MdmBomItem;
import com.neuedu.ems.service.mdm.MdmBomItemService;
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
@RequestMapping("/api/mdm/bom-item")
public class MdmBomItemController {

    @Autowired
    private MdmBomItemService mdmBomItemService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询 BOM 明细列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) Long bomId,
                           @RequestParam(required = false) Long materialId) {
        Page<MdmBomItem> result = mdmBomItemService.pageBomItems(current, size, bomId, materialId);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询 BOM 明细。
     */
    @GetMapping("/{bomItemId}")
    public ResultData getBomItem(@PathVariable("bomItemId") Long id) {
        MdmBomItem item = mdmBomItemService.getBomItemById(id);
        return ResultData.success(ResultCode.SUCCESS, item);
    }

    /**
     * 新增 BOM 明细。
     */
    @PostMapping
    public ResultData save(@RequestBody MdmBomItem item) {
        boolean flag = mdmBomItemService.saveBomItem(item);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新 BOM 明细。
     */
    @PutMapping
    public ResultData update(@RequestBody MdmBomItem item) {
        boolean flag = mdmBomItemService.updateBomItem(item);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除 BOM 明细，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = mdmBomItemService.removeBomItemById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出 BOM 明细 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) Long bomId,
                      @RequestParam(required = false) Long materialId) {
        List<MdmBomItem> items = mdmBomItemService.listBomItems(bomId, materialId);
        excelExportUtil.exportExcel(response, items, MdmBomItem.class, "BOM明细列表", "BOM明细列表");
    }
}
