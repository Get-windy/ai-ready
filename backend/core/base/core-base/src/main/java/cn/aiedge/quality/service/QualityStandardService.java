package cn.aiedge.quality.service;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.quality.entity.QualityStandard;

import java.util.List;

/**
 * 质检标准服务接口
 */
public interface QualityStandardService {

    /**
     * 创建质检标准
     */
    QualityStandard create(QualityStandard standard);

    /**
     * 更新质检标准
     */
    QualityStandard update(Long id, QualityStandard standard);

    /**
     * 删除质检标准
     */
    void delete(Long id);

    /**
     * 分页查询质检标准
     */
    PageResult<QualityStandard> page(Integer pageNum, Integer pageSize, String standardCode, String standardName, String inspectionType, Integer status);

    /**
     * 查询质检标准详情
     */
    QualityStandard get(Long id);

    /**
     * 根据检验类型查询标准列表
     */
    List<QualityStandard> listByType(String inspectionType);

    /**
     * 生成标准编码（QSTD-yyyyMMdd-UUID6）
     */
    String generateNo();
}