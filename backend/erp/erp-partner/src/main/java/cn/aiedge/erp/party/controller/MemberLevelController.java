package cn.aiedge.erp.party.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.party.entity.MemberLevel;
import cn.aiedge.erp.party.service.IMemberLevelService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会员级别（营销权益等级）
 * <p>
 * 对标「会员卡」弹窗上的会员级别* 选择器与「会员管理 → 会员设置」入口。
 * 与「客户级别」（价格等级，见 {@code /erp/customer/level}）是两套独立体系。
 * </p>
 */
@Tag(name = "会员级别")
@RestController
@RequestMapping("/api/erp/member-level")
@RequiredArgsConstructor
public class MemberLevelController {

    private final IMemberLevelService memberLevelService;

    @Operation(summary = "会员级别列表（下拉/放大镜选择用）")
    @GetMapping("/list")
    public Result<List<MemberLevel>> list(@RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<MemberLevel> w = new LambdaQueryWrapper<>();
        w.eq(MemberLevel::getDeleted, 0);
        if (StringUtils.hasText(keyword)) w.like(MemberLevel::getLevelName, keyword);
        w.orderByAsc(MemberLevel::getSortOrder).orderByAsc(MemberLevel::getId);
        return Result.ok(memberLevelService.list(w));
    }

    @Operation(summary = "新增会员级别")
    @PostMapping
    public Result<MemberLevel> create(@RequestBody MemberLevel body) {
        body.setId(null);
        if (!StringUtils.hasText(body.getLevelName())) return Result.fail("请输入会员级别名称");
        if (body.getStatus() == null) body.setStatus(1);
        memberLevelService.save(body);
        return Result.ok(body);
    }

    @Operation(summary = "更新会员级别")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody MemberLevel body) {
        MemberLevel exist = memberLevelService.getById(id);
        if (exist == null) return Result.fail("会员级别不存在");
        body.setId(id);
        return Result.ok(memberLevelService.updateById(body));
    }

    @Operation(summary = "删除会员级别")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(memberLevelService.removeById(id));
    }
}
