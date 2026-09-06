package cn.aiedge.quality.service;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.quality.entity.QualityDefectHandle;
import cn.aiedge.quality.entity.QualityDefectHandleHistory;

import java.math.BigDecimal;
import java.util.List;

/**
 * 不合格处理服务接口
 */
public interface QualityDefectHandleService {

    /**
     * 创建不合格处理记录
     */
    QualityDefectHandle create(Long inspectionId, String defectType, String defectDesc, BigDecimal defectQuantity, String defectLevel);

    /**
     * 处理不合格（含 8D/CAPA 纠正/预防措施，校验多步处置累计不超过缺陷数量）
     * @param id 处理记录ID
     * @param handleType 处理方式
     * @param handleQuantity 处理数量
     * @param handleResult 处理结果
     * @param correctiveAction 纠正措施(CAPA，可空)
     * @param preventiveAction 预防措施(CAPA，可空)
     */
    void handle(Long id, String handleType, BigDecimal handleQuantity, String handleResult,
                String correctiveAction, String preventiveAction);

    /**
     * 分页查询不合格处理记录
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param inspectionId 检验记录ID（可空）
     * @param defectType 缺陷类型（可空）
     * @param defectLevel 缺陷等级 S/Ma/Mi（可空）
     * @param status 状态 0待处理 1已处理（可空）
     * @param bizNo 来源单号（模糊，可空）
     * @param handlerName 处理人（模糊，可空）
     * @param createTimeStart 创建日期起 YYYY-MM-DD（可空）
     * @param createTimeEnd 创建日期止 YYYY-MM-DD（可空）
     */
    PageResult<QualityDefectHandle> page(Integer pageNum, Integer pageSize, Long inspectionId, String defectType, String defectLevel,
                                         Integer status, String bizNo, String handlerName,
                                         String createTimeStart, String createTimeEnd);

    /**
     * 查询处理记录详情
     */
    QualityDefectHandle get(Long id);

    /**
     * 查询待处理记录
     */
    List<QualityDefectHandle> listPending();

    /**
     * 查询缺陷处理历史（按缺陷记录ID，倒序）
     */
    List<QualityDefectHandleHistory> listHistory(Long defectId);

    /**
     * 回填处置生成的下游单号（采购退货单号/报损单号，由联动编排调用）
     */
    void linkDownstream(Long defectId, String returnNo, String damageNo);
}