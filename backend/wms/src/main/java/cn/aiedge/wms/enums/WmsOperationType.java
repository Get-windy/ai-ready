package cn.aiedge.wms.enums;

/**
 * WMS 作业类型枚举
 */
public interface WmsOperationType {
    /** 入库 */
    int RECEIPT = 1;
    /** 上架 */
    int PUTAWAY = 2;
    /** 拣货 */
    int PICK = 3;
    /** 发货 */
    int SHIP = 4;
    /** 移库 */
    int MOVE = 5;
    /** 盘点 */
    int CHECK = 6;
    /** 冻结 */
    int FREEZE = 7;
    /** 解冻 */
    int UNFREEZE = 8;
    /** 调整 */
    int ADJUST = 9;
}
