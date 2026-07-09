package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.MdmProduct;
import com.neuedu.ems.mapper.MdmProductMapper;
import com.neuedu.ems.service.MdmProductService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 产品 Service 实现类。
 */
@Service
public class MdmProductServiceImpl extends ServiceImpl<MdmProductMapper, MdmProduct> implements MdmProductService {

    @Override
    public Page<MdmProduct> pageProducts(Integer current, Integer size, String productCode,
                                         String productName, String status) {
        Page<MdmProduct> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildProductQuery(productCode, productName, status));
    }

    @Override
    public MdmProduct getProductById(Long productId) {
        QueryWrapper<MdmProduct> wrapper = new QueryWrapper<>();
        wrapper.eq("product_id", productId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveProduct(MdmProduct product) {
        if (product.getDeletedFlag() == null) {
            product.setDeletedFlag(0);
        }
        return baseMapper.insert(product) > 0;
    }

    @Override
    public boolean updateProduct(MdmProduct product) {
        if (product.getProductId() == null) {
            return false;
        }
        product.setDeletedFlag(null);
        return baseMapper.updateById(product) > 0;
    }

    @Override
    public boolean removeProductById(Long productId) {
        MdmProduct product = new MdmProduct();
        product.setProductId(productId);
        product.setDeletedFlag(1);
        return baseMapper.updateById(product) > 0;
    }

    @Override
    public List<MdmProduct> listProducts(String productCode, String productName, String status) {
        return baseMapper.selectList(buildProductQuery(productCode, productName, status));
    }

    private QueryWrapper<MdmProduct> buildProductQuery(String productCode, String productName, String status) {
        QueryWrapper<MdmProduct> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(productCode)) {
            wrapper.like("product_code", productCode);
        }
        if (StringUtils.hasText(productName)) {
            wrapper.like("product_name", productName);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("product_id");
        return wrapper;
    }
}
