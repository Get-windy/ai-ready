package cn.aiedge.erp.b2b.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.b2b.dto.AddressDTO;
import cn.aiedge.erp.b2b.dto.UserInfo;
import cn.aiedge.erp.b2b.mapper.MallAddressMapper;
import cn.aiedge.erp.b2b.mapper.ShopUserMapper;
import cn.aiedge.erp.b2b.mapper.ShopUserTenantMapper;
import cn.aiedge.erp.b2b.model.MallAddress;
import cn.aiedge.erp.b2b.model.ShopUser;
import cn.aiedge.erp.b2b.model.ShopUserTenant;
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
    /** 「顾客 × 租户」关联：本店准入状态（审核/启用）的权威来源 */
    private final ShopUserTenantMapper shopUserTenantMapper;

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

        // ── 本店准入状态（2026-09-26 新增）──
        // C 端「价格三态」的第三态判据：已登录但**未通过本店审核**时，前端要显示
        // 「认证后可见价 + 去认证」，而不是价格。审核/启用都**逐租户**存于
        // shop_user_tenant（shop_user.audit_status 已降级为历史列，不能再读）。
        // 查不到关联行时不写（保持 null）—— 由前端按"未认证"处理，不臆造为已通过。
        try {
            ShopUserTenant link = shopUserTenantMapper.selectLink(userId, getTenantId());
            if (link != null) {
                userInfo.setAuditStatus(link.getStatus());
                userInfo.setShopEnabled(link.getEnabled());
            }
        } catch (Exception e) {
            // 准入状态是**展示辅助**，取不到不应让"我的"整页失败
            log.warn("查询本店准入状态失败（按未认证处理）: userId={}, tenantId={}", userId, getTenantId(), e);
        }
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
        Long userId = StpUtil.getLoginIdAsLong();
        Long tenantId = getTenantId();

        List<MallAddress> addresses = mallAddressMapper.selectList(
                new LambdaQueryWrapper<MallAddress>()
                        .eq(MallAddress::getUserId, userId)
                        .eq(MallAddress::getTenantId, tenantId)
                        .eq(MallAddress::getDeleted, 0)
        );

        return addresses.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AddressDTO addAddress(AddressDTO addressDTO) {
        log.info("新增地址: {}", addressDTO.getConsignee());
        Long userId = StpUtil.getLoginIdAsLong();
        Long tenantId = getTenantId();

        MallAddress address = new MallAddress();
        address.setUserId(userId);
        address.setTenantId(tenantId);
        address.setConsignee(addressDTO.getConsignee());
        address.setPhone(addressDTO.getPhone());
        address.setRegion(addressDTO.getRegion());
        address.setAddress(addressDTO.getAddress());
        // DTO 用 Boolean（前端语义），表列是 integer（0/1）—— 在边界转换
        address.setIsDefault(Boolean.TRUE.equals(addressDTO.getIsDefault()) ? 1 : 0);

        if (Integer.valueOf(1).equals(address.getIsDefault())) {
            clearDefaultAddress(userId);
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
        address.setRegion(addressDTO.getRegion());
        address.setAddress(addressDTO.getAddress());
        address.setIsDefault(Boolean.TRUE.equals(addressDTO.getIsDefault()) ? 1 : 0);

        if (Integer.valueOf(1).equals(address.getIsDefault())) {
            clearDefaultAddress(address.getUserId());
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
        // ⚠️ 必须用 deleteById：@TableLogic 下 "setDeleted(1) + updateById" **不会**把它置为已删
        //    （MP 会把逻辑删除列排除在 UPDATE 的 SET 之外），结果是"删了还在列表里"。
        mallAddressMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void setDefaultAddress(Long id) {
        log.info("设置默认地址: {}", id);
        MallAddress address = mallAddressMapper.selectById(id);
        if (address == null) {
            throw BusinessException.notFound("地址不存在: " + id);
        }

        clearDefaultAddress(address.getUserId());

        address.setIsDefault(1);
        mallAddressMapper.updateById(address);
    }

    private void clearDefaultAddress(Long userId) {
        MallAddress defaultAddress = mallAddressMapper.selectOne(
                new LambdaQueryWrapper<MallAddress>()
                        .eq(MallAddress::getUserId, userId)
                        .eq(MallAddress::getIsDefault, 1)
                        .eq(MallAddress::getDeleted, 0)
        );
        if (defaultAddress != null) {
            defaultAddress.setIsDefault(0);
            mallAddressMapper.updateById(defaultAddress);
        }
    }

    private AddressDTO convertToDTO(MallAddress address) {
        AddressDTO dto = new AddressDTO();
        dto.setId(address.getId());
        dto.setConsignee(address.getConsignee());
        dto.setPhone(address.getPhone());
        dto.setRegion(address.getRegion());
        dto.setAddress(address.getAddress());
        dto.setIsDefault(Integer.valueOf(1).equals(address.getIsDefault()));
        return dto;
    }
}
