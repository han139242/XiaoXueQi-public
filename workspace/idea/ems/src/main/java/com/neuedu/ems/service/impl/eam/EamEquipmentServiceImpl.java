package com.neuedu.ems.service.impl.eam;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.eam.EamEquipment;
import com.neuedu.ems.mapper.eam.EamEquipmentMapper;
import com.neuedu.ems.service.eam.EamEquipmentService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 设备管理 Service 实现类。
 */
@Service
public class EamEquipmentServiceImpl extends ServiceImpl<EamEquipmentMapper, EamEquipment> implements EamEquipmentService {

    /**
     * 分页查询设备列表，支持关键字搜索、状态筛选和分类筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字（匹配设备编码或设备名称）
     * @param status 设备状态
     * @param categoryId 设备分类ID
     * @return 分页结果
     */
    @Override
    public Page<EamEquipment> pageEquipments(Integer current, Integer size, String keyword, String status, Long categoryId) {
        Page<EamEquipment> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildEquipmentQuery(keyword, status, categoryId));
    }

    /**
     * 根据设备ID查询设备信息。
     * @param equipmentId 设备ID
     * @return 设备实体
     */
    @Override
    public EamEquipment getEquipmentById(Long equipmentId) {
        QueryWrapper<EamEquipment> wrapper = new QueryWrapper<>();
        wrapper.eq("equipment_id", equipmentId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    /**
     * 新增设备，自动设置删除标志为0。
     * @param equipment 设备实体
     * @return 是否成功
     */
    @Override
    public boolean saveEquipment(EamEquipment equipment) {
        if (equipment.getDeletedFlag() == null) {
            equipment.setDeletedFlag(0);
        }
        return baseMapper.insert(equipment) > 0;
    }

    /**
     * 更新设备信息，不允许修改删除标志。
     * @param equipment 设备实体
     * @return 是否成功
     */
    @Override
    public boolean updateEquipment(EamEquipment equipment) {
        if (equipment.getEquipmentId() == null) {
            return false;
        }
        equipment.setDeletedFlag(null);
        return baseMapper.updateById(equipment) > 0;
    }

    /**
     * 根据ID删除设备（软删除，将删除标志设为1）。
     * @param equipmentId 设备ID
     * @return 是否成功
     */
    @Override
    public boolean removeEquipmentById(Long equipmentId) {
        EamEquipment equipment = new EamEquipment();
        equipment.setEquipmentId(equipmentId);
        equipment.setDeletedFlag(1);
        return baseMapper.updateById(equipment) > 0;
    }

    /**
     * 查询设备列表，支持关键字搜索、状态筛选和分类筛选。
     * @param keyword 关键字（匹配设备编码或设备名称）
     * @param status 设备状态
     * @param categoryId 设备分类ID
     * @return 设备列表
     */
    @Override
    public List<EamEquipment> listEquipments(String keyword, String status, Long categoryId) {
        return baseMapper.selectList(buildEquipmentQuery(keyword, status, categoryId));
    }

    /**
     * 构建设备查询条件，过滤已删除记录。
     * @param keyword 关键字
     * @param status 状态
     * @param categoryId 设备分类ID
     * @return 查询条件包装器
     */
    private QueryWrapper<EamEquipment> buildEquipmentQuery(String keyword, String status, Long categoryId) {
        QueryWrapper<EamEquipment> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like("equipment_code", keyword)
                    .or().like("equipment_name", keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        if (categoryId != null) {
            wrapper.eq("category_id", categoryId);
        }
        wrapper.orderByDesc("equipment_id");
        return wrapper;
    }
}
