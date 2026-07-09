package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.QmsInspectionItem;

import java.util.List;

/**
 * 检验项目管理 Service 接口。
 */
public interface QmsInspectionItemService extends IService<QmsInspectionItem> {

    /**
     * 分页查询检验项目列表，支持关键字搜索和类型筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字（匹配项目编码或项目名称）
     * @param itemType 项目类型
     * @return 分页结果
     */
    Page<QmsInspectionItem> pageItems(Integer current, Integer size, String keyword, String itemType);

    /**
     * 根据检验项目ID查询检验项目信息。
     * @param itemId 检验项目ID
     * @return 检验项目实体
     */
    QmsInspectionItem getItemById(Long itemId);

    /**
     * 新增检验项目。
     * @param item 检验项目实体
     * @return 是否成功
     */
    boolean saveItem(QmsInspectionItem item);

    /**
     * 更新检验项目信息。
     * @param item 检验项目实体
     * @return 是否成功
     */
    boolean updateItem(QmsInspectionItem item);

    /**
     * 根据ID删除检验项目（软删除）。
     * @param itemId 检验项目ID
     * @return 是否成功
     */
    boolean removeItemById(Long itemId);

    /**
     * 查询检验项目列表，支持关键字搜索和类型筛选。
     * @param keyword 关键字（匹配项目编码或项目名称）
     * @param itemType 项目类型
     * @return 检验项目列表
     */
    List<QmsInspectionItem> listItems(String keyword, String itemType);
}
