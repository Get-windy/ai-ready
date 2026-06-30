package cn.aiedge.erp.b2b.service;

import cn.aiedge.erp.b2b.dto.AddressDTO;
import cn.aiedge.erp.b2b.dto.UserInfo;

import java.util.List;

public interface MallUserService {

    UserInfo getUserInfo();

    UserInfo updateProfile(UserInfo userInfo);

    List<AddressDTO> getAddresses();

    AddressDTO addAddress(AddressDTO addressDTO);

    AddressDTO updateAddress(Long id, AddressDTO addressDTO);

    void deleteAddress(Long id);

    void setDefaultAddress(Long id);
}
