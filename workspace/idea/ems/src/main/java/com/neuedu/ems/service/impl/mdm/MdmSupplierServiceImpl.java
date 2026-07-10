package com.neuedu.ems.service.impl.mdm;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.mdm.MdmSupplier;
import com.neuedu.ems.mapper.mdm.MdmSupplierMapper;
import com.neuedu.ems.service.mdm.MdmSupplierService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 供应商 Service 实现类。
 */
@Service
public class MdmSupplierServiceImpl extends ServiceImpl<MdmSupplierMapper, MdmSupplier> implements MdmSupplierService {

    @Override
    public Page<MdmSupplier> pageSuppliers(Integer current, Integer size, String keyword, String status) {
        Page<MdmSupplier> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildSupplierQuery(keyword, status));
    }

    @Override
    public MdmSupplier getSupplierById(Long supplierId) {
        QueryWrapper<MdmSupplier> wrapper = new QueryWrapper<>();
        wrapper.eq("supplier_id", supplierId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveSupplier(MdmSupplier supplier) {
        if (supplier.getDeletedFlag() == null) {
            supplier.setDeletedFlag(0);
        }
        return baseMapper.insert(supplier) > 0;
    }

    @Override
    public boolean updateSupplier(MdmSupplier supplier) {
        if (supplier.getSupplierId() == null) {
            return false;
        }
        supplier.setDeletedFlag(null);
        return baseMapper.updateById(supplier) > 0;
    }

    @Override
    public boolean removeSupplierById(Long supplierId) {
        MdmSupplier supplier = new MdmSupplier();
        supplier.setSupplierId(supplierId);
        supplier.setDeletedFlag(1);
        return baseMapper.updateById(supplier) > 0;
    }

    @Override
    public List<MdmSupplier> listSuppliers(String keyword, String status) {
        return baseMapper.selectList(buildSupplierQuery(keyword, status));
    }

    private QueryWrapper<MdmSupplier> buildSupplierQuery(String keyword, String status) {
        QueryWrapper<MdmSupplier> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like("supplier_code", keyword)
                    .or()
                    .like("supplier_name", keyword)
                    .or()
                    .like("contact_name", keyword)
                    .or()
                    .like("contact_phone", keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("supplier_id");
        return wrapper;
    }
}
