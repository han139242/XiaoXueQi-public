package com.neuedu.ems.service.impl.qms;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.qms.QmsInspectionItem;
import com.neuedu.ems.mapper.qms.QmsInspectionItemMapper;
import com.neuedu.ems.service.qms.QmsInspectionItemService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 检验项目管理 Service 实现类。
 */
@Service
public class QmsInspectionItemServiceImpl extends ServiceImpl<QmsInspectionItemMapper, QmsInspectionItem> implements QmsInspectionItemService {

    /**
     * 分页查询检验项目列表，支持关键字搜索和类型筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字（匹配项目编码或项目名称）
     * @param itemType 项目类型
     * @return 分页结果
     */
    @Override
    public Page<QmsInspectionItem> pageItems(Integer current, Integer size, String keyword, String itemType) {
        Page<QmsInspectionItem> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildItemQuery(keyword, itemType));
    }

    /**
     * 根据检验项目ID查询检验项目信息。
     * @param itemId 检验项目ID
     * @return 检验项目实体
     */
    @Override
    public QmsInspectionItem getItemById(Long itemId) {
        QueryWrapper<QmsInspectionItem> wrapper = new QueryWrapper<>();
        wrapper.eq("item_id", itemId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    /**
     * 新增检验项目，自动设置删除标志为0。
     * @param item 检验项目实体
     * @return 是否成功
     */
    @Override
    public boolean saveItem(QmsInspectionItem item) {
        if (item.getDeletedFlag() == null) {
            item.setDeletedFlag(0);
        }
        return baseMapper.insert(item) > 0;
    }

    /**
     * 更新检验项目信息，不允许修改删除标志。
     * @param item 检验项目实体
     * @return 是否成功
     */
    @Override
    public boolean updateItem(QmsInspectionItem item) {
        if (item.getItemId() == null) {
            return false;
        }
        item.setDeletedFlag(null);
        return baseMapper.updateById(item) > 0;
    }

    /**
     * 根据ID删除检验项目（软删除，将删除标志设为1）。
     * @param itemId 检验项目ID
     * @return 是否成功
     */
    @Override
    public boolean removeItemById(Long itemId) {
        QmsInspectionItem item = new QmsInspectionItem();
        item.setItemId(itemId);
        item.setDeletedFlag(1);
        return baseMapper.updateById(item) > 0;
    }

    /**
     * 查询检验项目列表，支持关键字搜索和类型筛选。
     * @param keyword 关键字（匹配项目编码或项目名称）
     * @param itemType 项目类型
     * @return 检验项目列表
     */
    @Override
    public List<QmsInspectionItem> listItems(String keyword, String itemType) {
        return baseMapper.selectList(buildItemQuery(keyword, itemType));
    }

    /**
     * 构建检验项目查询条件，过滤已删除记录。
     * @param keyword 关键字
     * @param itemType 项目类型
     * @return 查询条件包装器
     */
    private QueryWrapper<QmsInspectionItem> buildItemQuery(String keyword, String itemType) {
        QueryWrapper<QmsInspectionItem> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like("item_code", keyword)
                    .or().like("item_name", keyword));
        }
        if (StringUtils.hasText(itemType)) {
            wrapper.eq("item_type", itemType);
        }
        wrapper.orderByDesc("item_id");
        return wrapper;
    }
}
