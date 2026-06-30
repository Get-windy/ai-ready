package cn.aiedge.erp.b2b.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
    /** 手机号（注册时使用，用于匹配已有会员） */
    private String phone;
}
