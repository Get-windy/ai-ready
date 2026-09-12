package cn.aiedge.erp.finance.service;

import cn.aiedge.erp.finance.dto.MonthClosingResultDTO;
import cn.aiedge.erp.finance.model.entity.MonthClosingLog;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

/**
 * 月结Service接口
 */
public interface MonthClosingService {

    /**
     * 执行月结：校验该期间凭证状态，全部通过则关闭期间并记录日志
     *
     * @param periodCode   期间编码 yyyy-MM
     * @param operatorId   操作人ID
     * @param operatorName 操作人姓名
     * @return 检查结果（success=false 时期间未关闭，checks 含未通过明细）
     */
    MonthClosingResultDTO execute(String periodCode, String operatorId, String operatorName);

    /**
     * 反月结：重新开启已关闭期间并记录日志
     */
    MonthClosingResultDTO reopen(String periodCode, String operatorId, String operatorName);

    /**
     * 批量执行月结：逐期间执行，收集结果（单个异常不阻断其余期间）
     */
    List<MonthClosingResultDTO> batchExecute(List<String> periodCodes, String operatorId, String operatorName);

    /**
     * 查询期间状态及最近一次月结日志
     */
    Map<String, Object> status(String periodCode);

    /**
     * 分页查询月结日志
     */
    IPage<MonthClosingLog> logPage(String periodCode, Page<MonthClosingLog> page);
}
