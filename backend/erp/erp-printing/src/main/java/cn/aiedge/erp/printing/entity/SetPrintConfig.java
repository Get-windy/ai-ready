package cn.aiedge.erp.printing.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 打印设置（租户级配置，一行一租户）
 *
 * <p>落库表 {@code set_print_config}（迁移 V11.402.0）。页面：设置 → 打印管理 → 打印设置（菜单 80930）。</p>
 *
 * <p><b>字段逐字取自 ql361 实拍</b>（《设置模块/打印设置开发文档.md》§4.2，证据
 * {@code tool-results/ql361/设置-live/打印设置.json/.png}），不发明字段：
 * 允许打印草稿 / 属性商品汇总打印 / 批次效期商品汇总打印 / 打印内容 /
 * 单据打印小数位数（数量、单价各 N 位）/ 助手打印 / 远程打印。</p>
 *
 * <p>两条落库口径（与开发文档 §7.3 的差异，已在该文档与本类注释登记）：</p>
 * <ul>
 *   <li>布尔一律用 {@code Integer} 0/1（本库既有列口径；文档 §7.3 写的是 boolean）；</li>
 *   <li>{@code qtyDecimal} / {@code priceDecimal} 只用数字，不建枚举 ——
 *       ql361 的下拉为「整数 / 1位小数 … 8位小数」9 档，而本系统主数据精度是
 *       {@code numeric(18,4)}，故取值域收敛为 0~4（本系统适配口径，非对标值），
 *       0 即「整数」。取舍已登记在 {@code PrintConfigService.DECIMAL_MIN/MAX} 注释中。</li>
 * </ul>
 *
 * <p>⚠️ 本页只是**打印的配置入口**，不是打印引擎：模板渲染 / 本地打印 / 打印链执行 / 客户端注册
 * 一律复用既有 {@code erp-printing} 模块能力，本类不承载任何打印执行逻辑。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("set_print_config")
public class SetPrintConfig extends BaseEntity {

    /** 允许打印草稿（0 否 / 1 是，ql361 实拍 1） */
    private Integer allowDraftPrint;

    /** 属性商品汇总打印（0 否 / 1 是，ql361 实拍 1） */
    private Integer attrSummaryPrint;

    /** 批次效期商品汇总打印（0 否 / 1 是，ql361 实拍 0） */
    private Integer batchSummaryPrint;

    /**
     * 打印内容（ql361 实测完整选项集 3 项：`批号 *数量` / `生产日期 *数量` / `批号 生产日期~到期日期 *数量`；
     * 实拍选中值「批号 *数量」）。决定明细行「批次效期」文本的拼接口径。
     */
    private String printContent;

    /** 单据打印小数位数总开关（0 关 / 1 开，ql361 实拍 0） */
    private Integer decimalEnabled;

    /** 数量小数位（ql361 实拍 2） */
    private Integer qtyDecimal;

    /** 单价小数位（ql361 实拍 2） */
    private Integer priceDecimal;

    /** 助手打印：开启后跳过预览直接打印（0 关 / 1 开，ql361 实拍 开） */
    private Integer assistantEnabled;

    /** 远程打印（0 关 / 1 开，ql361 实拍 开） */
    private Integer remoteEnabled;
}
