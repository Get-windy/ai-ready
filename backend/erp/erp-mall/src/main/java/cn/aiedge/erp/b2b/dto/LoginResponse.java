package cn.aiedge.erp.b2b.dto;

import lombok.Data;

import java.util.List;

@Data
public class LoginResponse {
    private String token;
    private UserInfo user;

    /** 用户可切换的所有身份（个人会员 + 关联的企业客户） */
    private List<IdentityDTO> identities;

    /** 当前激活的身份 partyId（默认首次登录为个人会员） */
    private Long activeIdentity;
}
