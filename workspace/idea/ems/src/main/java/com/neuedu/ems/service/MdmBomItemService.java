package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.MdmBomItem;

import java.util.List;

/**
 * BOM 明细 Service 接口。
 */
public interface MdmBomItemService extends IService<MdmBomItem> {

    Page<MdmBomItem> pageBomItems(Integer current, Integer size, Long bomId, Long materialId);

    MdmBomItem getBomItemById(Long bomItemId);

    boolean saveBomItem(MdmBomItem item);

    boolean updateBomItem(MdmBomItem item);

    boolean removeBomItemById(Long bomItemId);

    List<MdmBomItem> listBomItems(Long bomId, Long materialId);
}
