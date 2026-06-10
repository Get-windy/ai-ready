package cn.aiedge.erp.b2b.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.b2b.dto.AddressDTO;
import cn.aiedge.erp.b2b.dto.UserInfo;
import cn.aiedge.erp.b2b.mapper.MallAddressMapper;
import cn.aiedge.erp.b2b.mapper.ShopUserMapper;
import cn.aiedge.erp.b2b.model.MallAddress;
import cn.aiedge.erp.b2b.model.ShopUser;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MallUserServiceImpl implements MallUserService {

    private final MallAddressMapper mallAddressMapper;
    private final ShopUserMapper shopUserMapper;

    /** 获取当前登录用户的租户ID */
    private Long getTenantId() {
        Object tid = StpUtil.getSession().get("tenantId");
        return tid instanceof Number ? ((Number) tid).longValue() : 0L;
    }

    @Override
    public UserInfo getUserInfo() {
        log.info("获取用户信息");
        Long userId = StpUtil.getLoginIdAsLong();

        ShopUser user = shopUserMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setId(String.valueOf(user.getId()));
        userInfo.setUsername(user.getUsername());
        userInfo.setNickname(user.getNickname());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setPhone(user.getPhone());
        // B2B用户等级基于公司信息
        if (user.getCompanyName() != null && !user.getCompanyName().isEmpty()) {
            userInfo.setLevel("企业会员");
        } else {
            userInfo.setLevel("普通会员");
        }
        userInfo.setPoints(0);
        userInfo.setBalance(0.0);
        return userInfo;
    }

    @Override
    public UserInfo updateProfile(UserInfo userInfo) {
        log.info("更新用户信息: {}", userInfo.getUsername());
        Long userId = StpUtil.getLoginIdAsLong();

        ShopUser user = shopUserMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        if (userInfo.getNickname() != null) {
            user.setNickname(userInfo.getNickname());
        }
        if (userInfo.getPhone() != null) {
            user.setPhone(userInfo.getPhone());
        }
        if (userInfo.getAvatar() != null) {
            user.setAvatar(userInfo.getAvatar());
        }

        shopUserMapper.updateById(user);
        return userInfo;
    }

    @Override
    public List<AddressDTO> getAddresses() {
        log.info("获取地址列表");
        Long customerId = StpUtil.getLoginIdAsLong();
        Long tenantId = getTenantId();

        List<MallAddress> addresses = mallAddressMapper.selectList(
                new LambdaQueryWrapper<MallAddress>()
                        .eq(MallAddress::getCustomerId, customerId)
                        .eq(MallAddress::getTenantId, tenantId)
                        .eq(MallAddress::getDeleted, 0)
        );

        return addresses.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AddressDTO addAddress(AddressDTO addressDTO) {
        log.info("新增地址: {}", addressDTO.getConsignee());
        Long customerId = StpUtil.getLoginIdAsLong();
        Long tenantId = getTenantId();

        MallAddress address = new MallAddress();
        address.setCustomerId(customerId);
        address.setTenantId(tenantId);
        address.setConsignee(addressDTO.getConsignee());
        address.setPhone(addressDTO.getPhone());
        address.setProvince(addressDTO.getProvince());
        address.setCity(addressDTO.getCity());
        address.setDistrict(addressDTO.getDistrict());
        address.setDetailAddress(addressDTO.getDetailAddress());
        address.setIsDefault(addressDTO.getIsDefault());
        address.setLabel(addressDTO.getLabel());

        if (Boolean.TRUE.equals(address.getIsDefault())) {
            clearDefaultAddress(customerId);
        }

        mallAddressMapper.insert(address);
        return convertToDTO(address);
    }

    @Override
    @Transactional
    public AddressDTO updateAddress(Long id, AddressDTO addressDTO) {
        log.info("更新地址: {}", id);
        MallAddress address = mallAddressMapper.selectById(id);
        if (address == null) {
            throw BusinessException.notFound("地址不存在: " + id);
        }

        address.setConsignee(addressDTO.getConsignee());
        address.setPhone(addressDTO.getPhone());
        address.setProvince(addressDTO.getProvince());
        address.setCity(addressDTO.getCity());
        address.setDistrict(addressDTO.getDistrict());
        address.setDetailAddress(addressDTO.getDetailAddress());
        address.setIsDefault(addressDTO.getIsDefault());
        address.setLabel(addressDTO.getLabel());

        if (Boolean.TRUE.equals(address.getIsDefault())) {
            clearDefaultAddress(address.getCustomerId());
        }

        mallAddressMapper.updateById(address);
        return convertToDTO(address);
    }

    @Override
    @Transactional
    public void deleteAddress(Long id) {
        log.info("删除地址: {}", id);
        MallAddress address = mallAddressMapper.selectById(id);
        if (address == null) {
            throw BusinessException.notFound("地址不存在: " + id);
        }
        address.setDeleted(1);
        mallAddressMapper.updateById(address);
    }

    @Override
    @Transactional
    public void setDefaultAddress(Long id) {
        log.info("设置默认地址: {}", id);
        MallAddress address = mallAddressMapper.selectById(id);
        if (address == null) {
            throw BusinessException.notFound("地址不存在: " + id);
        }

        clearDefaultAddress(address.getCustomerId());

        address.setIsDefault(true);
        mallAddressMapper.updateById(address);
    }

    private void clearDefaultAddress(Long customerId) {
        MallAddress defaultAddress = mallAddressMapper.selectOne(
                new LambdaQueryWrapper<MallAddress>()
                        .eq(MallAddress::getCustomerId, customerId)
                        .eq(MallAddress::getIsDefault, true)
                        .eq(MallAddress::getDeleted, 0)
        );
        if (defaultAddress != null) {
            defaultAddress.setIsDefault(false);
            mallAddressMapper.updateById(defaultAddress);
        }
    }

    private AddressDTO convertToDTO(MallAddress address) {
        AddressDTO dto = new AddressDTO();
        dto.setId(address.getId());
        dto.setConsignee(address.getConsignee());
        dto.setPhone(address.getPhone());
        dto.setProvince(address.getProvince());
        dto.setCity(address.getCity());
        dto.setDistrict(address.getDistrict());
        dto.setDetailAddress(address.getDetailAddress());
        dto.setIsDefault(address.getIsDefault());
        dto.setLabel(address.getLabel());
        return dto;
    }
}
