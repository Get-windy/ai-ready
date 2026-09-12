package cn.aiedge.wms.warehouse.dto;

import lombok.Data;

/**
 * 货位批量生成参数（对标 ql361 新增货位弹窗）
 *
 * <p>编号规则：{通道号}{货架号}-{货架层}{货位列号}，如 A1-101；
 * 生成数量 = 通道生成数 × 货架生成数 × 货架层数 × 货位列生成数。</p>
 */
@Data
public class LocationGenerateDTO {

    /** 仓库ID（必填） */
    private Long warehouseId;

    /** 仓库名称快照（前端选择仓库时带出，后端缺失时回查） */
    private String warehouseName;

    /** 通道号，如 A */
    private String channelNo;

    /** 通道生成数 */
    private Integer channelCount;

    /** 货架号，如 01 */
    private String shelfNo;

    /** 货架生成数 */
    private Integer shelfCount;

    /** 货架层数 */
    private Integer layerCount;

    /** 货位列号，如 01 */
    private String columnNo;

    /** 货位列生成数 */
    private Integer columnCount;

    /** 备注 */
    private String remark;
}
