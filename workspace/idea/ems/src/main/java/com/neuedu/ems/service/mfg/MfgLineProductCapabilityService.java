package com.neuedu.ems.service.mfg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.mfg.MfgLineProductCapability;

import java.util.List;

/**
 * 产线产品能力配置服务。
 */
public interface MfgLineProductCapabilityService extends IService<MfgLineProductCapability> {

    Page<MfgLineProductCapability> pageCapabilities(Integer current, Integer size, Long lineId,
                                                     Long productId, Long routeId, String status);

    MfgLineProductCapability getCapabilityById(Long capabilityId);

    boolean saveCapability(MfgLineProductCapability capability);

    boolean updateCapability(MfgLineProductCapability capability);

    boolean disableCapability(Long capabilityId);

    List<MfgLineProductCapability> listCapabilities(Long lineId, Long productId, Long routeId, String status);
}
