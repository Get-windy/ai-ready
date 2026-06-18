package cn.aiedge.erp.product.kit.enums;

public enum AssemblyStatus {
    DRAFT(0, "草稿"),
    PENDING_APPROVAL(1, "待审批"),
    APPROVED(2, "已审批"),
    REJECTED(3, "已拒绝"),
    PENDING_EXECUTE(4, "待执行"),
    EXECUTING(5, "执行中"),
    COMPLETED(6, "已完成"),
    CANCELLED(7, "已取消");

    private final Integer code;
    private final String desc;

    AssemblyStatus(Integer code, String desc) {
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