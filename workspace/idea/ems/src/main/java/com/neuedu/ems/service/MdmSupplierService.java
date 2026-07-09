package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.MdmSupplier;

import java.util.List;

/**
 * 供应商 Service 接口。
 */
public interface MdmSupplierService extends IService<MdmSupplier> {

    Page<MdmSupplier> pageSuppliers(Integer current, Integer size, String keyword, String status);

    MdmSupplier getSupplierById(Long supplierId);

    boolean saveSupplier(MdmSupplier supplier);

    boolean updateSupplier(MdmSupplier supplier);

    boolean removeSupplierById(Long supplierId);

    List<MdmSupplier> listSuppliers(String keyword, String status);
}
