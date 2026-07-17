package com.neuedu.ems.controller.mdm;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.mdm.MdmBom;
import com.neuedu.ems.service.mdm.MdmBomService;
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
@RequestMapping("/api/mdm/bom")
public class MdmBomController {

    @Autowired
    private MdmBomService mdmBomService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询产品物料清单列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String bomCode,
                           @RequestParam(required = false) Long productId,
                           @RequestParam(required = false) String status) {
        Page<MdmBom> result = mdmBomService.pageBoms(current, size, bomCode, productId, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询产品物料清单。
     */
    @GetMapping("/{bomId}")
    public ResultData getBom(@PathVariable("bomId") Long id) {
        MdmBom bom = mdmBomService.getBomById(id);
        return ResultData.success(ResultCode.SUCCESS, bom);
    }

    /**
     * 新增产品物料清单。
     */
    @PostMapping
    public ResultData save(@RequestBody MdmBom bom) {
        boolean flag = mdmBomService.saveBom(bom);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新产品物料清单。
     */
    @PutMapping
    public ResultData update(@RequestBody MdmBom bom) {
        boolean flag = mdmBomService.updateBom(bom);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除产品物料清单，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = mdmBomService.removeBomById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出产品物料清单 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String bomCode,
                      @RequestParam(required = false) Long productId,
                      @RequestParam(required = false) String status) {
        List<MdmBom> boms = mdmBomService.listBoms(bomCode, productId, status);
        excelExportUtil.exportExcel(response, boms, MdmBom.class, "产品物料清单列表", "产品物料清单列表");
    }
}
