package cn.aiedge.wms.enums;

/**
 * 库存异动类型
 */
public interface InventoryChangeType {
    int INBOUND = 1;       // 入库
    int OUTBOUND = 2;      // 出库
    int FREEZE = 3;        // 冻结
    int UNFREEZE = 4;      // 解冻
    int SURPLUS = 5;       // 盘盈
    int LOSS = 6;          // 盘亏
    int MOVE = 7;          // 移库
    int ADJUST = 8;        // 调整
}
