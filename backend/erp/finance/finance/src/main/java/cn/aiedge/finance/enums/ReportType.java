package cn.aiedge.finance.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReportType {
    
    BALANCE_SHEET(1, "资产负债表"),
    INCOME_STATEMENT(2, "利润表"),
    CASH_FLOW_STATEMENT(3, "现金流量表"),
    EQUITY_CHANGE_STATEMENT(4, "所有者权益变动表"),
    PROFIT_DISTRIBUTION(5, "利润分配表"),
    COST_ANALYSIS(6, "成本分析表"),
    RECEIVABLE_AGING(7, "应收账款账龄分析表"),
    PAYABLE_AGING(8, "应付账款账龄分析表");
    
    private final Integer code;
    private final String name;
    
    public static ReportType fromCode(Integer code) {
        for (ReportType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}