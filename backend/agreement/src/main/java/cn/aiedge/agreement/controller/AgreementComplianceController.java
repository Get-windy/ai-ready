package cn.aiedge.agreement.controller;

import cn.aiedge.agreement.dto.AgreementQuery;
import cn.aiedge.agreement.dto.AgreementVO;
import cn.aiedge.agreement.service.AgreementService;
import cn.aiedge.base.annotation.OperLog;
import cn.aiedge.common.utils.Result;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * **平台合规抽查读**接口（DOMAIN-MODEL §13.9）。
 *
 * <h3>它解决什么问题</h3>
 * 用户 2026-09-22 明确：「平台**合规抽查读权限需要有**（避免非法交易）」。
 * 而在此之前协议的所有读端点都复用**租户级** `agreement:view`，
 * 加上 {@code AgreementVisibility} 只放行"两端之一"⇒ 平台侧要么读不到，
 * 要么只能把租户级码授给平台（那等于把"读别人协议"的能力散给所有租户管理员，**更糟**）。
 *
 * <h3>⚠️ 为什么单独立一个 Controller 而不是在原读接口上开个开关</h3>
 * 这两条路径的**可见性完全不同**（一个只放行两端、一个不限制），
 * 混在同一个方法里靠参数切换，迟早出现"某个分支忘了判角色"的越权。
 * 分开之后：**看得见这个类的每一行，都是"平台能读别人的协议"这件事的显式声明**。
 *
 * <h3>三道门（缺一不可）</h3>
 * <ol>
 *   <li><b>权限码</b> {@code agreement:platform:compliance:read} —— 前缀 {@code agreement:platform:}
 *       归属「系统」模块，而「系统」按 V11.455.0 **只开给系统租户** ⇒ 天然只有平台侧能拿；</li>
 *   <li><b>可见性收敛在 {@code AgreementVisibility}</b>：本类不自己拼 SQL 条件，
 *       放宽可见性的那一句写在那个类里（"唯一构造处"的纪律不破）；</li>
 *   <li><b>留痕</b>：{@code @OperLog} 记"谁在什么时候抽查了哪一份"。
 *       合规抽查是稽核动作，**没有留痕的抽查读等于给了平台一个静默的超级读权限**。</li>
 * </ol>
 *
 * <p>⚠️ 本类**只读**：不提供任何写能力，也不提供"按 id 改状态"之类的便利方法。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/agreement/platform/compliance")
@RequiredArgsConstructor
@Tag(name = "协议平台合规抽查读", description = "平台侧按 §13.9 抽查任意租户之间的协议（避免非法交易）；只读 + 留痕")
public class AgreementComplianceController {

    private final AgreementService agreementService;

    @SaCheckPermission("agreement:platform:compliance:read")
    @OperLog(module = "协议-平台合规抽查", action = "合规抽查读协议列表")
    @GetMapping("/page")
    @Operation(summary = "合规抽查：分页查询**任意租户之间**的协议（不按会话租户过滤，调用会留痕）")
    public Result<Page<AgreementVO>> page(AgreementQuery query) {
        return Result.success(agreementService.compliancePage(query));
    }

    @SaCheckPermission("agreement:platform:compliance:read")
    @OperLog(module = "协议-平台合规抽查", action = "合规抽查读协议详情")
    @GetMapping("/{id}")
    @Operation(summary = "合规抽查：协议详情（含当前生效版本与条款，用于取证；调用会留痕）")
    public Result<AgreementVO> detail(@PathVariable Long id) {
        return Result.success(agreementService.complianceDetail(id));
    }
}
