package com.neuedu.ems.service.factory;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.factory.FactoryLine;

import java.util.List;

/**
 * 产线 Service 接口。
 */
public interface FactoryLineService extends IService<FactoryLine> {

    Page<FactoryLine> pageLines(Integer current, Integer size, String keyword, Long workshopId, String status);

    FactoryLine getLineById(Long lineId);

    boolean saveLine(FactoryLine line);

    boolean updateLine(FactoryLine line);

    boolean removeLineById(Long lineId);

    List<FactoryLine> listLines(String keyword, Long workshopId, String status);
}
