package com.neuedu.ems.controller.mfg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.mfg.MfgLineProductCapability;
import com.neuedu.ems.service.mfg.MfgLineProductCapabilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 产线产品能力配置控制器。
 */
@RestController
@RequestMapping("/api/mfg/line-product-capability")
public class MfgLineProductCapabilityController {

    @Autowired
    private MfgLineProductCapabilityService mfgLineProductCapabilityService;

    @GetMapping("/page")
    public ResultData page(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) Long lineId,
                           @RequestParam(required = false) Long productId,
                           @RequestParam(required = false) Long routeId,
                           @RequestParam(required = false) String status) {
        Page<MfgLineProductCapability> result = mfgLineProductCapabilityService
                .pageCapabilities(current, size, lineId, productId, routeId, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    @GetMapping("/{capabilityId}")
    public ResultData detail(@PathVariable Long capabilityId) {
        return ResultData.success(ResultCode.SUCCESS,
                mfgLineProductCapabilityService.getCapabilityById(capabilityId));
    }

    @PostMapping
    public ResultData create(@RequestBody MfgLineProductCapability capability) {
        return mfgLineProductCapabilityService.saveCapability(capability)
                ? ResultData.success(ResultCode.SUCCESS)
                : ResultData.fail(ResultCode.FAILED);
    }

    @PutMapping
    public ResultData update(@RequestBody MfgLineProductCapability capability) {
        return mfgLineProductCapabilityService.updateCapability(capability)
                ? ResultData.success(ResultCode.SUCCESS)
                : ResultData.fail(ResultCode.FAILED);
    }

    @PutMapping("/{capabilityId}/disable")
    public ResultData disable(@PathVariable Long capabilityId) {
        return mfgLineProductCapabilityService.disableCapability(capabilityId)
                ? ResultData.success(ResultCode.SUCCESS)
                : ResultData.fail(ResultCode.FAILED);
    }
}
