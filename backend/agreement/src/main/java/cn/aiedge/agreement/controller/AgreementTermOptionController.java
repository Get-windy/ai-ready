package cn.aiedge.agreement.controller;

import cn.aiedge.agreement.dto.TermOptionDTO;
import cn.aiedge.agreement.dto.TermOptionGroupVO;
import cn.aiedge.agreement.dto.TermOptionVO;
import cn.aiedge.agreement.service.AgreementTermOptionService;
import cn.aiedge.common.utils.Result;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 平台条款字典接口。
 *
 * <h3>权限隔离（裁定⑤）</h3>
 * <ul>
 *   <li>读（{@code /grouped}、按类别查）：{@code agreement:term-option:list} ⇒ 归属模块 {@code agreement}（租户级）。
 *       租户侧要读字典才能在下拉里选条款，所以读权限属于租户级模块。</li>
 *   <li>写（增 / 改 / 删）：{@code agreement:platform:term-option:manage} ⇒ 按**最长前缀优先**归属模块
 *       {@code system}；而「系统」模块按 V11.455.0 只开给系统租户
 *       ⇒ 平台协议的字典维护**天然只有平台侧能用**，不必另造机制。</li>
 * </ul>
 *
 * <p>⚠️ 本控制器**不提供任何"默认值"的读写**：字典只定义"有哪些选项、各自什么含义"，
 * 不规定"必须选哪个"（㉜ / §3.4.4d1）。</p>
 *
 * <p>路径前缀与主控制器同族（{@code /api/agreement/term-options}），
 * Spring 对字面量路径的优先级高于 {@code /api/agreement/{id}}，因此
 * {@code GET /api/agreement/term-options} 不会被详情端点抢走。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/agreement/term-options")
@RequiredArgsConstructor
@Tag(name = "协议条款字典", description = "平台维护的条款选项字典：只定义选项与语义，不提供默认值")
public class AgreementTermOptionController {

    private final AgreementTermOptionService termOptionService;

    @SaCheckPermission("agreement:term-option:list")
    @GetMapping("/grouped")
    @Operation(summary = "条款字典（按条款类别分组，协议详情页一个下拉 = 一组）")
    public Result<List<TermOptionGroupVO>> grouped() {
        return Result.success(termOptionService.grouped());
    }

    @SaCheckPermission("agreement:term-option:list")
    @GetMapping
    @Operation(summary = "查询某类条款的全部可选选项")
    public Result<List<TermOptionVO>> listByTermCode(
            @Parameter(description = "条款类别，如 RETURN_FREIGHT") @RequestParam(required = false) String termCode) {
        return Result.success(termOptionService.listByTermCode(termCode));
    }

    @SaCheckPermission("agreement:platform:term-option:manage")
    @PostMapping
    @Operation(summary = "新增条款选项（平台侧）")
    public Result<Long> create(@RequestBody TermOptionDTO dto) {
        return Result.success(termOptionService.create(dto));
    }

    @SaCheckPermission("agreement:platform:term-option:manage")
    @PutMapping("/{id}")
    @Operation(summary = "修改条款选项（平台侧；历史协议快照不受影响）")
    public Result<Void> update(@PathVariable Long id, @RequestBody TermOptionDTO dto) {
        termOptionService.update(id, dto);
        return Result.success();
    }

    @SaCheckPermission("agreement:platform:term-option:manage")
    @DeleteMapping("/{id}")
    @Operation(summary = "删除条款选项（平台侧；软删，历史协议快照不受影响）")
    public Result<Void> delete(@PathVariable Long id) {
        termOptionService.delete(id);
        return Result.success();
    }
}
