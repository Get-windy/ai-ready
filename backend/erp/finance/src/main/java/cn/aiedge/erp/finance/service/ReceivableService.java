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
