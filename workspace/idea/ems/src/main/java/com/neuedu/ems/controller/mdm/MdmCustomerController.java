package com.neuedu.ems.controller.mdm;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.mdm.MdmCustomer;
import com.neuedu.ems.service.mdm.MdmCustomerService;
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
@RequestMapping("/api/mdm/customer")
public class MdmCustomerController {

    @Autowired
    private MdmCustomerService mdmCustomerService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询客户列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String status) {
        Page<MdmCustomer> result = mdmCustomerService.pageCustomers(current, size, keyword, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询客户。
     */
    @GetMapping("/{customerId}")
    public ResultData getCustomer(@PathVariable("customerId") Long id) {
        MdmCustomer customer = mdmCustomerService.getCustomerById(id);
        return ResultData.success(ResultCode.SUCCESS, customer);
    }

    /**
     * 新增客户。
     */
    @PostMapping
    public ResultData save(@RequestBody MdmCustomer customer) {
        boolean flag = mdmCustomerService.saveCustomer(customer);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新客户。
     */
    @PutMapping
    public ResultData update(@RequestBody MdmCustomer customer) {
        boolean flag = mdmCustomerService.updateCustomer(customer);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除客户，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = mdmCustomerService.removeCustomerById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出客户 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String status) {
        List<MdmCustomer> customers = mdmCustomerService.listCustomers(keyword, status);
        excelExportUtil.exportExcel(response, customers, MdmCustomer.class, "客户列表", "客户列表");
    }
}
