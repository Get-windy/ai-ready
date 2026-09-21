package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.dto.GroupBuyActivityRowVO;
import cn.aiedge.erp.stock.dto.GroupBuyOrderRowVO;
import cn.aiedge.erp.stock.entity.GroupBuyActivity;
import cn.aiedge.erp.stock.entity.GroupBuyParticipant;
import cn.aiedge.erp.stock.mapper.GroupBuyParticipantMapper;
import cn.aiedge.erp.stock.mapper.GroupBuyQueryMapper;
import cn.aiedge.erp.stock.service.GroupBuyActivityService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import cn.dev33.satoken.annotation.SaCheckPermission;

@Slf4j
@Tag(name = "团购管理")
@RestController
@RequestMapping("/api/erp/marketing/group-buy")
@RequiredArgsConstructor
public class GroupBuyController {

    private final GroupBuyActivityService activityService;
    private final GroupBuyParticipantMapper participantMapper;
    private final GroupBuyQueryMapper groupBuyQueryMapper;

    @Operation(summary = "分页查询团购活动")
    @SaCheckPermission("marketing:group-buy:list")
    @GetMapping("/page")
    public Result<IPage<GroupBuyActivity>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(activityService.page(new Page<>(pageNum, pageSize),
                new QueryWrapper<GroupBuyActivity>().eq("deleted", 0).orderByDesc("create_time")));
    }

    @Operation(summary = "商城拼团 →「拼团活动」Tab（9 列，含开团/成功团个数）")
    @SaCheckPermission("marketing:group-buy:list")
    @GetMapping("/activity/page")
    public Result<IPage<GroupBuyActivityRowVO>> activityPage(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long tenantId = cn.aiedge.base.utils.SecurityUtils.getCurrentTenantId();
        if (tenantId == null) tenantId = 1L;
        IPage<GroupBuyActivityRowVO> page = groupBuyQueryMapper.selectActivityPage(
                new Page<>(pageNum, pageSize), tenantId, name, status);
        page.getRecords().forEach(r -> r.setGroupType(formatGroupType(r.getMinGroupSize(), r.getMaxGroupSize())));
        return Result.ok(page);
    }

    @Operation(summary = "商城拼团 →「拼团订单」Tab（11 列）")
    @SaCheckPermission("marketing:group-buy:list")
    @GetMapping("/order/page")
    public Result<IPage<GroupBuyOrderRowVO>> orderPage(
            @RequestParam(required = false) String groupId,
            @RequestParam(required = false) String customer,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String groupStatus,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long tenantId = cn.aiedge.base.utils.SecurityUtils.getCurrentTenantId();
        if (tenantId == null) tenantId = 1L;
        return Result.ok(groupBuyQueryMapper.selectOrderPage(
                new Page<>(pageNum, pageSize), tenantId, groupId, customer, orderNo, groupStatus));
    }

    /** 成团类型：由 min/max 成团人数派生（相等=固定人数团，不等=区间团） */
    private String formatGroupType(Integer min, Integer max) {
        if (min == null) return "";
        if (max == null || max.equals(min)) return min + "人团";
        return min + "~" + max + "人团";
    }

    @Operation(summary = "查询团购详情")
    @SaCheckPermission("marketing:group-buy:detail")
    @GetMapping("/{id}")
    public Result<GroupBuyActivity> getById(@PathVariable Long id) {
        return Result.ok(activityService.getById(id));
    }

    @Operation(summary = "新建团购活动")
    @SaCheckPermission("marketing:group-buy:create")
    @PostMapping
    public Result<Boolean> create(@RequestBody GroupBuyActivity activity) {
        return Result.ok(activityService.save(activity));
    }

    @Operation(summary = "更新团购活动")
    @SaCheckPermission("marketing:group-buy:update")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody GroupBuyActivity activity) {
        activity.setId(id);
        return Result.ok(activityService.updateById(activity));
    }

    @Operation(summary = "变更活动状态")
    @SaCheckPermission("marketing:group-buy:update")
    @PutMapping("/{id}/status")
    public Result<Boolean> updateStatus(@PathVariable Long id, @RequestParam String status) {
        GroupBuyActivity a = new GroupBuyActivity();
        a.setId(id);
        a.setStatus(status);
        return Result.ok(activityService.updateById(a));
    }

    @Operation(summary = "查询参与记录")
    @SaCheckPermission("marketing:group-buy:view")
    @GetMapping("/{id}/participants")
    public Result<List<GroupBuyParticipant>> getParticipants(@PathVariable Long id) {
        return Result.ok(participantMapper.selectList(
                new QueryWrapper<GroupBuyParticipant>().eq("activity_id", id).eq("deleted", 0)));
    }
}
