package cn.aiedge.customer.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户联系人DTO
 */
@Data
public class CustomerContactDTO {

    private Long id;
    private Long customerId;
    private String contactName;
    private Integer gender;
    private String position;
    private String department;
    private Boolean isKeyDecisionMaker;
    private String phone;
    private String mobile;
    private String email;
    private String wechat;
    private String qq;
    private LocalDateTime birthday;
    private String remark;
    private Integer status;
}