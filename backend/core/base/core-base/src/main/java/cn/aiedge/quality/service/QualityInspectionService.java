package cn.aiedge.quality.service;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.quality.entity.QualityInspection;

import java.math.BigDecimal;
import java.util.List;

/**
 * 检验记录服务接口
 */
public interface QualityInspectionService {

    /**
     * 创建检验记录
     */
    QualityInspection create(QualityInspection inspection);

    /**
     * 完成检验
     * @param id 检验记录ID
     * @param result 检验结果 PASS/FAIL
     * @param passQuantity 合格数量
     * @param failQuantity 不合格数量
     */
    void complete(Long id, String result, BigDecimal passQuantity, BigDecimal failQuantity);

    /**
     * 分页查询检验记录
     */
    PageResult<QualityInspection> page(Integer pageNum, Integer pageSize, String bizType, String result);

    /**
     * 查询检验记录详情
     */
    QualityInspection get(Long id);

    /**
     * 查询待检记录
     */
    List<QualityInspection> listPending(String bizType);
}