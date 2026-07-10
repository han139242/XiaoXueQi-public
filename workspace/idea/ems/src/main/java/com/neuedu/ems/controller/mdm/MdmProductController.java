package com.neuedu.ems.controller.mdm;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.mdm.MdmProduct;
import com.neuedu.ems.service.mdm.MdmProductService;
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
@RequestMapping("/api/mdm/product")
public class MdmProductController {

    @Autowired
    private MdmProductService mdmProductService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询产品列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String productCode,
                           @RequestParam(required = false) String productName,
                           @RequestParam(required = false) String status) {
        Page<MdmProduct> result = mdmProductService.pageProducts(current, size, productCode, productName, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询产品。
     */
    @GetMapping("/{productId}")
    public ResultData getProduct(@PathVariable("productId") Long id) {
        MdmProduct product = mdmProductService.getProductById(id);
        return ResultData.success(ResultCode.SUCCESS, product);
    }

    /**
     * 新增产品。
     */
    @PostMapping
    public ResultData save(@RequestBody MdmProduct product) {
        boolean flag = mdmProductService.saveProduct(product);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新产品。
     */
    @PutMapping
    public ResultData update(@RequestBody MdmProduct product) {
        boolean flag = mdmProductService.updateProduct(product);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除产品，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = mdmProductService.removeProductById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出产品 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String productCode,
                      @RequestParam(required = false) String productName,
                      @RequestParam(required = false) String status) {
        List<MdmProduct> products = mdmProductService.listProducts(productCode, productName, status);
        excelExportUtil.exportExcel(response, products, MdmProduct.class, "产品列表", "产品列表");
    }
}
