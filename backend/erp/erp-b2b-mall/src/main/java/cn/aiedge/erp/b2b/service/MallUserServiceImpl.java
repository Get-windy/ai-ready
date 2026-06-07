package cn.aiedge.erp.b2b.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.b2b.dto.AddressDTO;
import cn.aiedge.erp.b2b.dto.UserInfo;
import cn.aiedge.erp.b2b.mapper.MallAddressMapper;
import cn.aiedge.erp.b2b.model.MallAddress;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MallUserServiceImpl implements MallUserService {

    private final MallAddressMapper mallAddressMapper;

    @Override
    public UserInfo getUserInfo() {
        log.info("获取用户信息");
        String userId = StpUtil.getLoginIdAsString();

        // TODO: Get actual user info from user service
        UserInfo userInfo = new UserInfo();
        userInfo.setId(userId);
        userInfo.setUsername("test_user");
        userInfo.setNickname("测试用户");
        userInfo.setLevel("普通会员");
        userInfo.setPoints(0);
        userInfo.setBalance(0.0);
        return userInfo;
    }

    @Override
    public UserInfo updateProfile(UserInfo userInfo) {
        log.info("更新用户信息: {}", userInfo.getUsername());

        // TODO: Implement actual user profile update
        return userInfo;
    }

    @Override
    public List<AddressDTO> getAddresses() {
        log.info("获取地址列表");
        String customerId = StpUtil.getLoginIdAsString();

        List<MallAddress> addresses = mallAddressMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MallAddress>()
                        .eq(MallAddress::getCustomerId, customerId)
                        .eq(MallAddress::getDeleted, false)
        );

        return addresses.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AddressDTO addAddress(AddressDTO addressDTO) {
        log.info("新增地址: {}", addressDTO.getConsignee());
        String customerId = StpUtil.getLoginIdAsString();

        MallAddress address = new MallAddress();
        address.setCustomerId(customerId);
        address.setConsignee(addressDTO.getConsignee());
        address.setPhone(addressDTO.getPhone());
        address.setProvince(addressDTO.getProvince());
        address.setCity(addressDTO.getCity());
        address.setDistrict(addressDTO.getDistrict());
        address.setDetailAddress(addressDTO.getDetailAddress());
        address.setIsDefault(addressDTO.getIsDefault());
        address.setLabel(addressDTO.getLabel());
        address.setCreatedAt(LocalDateTime.now());
        address.setUpdatedAt(LocalDateTime.now());

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
        address.setUpdatedAt(LocalDateTime.now());

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
        address.setDeleted(true);
        address.setUpdatedAt(LocalDateTime.now());
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
        address.setUpdatedAt(LocalDateTime.now());
        mallAddressMapper.updateById(address);
    }

    private void clearDefaultAddress(String customerId) {
        MallAddress defaultAddress = mallAddressMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MallAddress>()
                        .eq(MallAddress::getCustomerId, customerId)
                        .eq(MallAddress::getIsDefault, true)
                        .eq(MallAddress::getDeleted, false)
        );
        if (defaultAddress != null) {
            defaultAddress.setIsDefault(false);
            defaultAddress.setUpdatedAt(LocalDateTime.now());
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
