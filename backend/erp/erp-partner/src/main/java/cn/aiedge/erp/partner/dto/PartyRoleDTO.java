package cn.aiedge.erp.partner.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
public class PartyRoleDTO {

    private Long id;

    private String roleName;

    private String roleCode;

    private String description;

    private Integer status;

    private Integer sortOrder;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}