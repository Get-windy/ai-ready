package cn.aiedge.erp.printing.service.impl;

import cn.aiedge.common.cache.RedisCache;
import cn.aiedge.erp.printing.dto.v2.ScreenshotCreateRequest;
import cn.aiedge.erp.printing.dto.v2.ScreenshotVO;
import cn.aiedge.erp.printing.entity.v2.SysScreenshotTask;
import cn.aiedge.erp.printing.entity.v2.SysPrintTemplate;
import cn.aiedge.erp.printing.engine.FormatEngine;
import cn.aiedge.erp.printing.mapper.SysScreenshotTaskMapper;
import cn.aiedge.erp.printing.mapper.SysPrintTemplateMapper;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.printing.service.ScreenshotService;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScreenshotServiceImpl implements ScreenshotService {

    private final SysScreenshotTaskMapper screenshotMapper;
    private final SysPrintTemplateMapper templateMapper;
    private final FormatEngine formatEngine;
    private final ObjectMapper objectMapper;
    private final RedisCache redisCache;

    /** 截图创建速率限制：每个租户每分钟最多 N 个截图任务 */
    private static final long SCREENSHOT_RATE_LIMIT = 30;
    /** 速率限制 Redis key 前缀 */
    private static final String RATE_LIMIT_KEY_PREFIX = "print:rate:screenshot:";
    /** 截图 base64 最大长度 (10MB base64 ≈ 7.5MB 图片) */
    private static final long MAX_IMAGE_BASE64_LENGTH = 10_485_760;

    @Override
    @Transactional
    public ScreenshotVO createScreenshotTask(ScreenshotCreateRequest request, Long tenantId, Long userId) {
        // 速率限制：每租户每分钟最多创建 SCREENSHOT_RATE_LIMIT 个截图任务
        String rateKey = RATE_LIMIT_KEY_PREFIX + tenantId;
        Long count = redisCache.increment(rateKey);
        if (count == 1) {
            redisCache.expire(rateKey, 1, TimeUnit.MINUTES);
        }
        if (count > SCREENSHOT_RATE_LIMIT) {
            throw BusinessException.badRequest("截图请求过于频繁，请稍后再试");
        }

        // 校验模板存在
        SysPrintTemplate template = templateMapper.selectById(request.getTemplateId());
        if (template == null || !template.getTenantId().equals(tenantId)) {
            throw BusinessException.notFound("模板不存在");
        }

        SysScreenshotTask task = new SysScreenshotTask();
        task.setTaskCode("SS" + IdUtil.fastSimpleUUID().substring(0, 10).toUpperCase());
        task.setTenantId(tenantId);
        task.setTemplateId(request.getTemplateId());
        task.setPageCode(request.getPageCode() != null ? request.getPageCode() : template.getPageCode());

        try {
            task.setDataJson(objectMapper.writeValueAsString(
                    request.getDataJson() != null ? request.getDataJson() : Map.of()));
        } catch (Exception e) {
            task.setDataJson("{}");
        }

        task.setStatus("PENDING");
        task.setCreatedBy(userId);
        screenshotMapper.insert(task);

        log.info("创建截图任务: screenshotId={}, taskCode={}, tenantId={}",
                task.getScreenshotId(), task.getTaskCode(), tenantId);

        return toVO(task);
    }

    @Override
    public ScreenshotVO getScreenshot(Long screenshotId, Long tenantId) {
        SysScreenshotTask task = screenshotMapper.selectById(screenshotId);
        if (task == null || !task.getTenantId().equals(tenantId)) {
            throw BusinessException.notFound("截图任务不存在");
        }
        return toVO(task);
    }

    @Override
    public Page<ScreenshotVO> listScreenshots(Integer page, Integer size, Long tenantId) {
        Page<SysScreenshotTask> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<SysScreenshotTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysScreenshotTask::getTenantId, tenantId)
                .orderByDesc(SysScreenshotTask::getCreatedAt);

        Page<SysScreenshotTask> taskPage = screenshotMapper.selectPage(pageObj, wrapper);
        Page<ScreenshotVO> voPage = new Page<>(taskPage.getCurrent(), taskPage.getSize(), taskPage.getTotal());
        voPage.setRecords(taskPage.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    @Transactional
    public void executeScreenshot(Long screenshotId) {
        SysScreenshotTask task = screenshotMapper.selectById(screenshotId);
        if (task == null) return;

        long start = System.currentTimeMillis();

        try {
            task.setStatus("PROCESSING");
            screenshotMapper.updateById(task);

            // 渲染 HTML（服务端预渲染，最终的截图由前端 html2canvas 或客户端完成）
            String html = formatEngine.renderToHtml(
                    loadTemplateJson(task.getTemplateId()),
                    task.getDataJson()
            );

            log.info("截图任务 {} HTML 渲染完成, 长度={}", screenshotId, html.length());

            // 渲染完成后切回 PENDING，等待用户确认或客户端截图
            // PENDING 状态让调度器可检测 MANUAL_CONFIRM 超时
            task.setStatus("PENDING");
            task.setErrorMessage(null);
            screenshotMapper.updateById(task);

        } catch (Exception e) {
            log.error("截图任务执行失败: {}", screenshotId, e);
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            task.setDurationMs((int) (System.currentTimeMillis() - start));
            screenshotMapper.updateById(task);
        }
    }

    @Override
    @Transactional
    public void completeScreenshot(Long screenshotId, String imageUrl, String imageBase64,
                                    Integer width, Integer height, Long fileSize) {
        SysScreenshotTask task = screenshotMapper.selectById(screenshotId);
        if (task == null) return;

        // 校验 base64 大小限制
        if (StrUtil.isNotBlank(imageBase64) && imageBase64.length() > MAX_IMAGE_BASE64_LENGTH) {
            log.warn("截图 base64 数据过大: screenshotId={}, length={}", screenshotId, imageBase64.length());
            failScreenshot(screenshotId, "截图数据超过大小限制");
            return;
        }

        task.setStatus("COMPLETED");
        task.setImageUrl(imageUrl);
        task.setImageBase64(imageBase64);
        task.setImageWidth(width);
        task.setImageHeight(height);
        task.setFileSize(fileSize);
        task.setDurationMs((int) (java.time.Duration.between(task.getCreatedAt(), LocalDateTime.now()).getSeconds() * 1000));
        screenshotMapper.updateById(task);

        log.info("截图任务 {} 完成, URL={}, size={}x{}", screenshotId, imageUrl, width, height);
    }

    @Override
    @Transactional
    public void failScreenshot(Long screenshotId, String errorMessage) {
        SysScreenshotTask task = screenshotMapper.selectById(screenshotId);
        if (task == null) return;

        task.setStatus("FAILED");
        task.setErrorMessage(errorMessage);
        screenshotMapper.updateById(task);
    }

    @Override
    @Transactional
    public void retryScreenshot(Long screenshotId) {
        SysScreenshotTask task = screenshotMapper.selectById(screenshotId);
        if (task == null) {
            throw BusinessException.notFound("截图任务不存在");
        }
        task.setStatus("PENDING");
        task.setErrorMessage(null);
        task.setImageUrl(null);
        task.setImageBase64(null);
        screenshotMapper.updateById(task);

        log.info("截图任务已重置: screenshotId={}, taskCode={}", screenshotId, task.getTaskCode());
    }

    private String loadTemplateJson(Long templateId) {
        SysPrintTemplate template = templateMapper.selectById(templateId);
        return template != null ? template.getTemplateJson() : "{}";
    }

    private ScreenshotVO toVO(SysScreenshotTask task) {
        ScreenshotVO vo = new ScreenshotVO();
        vo.setScreenshotId(task.getScreenshotId());
        vo.setTaskCode(task.getTaskCode());
        vo.setTenantId(task.getTenantId());
        vo.setTemplateId(task.getTemplateId());
        vo.setPageCode(task.getPageCode());
        vo.setImageUrl(task.getImageUrl());
        vo.setImageWidth(task.getImageWidth());
        vo.setImageHeight(task.getImageHeight());
        vo.setFileSize(task.getFileSize());
        vo.setStatus(task.getStatus());
        vo.setErrorMessage(task.getErrorMessage());
        vo.setDurationMs(task.getDurationMs());
        vo.setCreatedAt(task.getCreatedAt());
        vo.setUpdatedAt(task.getUpdatedAt());
        return vo;
    }
}
