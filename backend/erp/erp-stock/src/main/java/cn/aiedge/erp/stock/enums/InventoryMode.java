package cn.aiedge.erp.stock.enums;

/**
 * 库存管理模式
 * <p>
 * BATCH  - 批次管理（生产日期、有效期）
 * SERIAL - 序列号管理（一物一码）
 * SKU    - SKU管理（简单库存单位）
 */
public enum InventoryMode {

    BATCH,
    SERIAL,
    SKU;

    public static InventoryMode fromString(String value) {
        if (value == null) return BATCH;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BATCH;
        }
    }
}
