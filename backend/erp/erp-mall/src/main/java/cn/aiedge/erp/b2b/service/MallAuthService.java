package cn.aiedge.erp.b2b.service;

import cn.aiedge.erp.b2b.dto.IdentityDTO;
import cn.aiedge.erp.b2b.dto.LoginRequest;
import cn.aiedge.erp.b2b.dto.LoginResponse;
import cn.aiedge.erp.b2b.dto.UserInfo;

import java.util.List;

public interface MallAuthService {

    LoginResponse login(LoginRequest request);

    void register(LoginRequest request);

    void logout();

    String refreshToken();

    /**
     * 获取当前用户可切换的所有身份。
     * 包含：个人会员身份 + 作为联系人的所有企业客户身份。
     */
    List<IdentityDTO> listIdentities();

    /**
     * 切换当前下单身份。
     *
     * @param partyId 目标身份的 biz_party.id
     */
    void switchIdentity(Long partyId);
}
