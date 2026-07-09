package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.SysDictData;

import java.util.List;

/**
 * 字典数据 Service 接口。
 */
public interface SysDictDataService extends IService<SysDictData> {

    Page<SysDictData> pageDictData(Integer current, Integer size, String keyword, String dictTypeCode);

    List<SysDictData> listDictDataByTypeCode(String dictTypeCode);

    SysDictData getDictDataById(Long dictDataId);

    boolean saveDictData(SysDictData dictData);

    boolean updateDictData(SysDictData dictData);

    boolean removeDictDataById(Long dictDataId);

    List<SysDictData> listDictData(String keyword, String dictTypeCode);
}
