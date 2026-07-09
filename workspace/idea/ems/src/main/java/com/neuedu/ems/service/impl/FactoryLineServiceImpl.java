package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.FactoryLine;
import com.neuedu.ems.mapper.FactoryLineMapper;
import com.neuedu.ems.service.FactoryLineService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 产线 Service 实现类。
 */
@Service
public class FactoryLineServiceImpl extends ServiceImpl<FactoryLineMapper, FactoryLine> implements FactoryLineService {

    @Override
    public Page<FactoryLine> pageLines(Integer current, Integer size, String keyword, Long workshopId, String status) {
        Page<FactoryLine> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildLineQuery(keyword, workshopId, status));
    }

    @Override
    public FactoryLine getLineById(Long lineId) {
        QueryWrapper<FactoryLine> wrapper = new QueryWrapper<>();
        wrapper.eq("line_id", lineId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveLine(FactoryLine line) {
        if (line.getDeletedFlag() == null) {
            line.setDeletedFlag(0);
        }
        return baseMapper.insert(line) > 0;
    }

    @Override
    public boolean updateLine(FactoryLine line) {
        if (line.getLineId() == null) {
            return false;
        }
        line.setDeletedFlag(null);
        return baseMapper.updateById(line) > 0;
    }

    @Override
    public boolean removeLineById(Long lineId) {
        FactoryLine line = new FactoryLine();
        line.setLineId(lineId);
        line.setDeletedFlag(1);
        return baseMapper.updateById(line) > 0;
    }

    @Override
    public List<FactoryLine> listLines(String keyword, Long workshopId, String status) {
        return baseMapper.selectList(buildLineQuery(keyword, workshopId, status));
    }

    private QueryWrapper<FactoryLine> buildLineQuery(String keyword, Long workshopId, String status) {
        QueryWrapper<FactoryLine> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like("line_code", keyword).or().like("line_name", keyword));
        }
        if (workshopId != null) {
            wrapper.eq("workshop_id", workshopId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("line_id");
        return wrapper;
    }
}
