package cn.aiedge.erp.stock.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 商城标签行 VO（含「对应商品」聚合）
 *
 * <p>对标 ql361「商品辅助资料 → 商品标签」列「对应商品」= 该标签下商品名聚合串。
 */
@Data
@Schema(description = "商城标签行")
public class MallTagVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 标签标准槽位编码 TAG_1..TAG_20 */
    private String tagCode;

    /** 标签显示名（用户自定义昵称，默认「标签N」） */
    private String tagName;

    private Integer sortOrder;

    /** 状态: 1启用 0停用 */
    private Integer status;

    @Schema(description = "对应商品数量")
    private Integer productCount;

    @Schema(description = "对应商品名称聚合串")
    private String productNames;
}
