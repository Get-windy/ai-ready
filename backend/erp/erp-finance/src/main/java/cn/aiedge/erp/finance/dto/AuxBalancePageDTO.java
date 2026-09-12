package cn.aiedge.erp.finance.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 辅助核算余额表分页结果（数据行 + 总条数 + 表尾合计）
 */
@Data
public class AuxBalancePageDTO {

    /** 当前页数据行 */
    private List<AuxBalanceRowDTO> records = new ArrayList<>();

    /** 满足条件的总行数 */
    private long total;

    /** 表尾合计（全量口径） */
    private AuxBalanceSummaryDTO summary = new AuxBalanceSummaryDTO();
}
