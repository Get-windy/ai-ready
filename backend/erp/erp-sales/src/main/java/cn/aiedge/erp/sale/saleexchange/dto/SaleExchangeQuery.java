package cn.aiedge.erp.sale.saleexchange.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 销售换货单列表查询条件。
 *
 * <p>字段与开发文档「页面配置 → 查询条件」20 项一一对应，供 /page 接口绑定。
 * 使用独立 DTO 而非 {@code Map<String,Object>}：@RequestParam 注入的 Map 值恒为 String，
 * 原实现 {@code (Integer) params.get("status")} 会抛 ClassCastException。</p>
 */
@Data
@Schema(description = "销售换货单查询条件")
public class SaleExchangeQuery {

    /** 日期-起（单据日期） */
    private String startDate;

    /** 日期-止（单据日期） */
    private String endDate;

    /** 单据编号 */
    private String exchangeNo;

    /** 客户 */
    private String customerName;

    /** 经手人 */
    private String handlerName;

    /** 部门 */
    private String deptName;

    /** 制单人 */
    private String creatorName;

    /** 记账人 */
    private String bookkeeperName;

    /** 入库（换入）仓库 */
    private String inWarehouseName;

    /** 出库（换出）仓库 */
    private String outWarehouseName;

    /** 单据状态：0草稿 1待审核 2已审核 4已完成 5已拒绝 6已取消 */
    private Integer status;

    /** 结算状态 */
    private String settleStatus;

    /** 销售类型 */
    private String salesType;

    /** 商品行属性（明细级条件，命中任一明细即返回该单据） */
    private String productLineAttr;

    /** 单据备注 */
    private String remark;

    /** 表头自定义字段1(数字) */
    private BigDecimal extNum1;

    /** 表头自定义字段2(数字) */
    private BigDecimal extNum2;

    /** 表头自定义字段3(文本) */
    private String extText1;

    /** 表头自定义字段4(文本) */
    private String extText2;

    /** 表头自定义字段5(文本) */
    private String extText3;

    /** 显示红冲：勾选后包含已取消单据 */
    private Boolean showRedFlush;

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页数量 */
    private Integer pageSize = 20;
}
