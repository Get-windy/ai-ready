package cn.aiedge.erp.stock.enums;

public enum StockCheckStatus {
    DRAFT(0, "草稿"),
    PENDING_APPROVAL(1, "待审批"),
    APPROVED(2, "已审批"),
    REJECTED(3, "已拒绝"),
    CHECKING(4, "盘点中"),
    IN_PROGRESS(5, "进行中"),
    COMPLETED(6, "已完成"),
    ADJUSTED(7, "已调整"),
    CANCELLED(8, "已取消");

    private final Integer code;
    private final String desc;

    StockCheckStatus(Integer code, String desc) {
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