package cn.aiedge.erp.finance.initial.vo;

import lombok.Data;

/**
 * 指定年度的会计期间开启情况（{@code GET /api/erp/finance/initial/period-status}）——
 * 财务期初与《会计期间》的关账联动判定结果。
 *
 * <p><b>关账保护口径（本实现裁定，已写进 InitialFinanceServiceImpl 注释与开发文档）</b>：
 * 「只要该年度**存在任一开启期间**即允许录入/修改期初」；只有当该年度的会计期间
 * **全部已关闭**（{@code status = 0} 的行数 &gt; 0 且开启行数 = 0）时才禁止。
 * 该年度**一条期间都没有**时（例如临时试录 2099 年）不算「全部关闭」，仍允许 ——
 * 保持与「回退链」一致的宽松口径。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class PeriodStatusVO {

    /** 判定的期初年度 */
    private Integer periodYear;

    /** 该年度开启中的期间数（status = 1） */
    private int openCount;

    /** 该年度已关闭的期间数（status = 0） */
    private int closedCount;

    /** 是否允许新增/修改/删除该年度的期初（前端据此置灰按钮） */
    private boolean editable;

    /** 禁止原因（editable = false 时有值，后端与前端展示同一文案） */
    private String reason;
}
