package cn.aiedge.erp.stock.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 商品图片列表行 VO（对标：商品图片列表 9 列）
 *
 * <p>商品维度一行，附该商品已关联的图片（图片列 + 主图标记）。</p>
 */
@Data
public class ProductImageRowVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 货号（优先 productCodeAlias，回退 productCode） */
    private String productCode;

    /** 商品状态 ENABLED / DISABLED */
    private String status;

    private String spec;

    private String model;

    private String origin;

    private String brand;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long categoryId;

    /** 该商品已关联的图片 */
    private List<ImageItem> images = new ArrayList<>();

    @Data
    public static class ImageItem {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        /** 图片名称 */
        private String imageName;
        /** 图片访问URL */
        private String imageUrl;
        /** 是否主图 */
        private Integer isMain;
        private Long fileSize;
    }
}
