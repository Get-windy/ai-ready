package cn.aiedge.customer.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户跟进记录DTO
 */
@Data
public class CustomerFollowDTO {

    private Long id;
    private Long tenantId;
    private Long customerId;
    private Integer followType;
    private String followTypeName;
    private String content;
    private LocalDateTime nextFollowTime;
    private Integer result;
    private String resultName;
    private Integer stageFrom;
    private Integer stageTo;
    private Long followerId;
    private String followerName;
    private String attachments;
    private LocalDateTime followTime;
    private String remark;
    private LocalDateTime createTime;
}