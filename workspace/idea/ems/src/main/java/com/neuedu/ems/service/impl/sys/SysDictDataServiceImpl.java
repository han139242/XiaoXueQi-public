package com.neuedu.ems.service.impl.sys;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.sys.SysDictData;
import com.neuedu.ems.mapper.sys.SysDictDataMapper;
import com.neuedu.ems.service.sys.SysDictDataService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 字典数据 Service 实现类。
 */
@Service
public class SysDictDataServiceImpl extends ServiceImpl<SysDictDataMapper, SysDictData> implements SysDictDataService {

    @Override
    public Page<SysDictData> pageDictData(Integer current, Integer size, String keyword, String dictTypeCode) {
        Page<SysDictData> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildDictDataQuery(keyword, dictTypeCode));
    }

    @Override
    public List<SysDictData> listDictDataByTypeCode(String dictTypeCode) {
        QueryWrapper<SysDictData> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        wrapper.eq("dict_type_code", dictTypeCode);
        wrapper.orderByAsc("sort_no", "dict_data_id");
        return baseMapper.selectList(wrapper);
    }

    @Override
    public SysDictData getDictDataById(Long dictDataId) {
        QueryWrapper<SysDictData> wrapper = new QueryWrapper<>();
        wrapper.eq("dict_data_id", dictDataId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveDictData(SysDictData dictData) {
        if (dictData.getDeletedFlag() == null) {
            dictData.setDeletedFlag(0);
        }
        return baseMapper.insert(dictData) > 0;
    }

    @Override
    public boolean updateDictData(SysDictData dictData) {
        if (dictData.getDictDataId() == null) {
            return false;
        }
        dictData.setDeletedFlag(null);
        return baseMapper.updateById(dictData) > 0;
    }

    @Override
    public boolean removeDictDataById(Long dictDataId) {
        SysDictData dictData = new SysDictData();
        dictData.setDictDataId(dictDataId);
        dictData.setDeletedFlag(1);
        return baseMapper.updateById(dictData) > 0;
    }

    @Override
    public List<SysDictData> listDictData(String keyword, String dictTypeCode) {
        return baseMapper.selectList(buildDictDataQuery(keyword, dictTypeCode));
    }

    private QueryWrapper<SysDictData> buildDictDataQuery(String keyword, String dictTypeCode) {
        QueryWrapper<SysDictData> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(dictTypeCode)) {
            wrapper.eq("dict_type_code", dictTypeCode);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like("dict_value", keyword).or().like("dict_label", keyword));
        }
        wrapper.orderByAsc("sort_no", "dict_data_id");
        return wrapper;
    }
}
