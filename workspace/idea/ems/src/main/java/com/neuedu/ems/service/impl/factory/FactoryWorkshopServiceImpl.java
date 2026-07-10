package com.neuedu.ems.service.impl.factory;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.factory.FactoryWorkshop;
import com.neuedu.ems.mapper.factory.FactoryWorkshopMapper;
import com.neuedu.ems.service.factory.FactoryWorkshopService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 车间 Service 实现类。
 */
@Service
public class FactoryWorkshopServiceImpl
        extends ServiceImpl<FactoryWorkshopMapper, FactoryWorkshop>
        implements FactoryWorkshopService {

    @Override
    public Page<FactoryWorkshop> pageWorkshops(Integer current, Integer size, String keyword, String status) {
        Page<FactoryWorkshop> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildWorkshopQuery(keyword, status));
    }

    @Override
    public FactoryWorkshop getWorkshopById(Long workshopId) {
        QueryWrapper<FactoryWorkshop> wrapper = new QueryWrapper<>();
        wrapper.eq("workshop_id", workshopId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveWorkshop(FactoryWorkshop workshop) {
        if (workshop.getDeletedFlag() == null) {
            workshop.setDeletedFlag(0);
        }
        return baseMapper.insert(workshop) > 0;
    }

    @Override
    public boolean updateWorkshop(FactoryWorkshop workshop) {
        if (workshop.getWorkshopId() == null) {
            return false;
        }
        workshop.setDeletedFlag(null);
        return baseMapper.updateById(workshop) > 0;
    }

    @Override
    public boolean removeWorkshopById(Long workshopId) {
        FactoryWorkshop workshop = new FactoryWorkshop();
        workshop.setWorkshopId(workshopId);
        workshop.setDeletedFlag(1);
        return baseMapper.updateById(workshop) > 0;
    }

    @Override
    public List<FactoryWorkshop> listWorkshops(String keyword, String status) {
        return baseMapper.selectList(buildWorkshopQuery(keyword, status));
    }

    private QueryWrapper<FactoryWorkshop> buildWorkshopQuery(String keyword, String status) {
        QueryWrapper<FactoryWorkshop> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like("workshop_code", keyword).or().like("workshop_name", keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("workshop_id");
        return wrapper;
    }
}
