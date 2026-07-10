package com.neuedu.ems.service.sys;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.sys.SysDictType;

import java.util.List;

/**
 * 字典类型 Service 接口。
 */
public interface SysDictTypeService extends IService<SysDictType> {

    Page<SysDictType> pageDictTypes(Integer current, Integer size, String keyword, String status);

    SysDictType getDictTypeById(Long dictTypeId);

    boolean saveDictType(SysDictType dictType);

    boolean updateDictType(SysDictType dictType);

    boolean removeDictTypeById(Long dictTypeId);

    List<SysDictType> listDictTypes(String keyword, String status);
}
