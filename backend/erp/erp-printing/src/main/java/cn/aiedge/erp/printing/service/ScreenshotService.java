package cn.aiedge.erp.printing.service;

import cn.aiedge.erp.printing.dto.v2.ScreenshotCreateRequest;
import cn.aiedge.erp.printing.dto.v2.ScreenshotVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface ScreenshotService {

    ScreenshotVO createScreenshotTask(ScreenshotCreateRequest request, Long tenantId, Long userId);

    ScreenshotVO getScreenshot(Long screenshotId, Long tenantId);

    Page<ScreenshotVO> listScreenshots(Integer page, Integer size, Long tenantId);

    /**
     * 执行截图生成（由异步任务或前端回调调用）
     */
    void executeScreenshot(Long screenshotId);

    /**
     * 前端截图生成后回调，上传截图结果
     */
    void completeScreenshot(Long screenshotId, String imageUrl, String imageBase64,
                            Integer width, Integer height, Long fileSize);

    void failScreenshot(Long screenshotId, String errorMessage);

    void retryScreenshot(Long screenshotId);
}
