package cn.aiedge.erp.stock.dto;

import lombok.Data;

/**
 * 图片自动匹配结果 VO
 */
@Data
public class ProductImageMatchResultVO {

    /** 参与匹配的素材总数 */
    private int total;

    /** 匹配成功数 */
    private int matched;

    /** 匹配到多个商品（歧义）跳过数 */
    private int ambiguous;

    /** 未匹配数 */
    private int unmatched;
}
