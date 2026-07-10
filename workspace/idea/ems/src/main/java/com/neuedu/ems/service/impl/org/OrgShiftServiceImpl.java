package com.neuedu.ems.service.impl.org;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.org.OrgShift;
import com.neuedu.ems.mapper.org.OrgShiftMapper;
import com.neuedu.ems.service.org.OrgShiftService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 班次 Service 实现类。
 */
@Service
public class OrgShiftServiceImpl extends ServiceImpl<OrgShiftMapper, OrgShift> implements OrgShiftService {

    @Override
    public Page<OrgShift> pageShifts(Integer current, Integer size, String keyword) {
        Page<OrgShift> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildShiftQuery(keyword));
    }

    @Override
    public OrgShift getShiftById(Long shiftId) {
        QueryWrapper<OrgShift> wrapper = new QueryWrapper<>();
        wrapper.eq("shift_id", shiftId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveShift(OrgShift shift) {
        if (shift.getDeletedFlag() == null) {
            shift.setDeletedFlag(0);
        }
        return baseMapper.insert(shift) > 0;
    }

    @Override
    public boolean updateShift(OrgShift shift) {
        if (shift.getShiftId() == null) {
            return false;
        }
        shift.setDeletedFlag(null);
        return baseMapper.updateById(shift) > 0;
    }

    @Override
    public boolean removeShiftById(Long shiftId) {
        OrgShift shift = new OrgShift();
        shift.setShiftId(shiftId);
        shift.setDeletedFlag(1);
        return baseMapper.updateById(shift) > 0;
    }

    @Override
    public List<OrgShift> listShifts(String keyword) {
        return baseMapper.selectList(buildShiftQuery(keyword));
    }

    private QueryWrapper<OrgShift> buildShiftQuery(String keyword) {
        QueryWrapper<OrgShift> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like("shift_code", keyword).or().like("shift_name", keyword));
        }
        wrapper.orderByDesc("shift_id");
        return wrapper;
    }
}
