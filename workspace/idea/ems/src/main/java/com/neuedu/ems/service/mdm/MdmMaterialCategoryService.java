package com.neuedu.ems.service.mdm;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.mdm.MdmMaterialCategory;

import java.util.List;

/**
 * 物料分类 Service 接口。
 */
public interface MdmMaterialCategoryService extends IService<MdmMaterialCategory> {

    Page<MdmMaterialCategory> pageCategories(Integer current, Integer size, String keyword, String status);

    MdmMaterialCategory getCategoryById(Long categoryId);

    boolean saveCategory(MdmMaterialCategory category);

    boolean updateCategory(MdmMaterialCategory category);

    boolean removeCategoryById(Long categoryId);

    List<MdmMaterialCategory> listCategories(String keyword, String status);

    List<MdmMaterialCategory> treeCategories(String status);
}
