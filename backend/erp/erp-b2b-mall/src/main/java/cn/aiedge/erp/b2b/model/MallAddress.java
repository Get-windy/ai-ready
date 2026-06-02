package cn.aiedge.erp.b2b.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "mall_address")
public class MallAddress extends BaseEntity {

    @Column(name = "customer_id", length = 64, nullable = false)
    private String customerId;

    @Column(name = "consignee", length = 50, nullable = false)
    private String consignee;

    @Column(name = "phone", length = 20, nullable = false)
    private String phone;

    @Column(name = "province", length = 50)
    private String province;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "district", length = 50)
    private String district;

    @Column(name = "detail_address", length = 500)
    private String detailAddress;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "label", length = 50)
    private String label;
}
