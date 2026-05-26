package cn.aiedge.erp.printing.enums;

public enum TemplateType {
    SALES_ORDER("SALES_ORDER", "销售订单"),
    PURCHASE_ORDER("PURCHASE_ORDER", "采购订单"),
    INVOICE("INVOICE", "发票"),
    RECEIPT("RECEIPT", "收据"),
    DELIVERY_NOTE("DELIVERY_NOTE", "送货单"),
    PICKING_LIST("PICKING_LIST", "拣货单"),
    STOCK_IN("STOCK_IN", "入库单"),
    STOCK_OUT("STOCK_OUT", "出库单"),
    REPORT("REPORT", "报表"),
    LABEL("LABEL", "标签"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String name;

    TemplateType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}