package cn.aiedge.erp.finance.service;

import cn.aiedge.erp.finance.dto.ReceivableDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 应收账款Service接口
 */
public interface ReceivableService {

    /**
     * 创建应收账款
     */
    ReceivableDTO create(ReceivableDTO dto);

    /**
     * 根据ID获取应收账款
     */
    ReceivableDTO getById(Long id);

    /**
     * 按来源单据判断是否已存在应收（业财集成防重复记账）
     */
    boolean existsBySource(String sourceType, Long sourceId);

    /**
     * 按来源单据<b>作废</b>应收 —— 业务单据被取消时调用。
     *
     * <p>与 {@link #writeOff} 的区别是语义：核销表示"钱收到了"，作废表示"这笔业务从未发生"
     * （单据取消/红冲）。取消的单据若把应收留在账上，客户对账单会凭空多出一笔欠款，
     * 且账龄分析会持续累计。</p>
     *
     * @param sourceType 业务来源类型（如 {@code SALE_SHIPMENT}）
     * @param sourceId   业务单据 ID
     * @param reason     作废原因（写入 remark 便于事后追溯）
     * @return 实际作废的条数（0 表示该来源本就没有应收，属正常情况，调用方不必当失败）
     */
    int cancelBySource(String sourceType, Long sourceId, String reason);

    /**
     * 分页查询应收账款
     */
    IPage<ReceivableDTO> list(String customerId, String status, Page<ReceivableDTO> page);

    /**
     * 获取账龄分析
     */
    List<Map<String, Object>> getAgingAnalysis();

    /**
     * 核销（部分/全额核销）
     */
    ReceivableDTO writeOff(Long id, BigDecimal amount);

    /**
     * 标记为坏账
     */
    ReceivableDTO markBadDebt(Long id);

    /**
     * 批量删除应收账款
     */
    void deleteBatch(List<Long> ids);

    /**
     * 导出应收账款列表
     */
    List<ReceivableDTO> exportList(String customerId, String status);
}
