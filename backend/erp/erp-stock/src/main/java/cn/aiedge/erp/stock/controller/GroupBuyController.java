package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.GroupBuyActivity;
import cn.aiedge.erp.stock.entity.GroupBuyParticipant;
import cn.aiedge.erp.stock.mapper.GroupBuyParticipantMapper;
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

@Slf4j
@Tag(name = "团购管理")
@RestController
@RequestMapping("/api/erp/marketing/group-buy")
@RequiredArgsConstructor
public class GroupBuyController {

    private final GroupBuyActivityService activityService;
    private final GroupBuyParticipantMapper participantMapper;

    @Operation(summary = "分页查询团购活动")
    @GetMapping("/page")
    public Result<IPage<GroupBuyActivity>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(activityService.page(new Page<>(pageNum, pageSize),
                new QueryWrapper<GroupBuyActivity>().eq("deleted", 0).orderByDesc("create_time")));
    }

    @Operation(summary = "查询团购详情")
    @GetMapping("/{id}")
    public Result<GroupBuyActivity> getById(@PathVariable Long id) {
        return Result.ok(activityService.getById(id));
    }

    @Operation(summary = "新建团购活动")
    @PostMapping
    public Result<Boolean> create(@RequestBody GroupBuyActivity activity) {
        return Result.ok(activityService.save(activity));
    }

    @Operation(summary = "更新团购活动")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody GroupBuyActivity activity) {
        activity.setId(id);
        return Result.ok(activityService.updateById(activity));
    }

    @Operation(summary = "变更活动状态")
    @PutMapping("/{id}/status")
    public Result<Boolean> updateStatus(@PathVariable Long id, @RequestParam String status) {
        GroupBuyActivity a = new GroupBuyActivity();
        a.setId(id);
        a.setStatus(status);
        return Result.ok(activityService.updateById(a));
    }

    @Operation(summary = "查询参与记录")
    @GetMapping("/{id}/participants")
    public Result<List<GroupBuyParticipant>> getParticipants(@PathVariable Long id) {
        return Result.ok(participantMapper.selectList(
                new QueryWrapper<GroupBuyParticipant>().eq("activity_id", id).eq("deleted", 0)));
    }
}
