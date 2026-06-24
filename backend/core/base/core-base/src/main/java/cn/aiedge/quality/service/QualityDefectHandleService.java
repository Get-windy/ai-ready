package cn.aiedge.quality.service;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.quality.entity.QualityDefectHandle;

import java.math.BigDecimal;
import java.util.List;

/**
 * 不合格处理服务接口
 */
public interface QualityDefectHandleService {

    /**
     * 创建不合格处理记录
     */
    QualityDefectHandle create(Long inspectionId, String defectType, String defectDesc, BigDecimal defectQuantity);

    /**
     * 处理不合格
     * @param id 处理记录ID
     * @param handleType 处理方式
     * @param handleQuantity 处理数量
     * @param handleResult 处理结果
     */
    void handle(Long id, String handleType, BigDecimal handleQuantity, String handleResult);

    /**
     * 分页查询不合格处理记录
     */
    PageResult<QualityDefectHandle> page(Integer pageNum, Integer pageSize, Integer status);

    /**
     * 查询处理记录详情
     */
    QualityDefectHandle get(Long id);

    /**
     * 查询待处理记录
     */
    List<QualityDefectHandle> listPending();
}