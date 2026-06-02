package cn.aiedge.erp.b2b.controller;

import cn.aiedge.erp.b2b.dto.AddressDTO;
import cn.aiedge.erp.b2b.dto.ApiResponse;
import cn.aiedge.erp.b2b.dto.UserInfo;
import cn.aiedge.erp.b2b.service.MallUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mall/user")
@Tag(name = "商城用户", description = "用户信息、地址管理等接口")
@RequiredArgsConstructor
public class MallUserController {

    private final MallUserService mallUserService;

    @Operation(summary = "获取用户信息", description = "获取当前登录用户的基本信息")
    @GetMapping("/info")
    public ApiResponse<UserInfo> getUserInfo() {
        UserInfo userInfo = mallUserService.getUserInfo();
        return ApiResponse.success(userInfo);
    }

    @Operation(summary = "更新用户信息", description = "更新当前登录用户的个人信息")
    @PutMapping("/profile")
    public ApiResponse<UserInfo> updateProfile(@RequestBody UserInfo userInfo) {
        UserInfo updated = mallUserService.updateProfile(userInfo);
        return ApiResponse.success("更新成功", updated);
    }

    @Operation(summary = "获取地址列表", description = "获取当前用户的收货地址列表")
    @GetMapping("/addresses")
    public ApiResponse<List<AddressDTO>> getAddresses() {
        List<AddressDTO> addresses = mallUserService.getAddresses();
        return ApiResponse.success(addresses);
    }

    @Operation(summary = "新增地址", description = "新增收货地址")
    @PostMapping("/addresses")
    public ApiResponse<AddressDTO> addAddress(@RequestBody AddressDTO addressDTO) {
        AddressDTO created = mallUserService.addAddress(addressDTO);
        return ApiResponse.success("新增地址成功", created);
    }

    @Operation(summary = "更新地址", description = "更新指定的收货地址")
    @PutMapping("/addresses/{id}")
    public ApiResponse<AddressDTO> updateAddress(
            @Parameter(description = "地址ID") @PathVariable Long id,
            @RequestBody AddressDTO addressDTO) {
        AddressDTO updated = mallUserService.updateAddress(id, addressDTO);
        return ApiResponse.success("更新地址成功", updated);
    }

    @Operation(summary = "删除地址", description = "删除指定的收货地址")
    @DeleteMapping("/addresses/{id}")
    public ApiResponse<Void> deleteAddress(
            @Parameter(description = "地址ID") @PathVariable Long id) {
        mallUserService.deleteAddress(id);
        return ApiResponse.success("删除地址成功", null);
    }

    @Operation(summary = "设为默认地址", description = "将指定地址设为默认收货地址")
    @PutMapping("/addresses/{id}/default")
    public ApiResponse<Void> setDefaultAddress(
            @Parameter(description = "地址ID") @PathVariable Long id) {
        mallUserService.setDefaultAddress(id);
        return ApiResponse.success("设置默认地址成功", null);
    }
}
