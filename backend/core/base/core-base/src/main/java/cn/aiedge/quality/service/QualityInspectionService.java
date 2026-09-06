package cn.aiedge.quality.service;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.quality.dto.QualityInspectionQuery;
import cn.aiedge.quality.entity.QualityInspection;

import java.math.BigDecimal;
import java.util.List;

/**
 * 检验记录服务接口
 */
public interface QualityInspectionService {

    /**
     * 创建检验记录（生成质检单号，默认状态待检）
     */
    QualityInspection create(QualityInspection inspection);

    /**
     * 更新检验记录（仅待检状态可编辑，保留单号与状态）
     */
    QualityInspection updateInspection(Long id, QualityInspection inspection);

    /**
     * 生成质检单号
     */
    String generateNo();

    /**
     * 完成检验
     * @param id 检验记录ID
     * @param result 检验结果 PASS/CONCESSION/FAIL
     * @param passQuantity 合格数量
     * @param failQuantity 不合格数量
     * @param remark 检验备注（可空）
     */
    void complete(Long id, String result, BigDecimal passQuantity, BigDecimal failQuantity, String remark);

    /**
     * 作废质检单
     */
    QualityInspection cancel(Long id);

    /**
     * 删除质检单（仅待检状态可删除）
     */
    void deleteInspection(Long id);

    /**
     * 批量删除质检单（仅待检状态可删除）
     */
    void batchDeleteInspection(List<Long> ids);

    /**
     * 多条件分页查询检验记录
     */
    PageResult<QualityInspection> page(QualityInspectionQuery query);

    /**
     * 查询检验记录详情
     */
    QualityInspection get(Long id);

    /**
     * 判断某来源单据+产品是否已质检放行（inspectionResult ∈ PASS/CONCESSION 且未作废）
     * 用于"未检不入库"门禁。
     */
    boolean hasPassed(String bizType, String bizNo, Long productId);

    /**
     * 查询待检记录
     */
    List<QualityInspection> listPending(String bizType);
}
