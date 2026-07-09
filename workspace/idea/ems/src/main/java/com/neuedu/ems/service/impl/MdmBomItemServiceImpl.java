package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.MdmBomItem;
import com.neuedu.ems.mapper.MdmBomItemMapper;
import com.neuedu.ems.service.MdmBomItemService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * BOM 明细 Service 实现类。
 */
@Service
public class MdmBomItemServiceImpl extends ServiceImpl<MdmBomItemMapper, MdmBomItem> implements MdmBomItemService {

    @Override
    public Page<MdmBomItem> pageBomItems(Integer current, Integer size, Long bomId, Long materialId) {
        Page<MdmBomItem> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildBomItemQuery(bomId, materialId));
    }

    @Override
    public MdmBomItem getBomItemById(Long bomItemId) {
        QueryWrapper<MdmBomItem> wrapper = new QueryWrapper<>();
        wrapper.eq("bom_item_id", bomItemId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveBomItem(MdmBomItem item) {
        if (item.getDeletedFlag() == null) {
            item.setDeletedFlag(0);
        }
        return baseMapper.insert(item) > 0;
    }

    @Override
    public boolean updateBomItem(MdmBomItem item) {
        if (item.getBomItemId() == null) {
            return false;
        }
        item.setDeletedFlag(null);
        return baseMapper.updateById(item) > 0;
    }

    @Override
    public boolean removeBomItemById(Long bomItemId) {
        MdmBomItem item = new MdmBomItem();
        item.setBomItemId(bomItemId);
        item.setDeletedFlag(1);
        return baseMapper.updateById(item) > 0;
    }

    @Override
    public List<MdmBomItem> listBomItems(Long bomId, Long materialId) {
        return baseMapper.selectList(buildBomItemQuery(bomId, materialId));
    }

    private QueryWrapper<MdmBomItem> buildBomItemQuery(Long bomId, Long materialId) {
        QueryWrapper<MdmBomItem> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (bomId != null) {
            wrapper.eq("bom_id", bomId);
        }
        if (materialId != null) {
            wrapper.eq("material_id", materialId);
        }
        wrapper.orderByAsc("sequence_no", "bom_item_id");
        return wrapper;
    }
}
