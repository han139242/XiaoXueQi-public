package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.MdmBom;

import java.util.List;

/**
 * BOM Service 接口。
 */
public interface MdmBomService extends IService<MdmBom> {

    Page<MdmBom> pageBoms(Integer current, Integer size, String bomCode, Long productId, String status);

    MdmBom getBomById(Long bomId);

    boolean saveBom(MdmBom bom);

    boolean updateBom(MdmBom bom);

    boolean removeBomById(Long bomId);

    List<MdmBom> listBoms(String bomCode, Long productId, String status);
}
