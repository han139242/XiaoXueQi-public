package com.neuedu.ems.service.impl.mdm;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.mdm.MdmUnit;
import com.neuedu.ems.mapper.mdm.MdmUnitMapper;
import com.neuedu.ems.service.mdm.MdmUnitService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 计量单位 Service 实现类。
 */
@Service
public class MdmUnitServiceImpl extends ServiceImpl<MdmUnitMapper, MdmUnit> implements MdmUnitService {

    @Override
    public Page<MdmUnit> pageUnits(Integer current, Integer size, String keyword, String status) {
        Page<MdmUnit> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildUnitQuery(keyword, status));
    }

    @Override
    public MdmUnit getUnitById(Long unitId) {
        QueryWrapper<MdmUnit> wrapper = new QueryWrapper<>();
        wrapper.eq("unit_id", unitId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveUnit(MdmUnit unit) {
        if (unit.getDeletedFlag() == null) {
            unit.setDeletedFlag(0);
        }
        return baseMapper.insert(unit) > 0;
    }

    @Override
    public boolean updateUnit(MdmUnit unit) {
        if (unit.getUnitId() == null) {
            return false;
        }
        unit.setDeletedFlag(null);
        return baseMapper.updateById(unit) > 0;
    }

    @Override
    public boolean removeUnitById(Long unitId) {
        MdmUnit unit = new MdmUnit();
        unit.setUnitId(unitId);
        unit.setDeletedFlag(1);
        return baseMapper.updateById(unit) > 0;
    }

    @Override
    public List<MdmUnit> listUnits(String keyword, String status) {
        return baseMapper.selectList(buildUnitQuery(keyword, status));
    }

    private QueryWrapper<MdmUnit> buildUnitQuery(String keyword, String status) {
        QueryWrapper<MdmUnit> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like("unit_code", keyword).or().like("unit_name", keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("unit_id");
        return wrapper;
    }
}
