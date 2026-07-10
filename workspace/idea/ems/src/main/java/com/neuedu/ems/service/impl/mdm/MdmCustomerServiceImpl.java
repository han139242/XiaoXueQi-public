package com.neuedu.ems.service.impl.mdm;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.mdm.MdmCustomer;
import com.neuedu.ems.mapper.mdm.MdmCustomerMapper;
import com.neuedu.ems.service.mdm.MdmCustomerService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 客户 Service 实现类。
 */
@Service
public class MdmCustomerServiceImpl extends ServiceImpl<MdmCustomerMapper, MdmCustomer> implements MdmCustomerService {

    @Override
    public Page<MdmCustomer> pageCustomers(Integer current, Integer size, String keyword, String status) {
        Page<MdmCustomer> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildCustomerQuery(keyword, status));
    }

    @Override
    public MdmCustomer getCustomerById(Long customerId) {
        QueryWrapper<MdmCustomer> wrapper = new QueryWrapper<>();
        wrapper.eq("customer_id", customerId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveCustomer(MdmCustomer customer) {
        if (customer.getDeletedFlag() == null) {
            customer.setDeletedFlag(0);
        }
        return baseMapper.insert(customer) > 0;
    }

    @Override
    public boolean updateCustomer(MdmCustomer customer) {
        if (customer.getCustomerId() == null) {
            return false;
        }
        customer.setDeletedFlag(null);
        return baseMapper.updateById(customer) > 0;
    }

    @Override
    public boolean removeCustomerById(Long customerId) {
        MdmCustomer customer = new MdmCustomer();
        customer.setCustomerId(customerId);
        customer.setDeletedFlag(1);
        return baseMapper.updateById(customer) > 0;
    }

    @Override
    public List<MdmCustomer> listCustomers(String keyword, String status) {
        return baseMapper.selectList(buildCustomerQuery(keyword, status));
    }

    private QueryWrapper<MdmCustomer> buildCustomerQuery(String keyword, String status) {
        QueryWrapper<MdmCustomer> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like("customer_code", keyword)
                    .or()
                    .like("customer_name", keyword)
                    .or()
                    .like("contact_name", keyword)
                    .or()
                    .like("contact_phone", keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("customer_id");
        return wrapper;
    }
}
