package cn.aiedge.erp.printing.dto;

import lombok.Data;

/**
 * 打印设置的保存入参（部分更新语义）
 *
 * <p>刻意做成「**全部字段可空**」：null 表示本次不改该项（MyBatis-Plus 的 {@code updateById}
 * 本身忽略 null），因此前端只提交改动过的字段也能正确工作。</p>
 *
 * <p>⚠️ <b>刻意不含 {@code tenantId} / {@code id}</b>：租户由服务端从登录会话取（开发文档 §6.1-1
 * 「tenantId 服务端取，不由客户端传」），客户端伪造 tenantId 在契约上不可能 ——
 * 这样跨租户覆盖在入参层面就被排除，而不是靠校验兜底。</p>
 *
 * <p>布尔一律 {@link Boolean}（落库转 0/1），与库列 {@code INTEGER} 口径对应。</p>
 */
@Data
public class PrintConfigRequest {

    /** 允许打印草稿 */
    private Boolean allowDraftPrint;

    /** 属性商品汇总打印 */
    private Boolean attrSummaryPrint;

    /** 批次效期商品汇总打印 */
    private Boolean batchSummaryPrint;

    /** 打印内容（如「批号 *数量」） */
    private String printContent;

    /** 单据打印小数位数总开关 */
    private Boolean decimalEnabled;

    /** 数量小数位 */
    private Integer qtyDecimal;

    /** 单价小数位 */
    private Integer priceDecimal;

    /** 助手打印（跳过预览直接打印） */
    private Boolean assistantEnabled;

    /** 远程打印 */
    private Boolean remoteEnabled;
}
