package cn.aiedge.crm.customer.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 跟进记录分页查询条件。
 *
 * <p>相比早期的 5 个参数版本，补齐了「按方式/结果筛选」与「按日期区间筛选」——
 * 后者是《客户跟进开发文档》§6.3 点名的最大能力缺口：
 * 用既有的 {@code next_follow_up_date} 列即可回答「今天/本周要跟进谁」，无需新增字段。
 */
@Data
public class CustomerFollowUpQuery {

    /** 客户ID */
    private Long customerId;

    /** 商机ID */
    private Long opportunityId;

    /** 线索ID */
    private Long leadId;

    /** 销售人员ID */
    private Long salesPersonId;

    /** 跟进方式（1 电话 / 2 拜访 / 3 邮件 / 4 微信 / 5 其他） */
    private Integer followUpType;

    /** 跟进结果（1 有意向 / 2 无意向 / 3 待跟进） */
    private Integer followUpResult;

    /** 跟进日期起（含） */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate followUpDateStart;

    /** 跟进日期止（含） */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate followUpDateEnd;

    /** 下次跟进日期起（含）——「待办跟进」口径 */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate nextFollowUpDateStart;

    /** 下次跟进日期止（含） */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate nextFollowUpDateEnd;

    /** 关键词：跟进单号 / 客户名 / 内容 模糊匹配 */
    private String keyword;

    private Integer pageNum = 1;

    private Integer pageSize = 20;
}
