package com.neuedu.ems.service.org;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.org.OrgShift;

import java.util.List;

/**
 * 班次 Service 接口。
 */
public interface OrgShiftService extends IService<OrgShift> {

    Page<OrgShift> pageShifts(Integer current, Integer size, String keyword);

    OrgShift getShiftById(Long shiftId);

    boolean saveShift(OrgShift shift);

    boolean updateShift(OrgShift shift);

    boolean removeShiftById(Long shiftId);

    List<OrgShift> listShifts(String keyword);
}
