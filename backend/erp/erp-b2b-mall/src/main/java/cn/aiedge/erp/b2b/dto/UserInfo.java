package cn.aiedge.erp.b2b.dto;

import lombok.Data;

@Data
public class UserInfo {
    private String id;
    private String username;
    private String nickname;
    private String phone;
    private String email;
    private String avatar;
    private String level;
    private int points;
    private double balance;
}
