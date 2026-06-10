package cn.aiedge.erp.b2b.service;

import cn.aiedge.erp.b2b.dto.LoginRequest;
import cn.aiedge.erp.b2b.dto.LoginResponse;
import cn.aiedge.erp.b2b.dto.UserInfo;

public interface MallAuthService {

    LoginResponse login(LoginRequest request);

    void register(LoginRequest request);

    void logout();

    String refreshToken();
}
