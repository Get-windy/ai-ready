package cn.aiedge.erp.party.mapper;

import lombok.Data;

import java.time.LocalDate;

/**
 * Party 分页/搜索查询参数
 */
@Data
public class PartyQueryParam {
    private String keyword;
    private Integer partyType;
    private Integer status;
    private Long categoryId;
    private String settleType;
    private String region;
    private String handler;
    private String address;
    private LocalDate createTimeStart;
    private LocalDate createTimeEnd;
    private LocalDate lastTradeStart;
    private LocalDate lastTradeEnd;
    private Integer pageSize;
}
