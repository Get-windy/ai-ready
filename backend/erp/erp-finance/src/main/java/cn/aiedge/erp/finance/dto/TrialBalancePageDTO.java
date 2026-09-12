package cn.aiedge.erp.finance.dto;

import lombok.Data;

import java.util.List;

/**
 * 科目余额表结果（无分页：账簿汇总页一次性返回全部科目行 + 合计行）
 */
@Data
public class TrialBalancePageDTO {

    /** 科目行（按科目编码升序） */
    private List<TrialBalanceRowDTO> records;

    /** 行数 */
    private long total;

    /** 合计行（试算平衡校验） */
    private TrialBalanceSummaryDTO summary;
}
