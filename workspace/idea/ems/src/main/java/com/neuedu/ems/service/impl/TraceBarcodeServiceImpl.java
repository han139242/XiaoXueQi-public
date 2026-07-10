package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.TraceBarcode;
import com.neuedu.ems.mapper.TraceBarcodeMapper;
import com.neuedu.ems.service.TraceBarcodeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 条码追溯 Service 实现类。
 */
@Service
public class TraceBarcodeServiceImpl extends ServiceImpl<TraceBarcodeMapper, TraceBarcode> implements TraceBarcodeService {

    private static final DateTimeFormatter BARCODE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /**
     * 分页查询条码，默认过滤已删除记录。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字，匹配条码值或生成规则
     * @param barcodeType 条码类型
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @param status 状态
     * @return 分页结果
     */
    @Override
    public Page<TraceBarcode> pageBarcodes(Integer current, Integer size, String keyword, String barcodeType,
                                           String bizType, Long bizId, String status) {
        Page<TraceBarcode> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildBarcodeQuery(keyword, barcodeType, bizType, bizId, status));
    }

    /**
     * 查询条码列表，默认过滤已删除记录。
     * @param keyword 关键字，匹配条码值或生成规则
     * @param barcodeType 条码类型
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @param status 状态
     * @return 条码列表
     */
    @Override
    public List<TraceBarcode> listBarcodes(String keyword, String barcodeType, String bizType, Long bizId, String status) {
        return baseMapper.selectList(buildBarcodeQuery(keyword, barcodeType, bizType, bizId, status));
    }

    /**
     * 根据条码ID查询未删除的条码详情。
     * @param barcodeId 条码ID
     * @return 条码实体
     */
    @Override
    public TraceBarcode getBarcodeById(Long barcodeId) {
        LambdaQueryWrapper<TraceBarcode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TraceBarcode::getBarcodeId, barcodeId)
                .eq(TraceBarcode::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 新增条码，自动设置删除标志为0。
     * @param barcode 条码实体
     * @return 是否成功
     */
    @Override
    public boolean saveBarcode(TraceBarcode barcode) {
        LocalDateTime now = LocalDateTime.now();
        if (barcode.getDeletedFlag() == null) {
            barcode.setDeletedFlag(0);
        }
        if (barcode.getCreatedTime() == null) {
            barcode.setCreatedTime(now);
        }
        barcode.setUpdatedTime(now);
        return baseMapper.insert(barcode) > 0;
    }

    /**
     * 更新条码，不允许通过普通更新修改删除标志。
     * @param barcode 条码实体
     * @return 是否成功
     */
    @Override
    public boolean updateBarcode(TraceBarcode barcode) {
        if (barcode.getBarcodeId() == null) {
            return false;
        }
        barcode.setUpdatedTime(LocalDateTime.now());
        barcode.setDeletedFlag(null);
        return baseMapper.updateById(barcode) > 0;
    }

    /**
     * 根据ID软删除条码。
     * @param barcodeId 条码ID
     * @return 是否成功
     */
    @Override
    public boolean removeBarcodeById(Long barcodeId) {
        TraceBarcode barcode = new TraceBarcode();
        barcode.setBarcodeId(barcodeId);
        barcode.setDeletedFlag(1);
        return baseMapper.updateById(barcode) > 0;
    }

    /**
     * 按生成规则批量生成条码。
     * @param barcodeType 条码类型
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @param quantity 生成数量
     * @param remark 备注
     * @return 新生成的条码列表
     */
    @Override
    @Transactional
    public List<TraceBarcode> generateBarcodes(String barcodeType, String bizType, Long bizId, Integer quantity, String remark) {
        List<TraceBarcode> barcodes = new ArrayList<>();
        if (!StringUtils.hasText(barcodeType) || !StringUtils.hasText(bizType) || bizId == null || quantity == null || quantity <= 0) {
            return barcodes;
        }
        String prefix = "BC" + LocalDateTime.now().format(BARCODE_FORMATTER);
        for (int i = 1; i <= quantity; i++) {
            LocalDateTime now = LocalDateTime.now();
            TraceBarcode barcode = new TraceBarcode();
            barcode.setBarcodeValue(prefix + String.format("%04d", i));
            barcode.setBarcodeType(barcodeType);
            barcode.setBizType(bizType);
            barcode.setBizId(bizId);
            barcode.setGeneratedRule("BC+yyyyMMddHHmmssSSS+SEQ");
            barcode.setStatus("启用");
            barcode.setRemark(remark);
            barcode.setCreatedTime(now);
            barcode.setUpdatedTime(now);
            barcode.setDeletedFlag(0);
            baseMapper.insert(barcode);
            barcodes.add(barcode);
        }
        return barcodes;
    }

    /**
     * 根据条码值查询条码详情。
     * @param barcodeValue 条码值
     * @return 条码实体
     */
    @Override
    public TraceBarcode scanBarcode(String barcodeValue) {
        LambdaQueryWrapper<TraceBarcode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TraceBarcode::getBarcodeValue, barcodeValue)
                .eq(TraceBarcode::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 构建条码查询条件。
     * @param keyword 关键字
     * @param barcodeType 条码类型
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @param status 状态
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<TraceBarcode> buildBarcodeQuery(String keyword, String barcodeType, String bizType,
                                                               Long bizId, String status) {
        LambdaQueryWrapper<TraceBarcode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TraceBarcode::getDeletedFlag, 0)
                .and(StringUtils.hasText(keyword), item -> item.like(TraceBarcode::getBarcodeValue, keyword)
                        .or().like(TraceBarcode::getGeneratedRule, keyword))
                .eq(StringUtils.hasText(barcodeType), TraceBarcode::getBarcodeType, barcodeType)
                .eq(StringUtils.hasText(bizType), TraceBarcode::getBizType, bizType)
                .eq(bizId != null, TraceBarcode::getBizId, bizId)
                .eq(StringUtils.hasText(status), TraceBarcode::getStatus, status)
                .orderByDesc(TraceBarcode::getBarcodeId);
        return queryWrapper;
    }
}
