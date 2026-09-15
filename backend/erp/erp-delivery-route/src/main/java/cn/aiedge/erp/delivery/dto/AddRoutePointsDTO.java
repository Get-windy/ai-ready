package cn.aiedge.erp.delivery.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 手动添加配送点位（**不受围栏限制**，用于把围栏外订单补进路线单）
 *
 * 与「围栏自动归集」互补：自动归集负责常规批量，本接口负责人工指定。
 */
@Data
public class AddRoutePointsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 追加到路线单末尾（true）还是按地图能力重排（false，默认 false） */
    private Boolean replan;

    private List<Item> items;

    @Data
    public static class Item implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 来源类型：SO-销售订单 / OUT-销售出库单 */
        private String sourceType;

        private Long sourceId;

        /** 可选：覆盖地址 / 接收人（不传则取单据快照） */
        private String address;

        private String receiverName;

        private String receiverPhone;
    }
}
