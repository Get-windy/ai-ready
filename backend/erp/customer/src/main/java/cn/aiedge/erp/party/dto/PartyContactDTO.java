package cn.aiedge.erp.party.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
public class PartyContactDTO {

    private Long id;

    private Long partyId;

    private String contactName;

    private String position;

    private String department;

    private String phone;

    private String mobile;

    private String email;

    private String wechat;

    private String qq;

    private Integer isPrimary;

    private Integer contactRole;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
