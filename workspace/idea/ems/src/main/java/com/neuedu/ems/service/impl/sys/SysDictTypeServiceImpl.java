package com.neuedu.ems.service.impl.sys;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.sys.SysDictType;
import com.neuedu.ems.mapper.sys.SysDictTypeMapper;
import com.neuedu.ems.service.sys.SysDictTypeService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 字典类型 Service 实现类。
 */
@Service
public class SysDictTypeServiceImpl extends ServiceImpl<SysDictTypeMapper, SysDictType> implements SysDictTypeService {

    @Override
    public Page<SysDictType> pageDictTypes(Integer current, Integer size, String keyword, String status) {
        Page<SysDictType> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildDictTypeQuery(keyword, status));
    }

    @Override
    public SysDictType getDictTypeById(Long dictTypeId) {
        QueryWrapper<SysDictType> wrapper = new QueryWrapper<>();
        wrapper.eq("dict_type_id", dictTypeId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveDictType(SysDictType dictType) {
        if (dictType.getDeletedFlag() == null) {
            dictType.setDeletedFlag(0);
        }
        return baseMapper.insert(dictType) > 0;
    }

    @Override
    public boolean updateDictType(SysDictType dictType) {
        if (dictType.getDictTypeId() == null) {
            return false;
        }
        dictType.setDeletedFlag(null);
        return baseMapper.updateById(dictType) > 0;
    }

    @Override
    public boolean removeDictTypeById(Long dictTypeId) {
        SysDictType dictType = new SysDictType();
        dictType.setDictTypeId(dictTypeId);
        dictType.setDeletedFlag(1);
        return baseMapper.updateById(dictType) > 0;
    }

    @Override
    public List<SysDictType> listDictTypes(String keyword, String status) {
        return baseMapper.selectList(buildDictTypeQuery(keyword, status));
    }

    private QueryWrapper<SysDictType> buildDictTypeQuery(String keyword, String status) {
        QueryWrapper<SysDictType> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like("dict_type_code", keyword).or().like("dict_type_name", keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("dict_type_id");
        return wrapper;
    }
}
