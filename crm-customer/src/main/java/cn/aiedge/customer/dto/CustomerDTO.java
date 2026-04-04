package cn.aiedge.customer.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 客户DTO
 */
@Data
public class CustomerDTO {

    private Long id;
    private Long tenantId;
    private String customerCode;
    private String customerName;
    private String shortName;
    private Integer customerType;
    private String customerTypeName;
    private Integer source;
    private String sourceName;
    private String industry;
    private Integer level;
    private String levelName;
    private Integer stage;
    private String stageName;
    private Long ownerId;
    private String ownerName;
    private Long deptId;
    private String phone;
    private String mobile;
    private String email;
    private String website;
    private String province;
    private String city;
    private String district;
    private String address;
    private String remark;
    private LocalDateTime nextFollowTime;
    private LocalDateTime lastFollowTime;
    private Integer followCount;
    private BigDecimal dealAmount;
    private Integer dealCount;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    /** 联系人列表 */
    private List<CustomerContactDTO> contacts;
}