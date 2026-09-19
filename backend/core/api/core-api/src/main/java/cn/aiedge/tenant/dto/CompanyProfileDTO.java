package cn.aiedge.tenant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 「企业信息」页（设置 → 系统配置 → 企业信息，菜单 80624 / set:company-info）的写入 DTO。
 *
 * <p>为什么不用 {@link cn.aiedge.base.entity.SysTenant} 直接接 body：
 * 该实体同时含**平台侧列**（tenantCode / adminUserId / level / expireTime / status / deleted），
 * 若用实体接 body，租户管理员就能顺手改自己的套餐等级与到期时间（越权面）。
 * 这里用白名单 DTO，只放「企业自身可改」的档案字段。
 *
 * <p>空串容忍：可空的文本字段允许传空串（前端清空后即空串/undefined），
 * 控制器统一把空白归一为 {@code null} 落库，保证「清空」语义生效
 * （MyBatis-Plus `updateById` 忽略 null 的历史坑，本项目改用 UpdateWrapper 显式 set，见 TenantController）。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public class CompanyProfileDTO {

    /**
     * 企业档案保存请求（对应 Tab① 企业信息 + Tab② 纳税人信息）
     */
    public record Save(
            @NotBlank(message = "企业名称不能为空")
            @Size(max = 50, message = "企业名称最长 50 个字符")
            @JsonProperty("tenantName")
            String tenantName,

            @Pattern(regexp = "^$|^[0-9A-Z]{18}$", message = "统一社会信用代码应为 18 位大写字母或数字")
            @Size(max = 32, message = "统一社会信用代码最长 32 个字符")
            @JsonProperty("creditCode")
            String creditCode,

            @Size(max = 32, message = "企业类型最长 32 个字符")
            @JsonProperty("companyType")
            String companyType,

            @Size(max = 64, message = "法定代表人最长 64 个字符")
            @JsonProperty("legalPerson")
            String legalPerson,

            @DecimalMin(value = "0", message = "注册资本不能为负数")
            @JsonProperty("registeredCapital")
            BigDecimal registeredCapital,

            @Size(max = 64, message = "所属行业最长 64 个字符")
            @JsonProperty("industry")
            String industry,

            @Size(max = 32, message = "企业规模最长 32 个字符")
            @JsonProperty("companyScale")
            String companyScale,

            @JsonFormat(pattern = "yyyy-MM-dd")
            @JsonProperty("establishDate")
            LocalDate establishDate,

            @Size(max = 2000, message = "经营范围最长 2000 个字符")
            @JsonProperty("businessScope")
            String businessScope,

            @Size(max = 32, message = "联系人最长 32 个字符")
            @JsonProperty("contactPerson")
            String contactPerson,

            @Size(max = 32, message = "联系电话最长 32 个字符")
            @JsonProperty("contactPhone")
            String contactPhone,

            @Email(message = "请输入有效的企业邮箱")
            @Size(max = 128, message = "企业邮箱最长 128 个字符")
            @JsonProperty("contactEmail")
            String contactEmail,

            @Size(max = 255, message = "企业地址最长 255 个字符")
            @JsonProperty("address")
            String address,

            @Size(max = 32, message = "联系人电话最长 32 个字符")
            @JsonProperty("contactPersonPhone")
            String contactPersonPhone,

            @Email(message = "请输入有效的联系人邮箱")
            @Size(max = 128, message = "联系人邮箱最长 128 个字符")
            @JsonProperty("contactPersonEmail")
            String contactPersonEmail,

            @Size(max = 64, message = "纳税人识别号最长 64 个字符")
            @JsonProperty("taxNumber")
            String taxNumber,

            @Size(max = 255, message = "开票地址最长 255 个字符")
            @JsonProperty("taxpayerAddress")
            String taxpayerAddress,

            @Size(max = 32, message = "开票电话最长 32 个字符")
            @JsonProperty("taxpayerPhone")
            String taxpayerPhone,

            @Size(max = 128, message = "开户行地址最长 128 个字符")
            @JsonProperty("bankName")
            String bankName,

            @Size(max = 64, message = "开户行账号最长 64 个字符")
            @JsonProperty("bankAccount")
            String bankAccount,

            /**
             * 企业 LOGO 地址（2026-09-18 本轮新增）
             *
             * <p>值来自通用上传端点 {@code POST /api/file/upload} 返回的 {@code url}
             * （形如 {@code /api/file/view/yyyy/MM/dd/xxx.png}）。只接收 URL 字符串，
             * 不做服务端下载/校验（与 CertUploadList / 银行账户收款码等既有上传口径一致）；
             * 传空串/不传即清空 LOGO。</p>
             */
            @Size(max = 500, message = "LOGO 地址最长 500 个字符")
            @JsonProperty("logoUrl")
            String logoUrl
    ) {
    }
}
