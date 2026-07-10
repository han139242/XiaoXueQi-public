package com.neuedu.ems.service.mdm;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.mdm.MdmCustomer;

import java.util.List;

/**
 * 客户 Service 接口。
 */
public interface MdmCustomerService extends IService<MdmCustomer> {

    Page<MdmCustomer> pageCustomers(Integer current, Integer size, String keyword, String status);

    MdmCustomer getCustomerById(Long customerId);

    boolean saveCustomer(MdmCustomer customer);

    boolean updateCustomer(MdmCustomer customer);

    boolean removeCustomerById(Long customerId);

    List<MdmCustomer> listCustomers(String keyword, String status);
}
