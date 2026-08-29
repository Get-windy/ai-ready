package cn.aiedge.erp.purchase.purchasereturn.enums;

/**
 * 采购退货单状态
 * 0草稿 / 1待审批 / 2已审批 / 3已驳回 / 4已完成 / 5已取消
 */
public enum ReturnStatus {
    DRAFT(0, "草稿"),
    PENDING_APPROVAL(1, "待审批"),
    APPROVED(2, "已审批"),
    REJECTED(3, "已驳回"),
    COMPLETED(4, "已完成"),
    CANCELLED(5, "已取消");

    private final Integer code;
    private final String desc;

    ReturnStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
