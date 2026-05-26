package cn.aiedge.crm.marketing.enums;

public enum CampaignStatus {
    DRAFT(0, "草稿"),
    PENDING_APPROVAL(1, "待审批"),
    APPROVED(2, "已审批"),
    SCHEDULED(3, "已排期"),
    RUNNING(4, "进行中"),
    PAUSED(5, "已暂停"),
    COMPLETED(6, "已完成"),
    CANCELLED(7, "已取消");

    private final Integer code;
    private final String desc;

    CampaignStatus(Integer code, String desc) {
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