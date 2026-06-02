package cn.aiedge.erp.b2b.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private UserInfo user;
}
