package cn.aiedge.erp.b2b.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 收货地址
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_address")
public class MallAddress extends BaseEntity {

    @TableField("customer_id")
    private Long customerId;

    @TableField("consignee")
    private String consignee;

    @TableField("phone")
    private String phone;

    @TableField("province")
    private String province;

    @TableField("city")
    private String city;

    @TableField("district")
    private String district;

    @TableField("detail_address")
    private String detailAddress;

    @TableField("is_default")
    private Boolean isDefault = false;

    @TableField("label")
    private String label;
}
