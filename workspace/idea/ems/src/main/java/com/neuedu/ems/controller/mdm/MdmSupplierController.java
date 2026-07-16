package com.neuedu.ems.controller.mdm;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.dto.mdm.MdmSupplierRequest;
import com.neuedu.ems.dto.mdm.MdmSupplierView;
import com.neuedu.ems.entity.mdm.MdmSupplier;
import com.neuedu.ems.security.phone.SensitivePhoneDtoMapper;
import com.neuedu.ems.service.mdm.MdmSupplierService;
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
@RequestMapping("/api/mdm/supplier")
public class MdmSupplierController {

    @Autowired
    private MdmSupplierService mdmSupplierService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    @Autowired
    private SensitivePhoneDtoMapper sensitivePhoneDtoMapper;

    /**
     * 分页查询供应商列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String status) {
        return ResultData.success(ResultCode.SUCCESS,
                toViewPage(mdmSupplierService.pageSuppliers(current, size, keyword, status)));
    }

    /**
     * 根据 ID 查询供应商。
     */
    @GetMapping("/{supplierId}")
    public ResultData getSupplier(@PathVariable("supplierId") Long id) {
        MdmSupplier supplier = mdmSupplierService.getSupplierById(id);
        return ResultData.success(ResultCode.SUCCESS, sensitivePhoneDtoMapper.toView(supplier));
    }

    /**
     * 新增供应商。
     */
    @PostMapping
    public ResultData save(@RequestBody MdmSupplierRequest supplier) {
        boolean flag = mdmSupplierService.saveSupplier(sensitivePhoneDtoMapper.toEntity(supplier), supplier.getContactPhone());
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新供应商。
     */
    @PutMapping
    public ResultData update(@RequestBody MdmSupplierRequest supplier) {
        boolean flag = mdmSupplierService.updateSupplier(sensitivePhoneDtoMapper.toEntity(supplier), supplier.getContactPhone());
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除供应商，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = mdmSupplierService.removeSupplierById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出供应商 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String status) {
        List<MdmSupplierView> suppliers = mdmSupplierService.listSuppliers(keyword, status).stream()
                .map(sensitivePhoneDtoMapper::toView)
                .toList();
        excelExportUtil.exportExcel(response, suppliers, MdmSupplierView.class, "供应商列表", "供应商列表");
    }

    private Page<MdmSupplierView> toViewPage(Page<MdmSupplier> source) {
        Page<MdmSupplierView> page = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        page.setRecords(source.getRecords().stream().map(sensitivePhoneDtoMapper::toView).toList());
        return page;
    }
}
