package cn.aiedge.base.controller;

import cn.aiedge.base.dto.NotificationQuery;
import cn.aiedge.base.dto.NotificationVO;
import cn.aiedge.base.dto.UnreadCountVO;
import cn.aiedge.base.entity.SysMessage;
import cn.aiedge.base.service.MessageService;
import cn.aiedge.base.vo.Result;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知管理控制器
 * 提供通知分页查询、未读统计、标记已读、删除等接口
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "通知管理", description = "系统通知、业务通知、审批通知管理")
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
@SaCheckLogin
public class NotificationController {

    private final MessageService messageService;

    // ── 内部转换方法 ─────────────────────────────────────────

    /**
     * businessType → 通知类型映射
     * null       → 1 (SYSTEM)
     * "approval" → 3 (APPROVAL)
     * 其他        → 2 (BUSINESS)
     */
    private Integer toNotificationType(String businessType) {
        if (businessType == null) return 1;
        if (businessType.contains("approval")) return 3;
        return 2;
    }

    private NotificationVO toNotificationVO(SysMessage msg) {
        NotificationVO vo = new NotificationVO();
        vo.setId(msg.getId());
        vo.setType(toNotificationType(msg.getBusinessType()));
        vo.setTitle(msg.getTitle());
        vo.setContent(msg.getContent());
        vo.setSummary(msg.getContent() != null && msg.getContent().length() > 200
                ? msg.getContent().substring(0, 200) : msg.getContent());
        vo.setSendTime(msg.getCreateTime());
        vo.setReadStatus(msg.getIsRead());
        return vo;
    }

    /**
     * 构建通知查询 Wrapper
     * 仅查询站内信 (msgType=2)，按创建时间倒序
     */
    private LambdaQueryWrapper<SysMessage> buildQueryWrapper(Long userId, NotificationQuery query) {
        LambdaQueryWrapper<SysMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMessage::getReceiverId, userId)
               .eq(SysMessage::getMsgType, 2)
               .orderByDesc(SysMessage::getCreateTime);

        // 通知类型 → businessType 过滤
        if (query.getType() != null) {
            switch (query.getType()) {
                case 1 -> wrapper.isNull(SysMessage::getBusinessType);        // SYSTEM
                case 2 -> wrapper.eq(SysMessage::getBusinessType, "other");    // BUSINESS
                case 3 -> wrapper.eq(SysMessage::getBusinessType, "approval"); // APPROVAL
            }
        }

        if (query.getReadStatus() != null) {
            wrapper.eq(SysMessage::getIsRead, query.getReadStatus());
        }

        return wrapper;
    }

    // ── API 端点 ─────────────────────────────────────────────

    @Operation(summary = "分页查询通知")
    @GetMapping("/page")
    public Result<Page<NotificationVO>> page(NotificationQuery query) {
        Long userId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<SysMessage> wrapper = buildQueryWrapper(userId, query);
        Page<SysMessage> pageParam = new Page<>(query.getPageNum(), query.getPageSize());

        Page<SysMessage> msgPage = messageService.page(pageParam, wrapper);
        Page<NotificationVO> voPage = new Page<>(msgPage.getCurrent(), msgPage.getSize(), msgPage.getTotal());
        voPage.setRecords(msgPage.getRecords().stream().map(this::toNotificationVO).collect(Collectors.toList()));

        return Result.ok(voPage);
    }

    @Operation(summary = "获取通知详情")
    @GetMapping("/{id}")
    public Result<NotificationVO> getById(@PathVariable Long id) {
        SysMessage msg = messageService.getById(id);
        if (msg == null) {
            return Result.fail(404, "通知不存在");
        }
        return Result.ok(toNotificationVO(msg));
    }

    @Operation(summary = "获取通知列表（未读优先，按时间倒序）")
    @GetMapping("/list")
    public Result<List<NotificationVO>> getList(NotificationQuery query) {
        Long userId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<SysMessage> wrapper = buildQueryWrapper(userId, query);
        Page<SysMessage> pageParam = new Page<>(query.getPageNum(), query.getPageSize());

        Page<SysMessage> msgPage = messageService.page(pageParam, wrapper);
        List<SysMessage> records = msgPage.getRecords();

        // 未读优先排序
        records.sort((a, b) -> {
            int readCompare = Integer.compare(
                    a.getIsRead() != null ? a.getIsRead() : 0,
                    b.getIsRead() != null ? b.getIsRead() : 0);
            if (readCompare != 0) return readCompare;
            if (a.getCreateTime() == null && b.getCreateTime() == null) return 0;
            if (a.getCreateTime() == null) return 1;
            if (b.getCreateTime() == null) return -1;
            return b.getCreateTime().compareTo(a.getCreateTime());
        });

        return Result.ok(records.stream().map(this::toNotificationVO).collect(Collectors.toList()));
    }

    @Operation(summary = "获取未读数量统计")
    @GetMapping("/unread-count")
    public Result<UnreadCountVO> getUnreadCount() {
        Long userId = StpUtil.getLoginIdAsLong();
        UnreadCountVO vo = messageService.getUnreadCount(userId);
        return Result.ok(vo);
    }

    @Operation(summary = "标记已读")
    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        messageService.markAsRead(id);
        return Result.ok();
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<SysMessage> unread = messageService.getUnreadMessages(userId);
        List<Long> ids = unread.stream().map(SysMessage::getId).collect(Collectors.toList());
        if (!ids.isEmpty()) {
            messageService.batchMarkAsRead(ids);
        }
        return Result.ok();
    }

    @Operation(summary = "删除通知")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        messageService.removeById(id);
        return Result.ok();
    }

    @Operation(summary = "批量删除通知")
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            messageService.removeByIds(ids);
        }
        return Result.ok();
    }
}
