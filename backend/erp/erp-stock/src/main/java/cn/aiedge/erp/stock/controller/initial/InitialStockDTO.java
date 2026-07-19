package cn.aiedge.erp.stock.controller.initial;

import java.math.BigDecimal;

/**
 * 期初库存数据传输对象
 */
public class InitialStockDTO {
    private Long id;
    private String productCode;
    private String productName;
    private String warehouseName;
    private BigDecimal quantity;
    private BigDecimal unitPrice;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}
