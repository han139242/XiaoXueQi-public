package com.neuedu.ems.service.impl.eam;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.eam.EamSpotCheckRecord;
import com.neuedu.ems.mapper.eam.EamSpotCheckRecordMapper;
import com.neuedu.ems.service.eam.EamSpotCheckRecordService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 点检记录 Service 实现类。
 */
@Service
public class EamSpotCheckRecordServiceImpl extends ServiceImpl<EamSpotCheckRecordMapper, EamSpotCheckRecord>
        implements EamSpotCheckRecordService {

    /**
     * 分页查询点检记录，默认过滤已删除记录。
     * @param current 当前页码
     * @param size 每页记录数
     * @param planId 点检计划ID
     * @param equipmentId 设备ID
     * @param checkerId 点检人ID
     * @param checkResult 点检结果
     * @return 分页结果
     */
    @Override
    public Page<EamSpotCheckRecord> pageSpotCheckRecords(Integer current, Integer size, Long planId,
                                                         Long equipmentId, Long checkerId, String checkResult) {
        Page<EamSpotCheckRecord> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildSpotCheckRecordQuery(planId, equipmentId, checkerId, checkResult));
    }

    /**
     * 查询点检记录列表，默认过滤已删除记录。
     * @param planId 点检计划ID
     * @param equipmentId 设备ID
     * @param checkerId 点检人ID
     * @param checkResult 点检结果
     * @return 点检记录列表
     */
    @Override
    public List<EamSpotCheckRecord> listSpotCheckRecords(Long planId, Long equipmentId, Long checkerId, String checkResult) {
        return baseMapper.selectList(buildSpotCheckRecordQuery(planId, equipmentId, checkerId, checkResult));
    }

    /**
     * 根据点检记录ID查询未删除的记录详情。
     * @param recordId 点检记录ID
     * @return 点检记录实体
     */
    @Override
    public EamSpotCheckRecord getSpotCheckRecordById(Long recordId) {
        LambdaQueryWrapper<EamSpotCheckRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EamSpotCheckRecord::getRecordId, recordId)
                .eq(EamSpotCheckRecord::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 新增点检记录，自动设置删除标志为0。
     * @param spotCheckRecord 点检记录实体
     * @return 是否成功
     */
    @Override
    public boolean saveSpotCheckRecord(EamSpotCheckRecord spotCheckRecord) {
        if (spotCheckRecord.getDeletedFlag() == null) {
            spotCheckRecord.setDeletedFlag(0);
        }
        return baseMapper.insert(spotCheckRecord) > 0;
    }

    /**
     * 更新点检记录，不允许通过普通更新修改删除标志。
     * @param spotCheckRecord 点检记录实体
     * @return 是否成功
     */
    @Override
    public boolean updateSpotCheckRecord(EamSpotCheckRecord spotCheckRecord) {
        if (spotCheckRecord.getRecordId() == null) {
            return false;
        }
        spotCheckRecord.setDeletedFlag(null);
        return baseMapper.updateById(spotCheckRecord) > 0;
    }

    /**
     * 根据ID软删除点检记录。
     * @param recordId 点检记录ID
     * @return 是否成功
     */
    @Override
    public boolean removeSpotCheckRecordById(Long recordId) {
        EamSpotCheckRecord spotCheckRecord = new EamSpotCheckRecord();
        spotCheckRecord.setRecordId(recordId);
        spotCheckRecord.setDeletedFlag(1);
        return baseMapper.updateById(spotCheckRecord) > 0;
    }

    /**
     * 构建点检记录查询条件。
     * @param planId 点检计划ID
     * @param equipmentId 设备ID
     * @param checkerId 点检人ID
     * @param checkResult 点检结果
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<EamSpotCheckRecord> buildSpotCheckRecordQuery(Long planId, Long equipmentId,
                                                                             Long checkerId, String checkResult) {
        LambdaQueryWrapper<EamSpotCheckRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EamSpotCheckRecord::getDeletedFlag, 0)
                .eq(planId != null, EamSpotCheckRecord::getPlanId, planId)
                .eq(equipmentId != null, EamSpotCheckRecord::getEquipmentId, equipmentId)
                .eq(checkerId != null, EamSpotCheckRecord::getCheckerId, checkerId)
                .eq(StringUtils.hasText(checkResult), EamSpotCheckRecord::getCheckResult, checkResult)
                .orderByDesc(EamSpotCheckRecord::getCheckTime)
                .orderByDesc(EamSpotCheckRecord::getRecordId);
        return queryWrapper;
    }
}
