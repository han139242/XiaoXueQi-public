package com.neuedu.ems.service.impl.eam;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.eam.EamMaintenanceRecord;
import com.neuedu.ems.mapper.eam.EamMaintenanceRecordMapper;
import com.neuedu.ems.service.eam.EamMaintenanceRecordService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 保养记录 Service 实现类。
 */
@Service
public class EamMaintenanceRecordServiceImpl extends ServiceImpl<EamMaintenanceRecordMapper, EamMaintenanceRecord>
        implements EamMaintenanceRecordService {

    /**
     * 分页查询保养记录，默认过滤已删除记录。
     * @param current 当前页码
     * @param size 每页记录数
     * @param planId 保养计划ID
     * @param equipmentId 设备ID
     * @param maintainerId 保养人ID
     * @return 分页结果
     */
    @Override
    public Page<EamMaintenanceRecord> pageMaintenanceRecords(Integer current, Integer size, Long planId,
                                                             Long equipmentId, Long maintainerId) {
        Page<EamMaintenanceRecord> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildMaintenanceRecordQuery(planId, equipmentId, maintainerId));
    }

    /**
     * 查询保养记录列表，默认过滤已删除记录。
     * @param planId 保养计划ID
     * @param equipmentId 设备ID
     * @param maintainerId 保养人ID
     * @return 保养记录列表
     */
    @Override
    public List<EamMaintenanceRecord> listMaintenanceRecords(Long planId, Long equipmentId, Long maintainerId) {
        return baseMapper.selectList(buildMaintenanceRecordQuery(planId, equipmentId, maintainerId));
    }

    /**
     * 根据保养记录ID查询未删除的记录详情。
     * @param recordId 保养记录ID
     * @return 保养记录实体
     */
    @Override
    public EamMaintenanceRecord getMaintenanceRecordById(Long recordId) {
        LambdaQueryWrapper<EamMaintenanceRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EamMaintenanceRecord::getRecordId, recordId)
                .eq(EamMaintenanceRecord::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 新增保养记录，自动设置删除标志为0。
     * @param maintenanceRecord 保养记录实体
     * @return 是否成功
     */
    @Override
    public boolean saveMaintenanceRecord(EamMaintenanceRecord maintenanceRecord) {
        if (maintenanceRecord.getDeletedFlag() == null) {
            maintenanceRecord.setDeletedFlag(0);
        }
        return baseMapper.insert(maintenanceRecord) > 0;
    }

    /**
     * 更新保养记录，不允许通过普通更新修改删除标志。
     * @param maintenanceRecord 保养记录实体
     * @return 是否成功
     */
    @Override
    public boolean updateMaintenanceRecord(EamMaintenanceRecord maintenanceRecord) {
        if (maintenanceRecord.getRecordId() == null) {
            return false;
        }
        maintenanceRecord.setDeletedFlag(null);
        return baseMapper.updateById(maintenanceRecord) > 0;
    }

    /**
     * 根据ID软删除保养记录。
     * @param recordId 保养记录ID
     * @return 是否成功
     */
    @Override
    public boolean removeMaintenanceRecordById(Long recordId) {
        EamMaintenanceRecord maintenanceRecord = new EamMaintenanceRecord();
        maintenanceRecord.setRecordId(recordId);
        maintenanceRecord.setDeletedFlag(1);
        return baseMapper.updateById(maintenanceRecord) > 0;
    }

    /**
     * 构建保养记录查询条件。
     * @param planId 保养计划ID
     * @param equipmentId 设备ID
     * @param maintainerId 保养人ID
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<EamMaintenanceRecord> buildMaintenanceRecordQuery(Long planId, Long equipmentId,
                                                                                 Long maintainerId) {
        LambdaQueryWrapper<EamMaintenanceRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EamMaintenanceRecord::getDeletedFlag, 0)
                .eq(planId != null, EamMaintenanceRecord::getPlanId, planId)
                .eq(equipmentId != null, EamMaintenanceRecord::getEquipmentId, equipmentId)
                .eq(maintainerId != null, EamMaintenanceRecord::getMaintainerId, maintainerId)
                .orderByDesc(EamMaintenanceRecord::getMaintenanceTime)
                .orderByDesc(EamMaintenanceRecord::getRecordId);
        return queryWrapper;
    }
}
