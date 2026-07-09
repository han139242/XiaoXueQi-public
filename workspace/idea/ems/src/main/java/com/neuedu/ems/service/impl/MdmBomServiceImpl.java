package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.MdmBom;
import com.neuedu.ems.mapper.MdmBomMapper;
import com.neuedu.ems.service.MdmBomService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * BOM Service 实现类。
 */
@Service
public class MdmBomServiceImpl extends ServiceImpl<MdmBomMapper, MdmBom> implements MdmBomService {

    @Override
    public Page<MdmBom> pageBoms(Integer current, Integer size, String bomCode, Long productId, String status) {
        Page<MdmBom> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildBomQuery(bomCode, productId, status));
    }

    @Override
    public MdmBom getBomById(Long bomId) {
        QueryWrapper<MdmBom> wrapper = new QueryWrapper<>();
        wrapper.eq("bom_id", bomId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveBom(MdmBom bom) {
        if (bom.getDeletedFlag() == null) {
            bom.setDeletedFlag(0);
        }
        return baseMapper.insert(bom) > 0;
    }

    @Override
    public boolean updateBom(MdmBom bom) {
        if (bom.getBomId() == null) {
            return false;
        }
        bom.setDeletedFlag(null);
        return baseMapper.updateById(bom) > 0;
    }

    @Override
    public boolean removeBomById(Long bomId) {
        MdmBom bom = new MdmBom();
        bom.setBomId(bomId);
        bom.setDeletedFlag(1);
        return baseMapper.updateById(bom) > 0;
    }

    @Override
    public List<MdmBom> listBoms(String bomCode, Long productId, String status) {
        return baseMapper.selectList(buildBomQuery(bomCode, productId, status));
    }

    private QueryWrapper<MdmBom> buildBomQuery(String bomCode, Long productId, String status) {
        QueryWrapper<MdmBom> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(bomCode)) {
            wrapper.like("bom_code", bomCode);
        }
        if (productId != null) {
            wrapper.eq("product_id", productId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("bom_id");
        return wrapper;
    }
}
