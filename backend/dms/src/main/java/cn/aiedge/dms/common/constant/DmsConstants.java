package cn.aiedge.dms.common.constant;

/**
 * DMS 系统常量定义
 */
public interface DmsConstants {
    /** API 基础路径 */
    String API_PREFIX = "/api/dms";
    /** 默认租户ID */
    Long DEFAULT_TENANT_ID = 0L;
    /** 地图服务默认提供商 */
    String DEFAULT_MAP_PROVIDER = "amap";
    /** 默认上班时间 */
    String DEFAULT_WORK_HOURS_START = "08:00";
    /** 默认下班时间 */
    String DEFAULT_WORK_HOURS_END = "18:00";
    // 配送员状态
    int RIDER_STATUS_OFFLINE = 0;
    int RIDER_STATUS_IDLE = 1;
    int RIDER_STATUS_BUSY = 2;
    int RIDER_STATUS_REST = 3;
    // 车辆状态
    int VEHICLE_STATUS_IDLE = 0;
    int VEHICLE_STATUS_IN_USE = 1;
    int VEHICLE_STATUS_MAINTENANCE = 2;
    int VEHICLE_STATUS_SCRAPPED = 3;
    // 绑定状态
    int BINDING_STATUS_ACTIVE = 0;
    int BINDING_STATUS_HANDED_OVER = 1;
    int BINDING_STATUS_ABNORMAL = 2;
    // 任务状态
    int TASK_PENDING = 0;
    int TASK_ASSIGNED = 1;
    int TASK_ACCEPTED = 2;
    int TASK_PICKING_UP = 3;
    int TASK_DELIVERING = 4;
    int TASK_SIGNED = 5;
    int TASK_COMPLETED = 6;
    int TASK_CANCELLED = 7;
    int TASK_EXCEPTION = 8;
}
