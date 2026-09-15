package cn.aiedge.erp.delivery.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 配送 ETA（预估到达时间）结果 */
@Data
public class RouteEtaVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long routeId;

    private String routeCode;

    /** 计算基准时间 */
    private LocalDateTime baseTime;

    /** 剩余待配送点位数 */
    private int remainingPoints;

    /** 使用的服务商（local 表示降级直线估算） */
    private String provider;

    /** true = 未配置地图 Key，按直线距离 + 平均时速降级估算 */
    private boolean degraded;

    /** 附加说明（如「N 个点位未维护坐标，未参与估算」） */
    private String message;

    private List<Item> points = new ArrayList<>();

    @Data
    public static class Item implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long pointId;

        private Integer pointOrder;

        private String customerName;

        private String customerPhone;

        private String address;

        private String status;

        /** 预估到达时间 */
        private LocalDateTime etaTime;

        /** 距上一点距离（米） */
        private Double distanceFromPrev;

        /** 距上一点时长（秒） */
        private Integer durationFromPrev;
    }
}
