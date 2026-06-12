package cn.aiedge.wms.enums;

/**
 * 事件状态
 */
public interface EventStatus {
    int PENDING = 0;       // 待发送
    int SUCCESS = 1;       // 发送成功
    int FAILED = 2;        // 发送失败
}
