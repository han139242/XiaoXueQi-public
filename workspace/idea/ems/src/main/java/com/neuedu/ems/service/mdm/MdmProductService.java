package com.neuedu.ems.service.mdm;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.mdm.MdmProduct;

import java.util.List;

/**
 * 产品 Service 接口。
 */
public interface MdmProductService extends IService<MdmProduct> {

    Page<MdmProduct> pageProducts(Integer current, Integer size, String productCode, String productName, String status);

    MdmProduct getProductById(Long productId);

    boolean saveProduct(MdmProduct product);

    boolean updateProduct(MdmProduct product);

    boolean removeProductById(Long productId);

    List<MdmProduct> listProducts(String productCode, String productName, String status);
}
