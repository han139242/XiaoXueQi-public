package com.neuedu.ems.service.mdm;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.mdm.MdmUnit;

import java.util.List;

/**
 * 计量单位 Service 接口。
 */
public interface MdmUnitService extends IService<MdmUnit> {

    Page<MdmUnit> pageUnits(Integer current, Integer size, String keyword, String status);

    MdmUnit getUnitById(Long unitId);

    boolean saveUnit(MdmUnit unit);

    boolean updateUnit(MdmUnit unit);

    boolean removeUnitById(Long unitId);

    List<MdmUnit> listUnits(String keyword, String status);
}
