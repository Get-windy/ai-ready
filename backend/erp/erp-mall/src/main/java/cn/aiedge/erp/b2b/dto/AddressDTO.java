package cn.aiedge.erp.b2b.dto;

import lombok.Data;

/**
 * 收货地址（与表 {@code mall_address} 一一对应）。
 *
 * <p>2026-09-26：原先这里是 {@code province / city / district / detailAddress / label} 五个字段，
 * 而表里其实是 {@code region}（省市区一个串）+ {@code address}（详细地址）—— 字段对不上，
 * 地址接口全部 500（详见 {@code MallAddress} 的类注释）。
 * 现按表结构收敛为 {@code region} + {@code address}，前后端一起改。</p>
 */
@Data
public class AddressDTO {

    private Long id;

    /** 收货人 */
    private String consignee;

    /** 联系电话 */
    private String phone;

    /** 省市区（一个字符串，如「广东省深圳市南山区」） */
    private String region;

    /** 详细地址（街道门牌等） */
    private String address;

    private Boolean isDefault;
}
