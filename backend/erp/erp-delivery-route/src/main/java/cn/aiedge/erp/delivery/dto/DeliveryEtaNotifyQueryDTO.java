package cn.aiedge.erp.delivery.dto;

import lombok.Data;

import java.io.Serializable;

/** ETA 通知台账查询参数 */
@Data
public class DeliveryEtaNotifyQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 按路线单过滤（可空 = 全部） */
    private Long routeId;

    /** 状态过滤：PENDING / SENT / FAILED / CANCELLED（可空 = 全部） */
    private String status;

    /** 关键字：路线编号 / 客户名 / 手机号 */
    private String keyword;

    private Integer pageNum;

    private Integer pageSize;
}
