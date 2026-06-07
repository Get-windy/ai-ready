package cn.aiedge.base.service;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.glue.GlueTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * XXL-Job 任务管理服务
 * 提供对调度任务的高级管理功能
 * 
 * 注意：XXL-Job 2.4.0 版本中 AdminBiz 接口不再包含 triggerJob/killJob 方法，
 * 这些操作需要通过调度中心的 HTTP API 实现。
 */
@Service
public class XxlJobManagementService {

    private static final Logger logger = LoggerFactory.getLogger(XxlJobManagementService.class);

    @Value("${xxl.job.admin.addresses:http://127.0.0.1:5655/xxl-job-admin}")
    private String adminAddresses;

    @Value("${xxl.job.accessToken:default_token}")
    private String accessToken;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 手动触发任务执行
     * 通过调用 XXL-Job Admin 的 HTTP API 触发任务
     *
     * @param jobId 任务ID
     * @param executorParam 执行参数
     * @return 执行结果
     */
    public ReturnT<String> triggerJob(int jobId, String executorParam) {
        try {
            // 构造触发请求URL
            String triggerUrl = adminAddresses + "/jobinfo/trigger";
            
            // 构造请求参数
            Map<String, Object> params = new HashMap<>();
            params.put("id", jobId);
            params.put("executorParam", executorParam);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("XXL-JOB-ACCESS-TOKEN", accessToken);
            
            // 构造请求体
            String body = "id=" + jobId + "&executorParam=" + executorParam;
            HttpEntity<String> request = new HttpEntity<>(body, headers);
            
            // 发送请求
            ResponseEntity<String> response = restTemplate.postForEntity(triggerUrl, request, String.class);
            
            logger.info("手动触发任务执行，任务ID: {}, 参数: {}, 结果: {}", jobId, executorParam, response.getBody());
            
            return new ReturnT<>(ReturnT.SUCCESS_CODE, response.getBody());
        } catch (Exception e) {
            logger.error("手动触发任务失败，任务ID: " + jobId, e);
            return new ReturnT<>(ReturnT.FAIL_CODE, "触发任务失败: " + e.getMessage());
        }
    }

    /**
     * 检查任务执行状态
     * 
     * @param jobId 任务ID
     * @return 任务状态
     */
    public boolean checkJobExists(int jobId) {
        try {
            // 这里可以实现检查任务是否存在的逻辑
            logger.debug("检查任务是否存在，任务ID: {}", jobId);
            return true; // 简化实现
        } catch (Exception e) {
            logger.error("检查任务存在性失败，任务ID: " + jobId, e);
            return false;
        }
    }

    /**
     * 停止正在运行的任务
     * 通过调用 XXL-Job Admin 的 HTTP API 停止任务
     *
     * @param jobId 任务ID
     * @return 操作结果
     */
    public ReturnT<String> killJob(int jobId) {
        try {
            // 构造停止请求URL
            String stopUrl = adminAddresses + "/joblog/kill";
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("XXL-JOB-ACCESS-TOKEN", accessToken);
            
            // 构造请求体
            String body = "id=" + jobId;
            HttpEntity<String> request = new HttpEntity<>(body, headers);
            
            // 发送请求
            ResponseEntity<String> response = restTemplate.postForEntity(stopUrl, request, String.class);
            
            logger.info("停止任务执行，任务ID: {}, 结果: {}", jobId, response.getBody());
            
            return new ReturnT<>(ReturnT.SUCCESS_CODE, response.getBody());
        } catch (Exception e) {
            logger.error("停止任务失败，任务ID: " + jobId, e);
            return new ReturnT<>(ReturnT.FAIL_CODE, "停止任务失败: " + e.getMessage());
        }
    }

    /**
     * 获取任务调度报表
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 调度报表
     */
    public String getScheduleReport(String startDate, String endDate) {
        logger.info("获取调度报表，开始日期: {}, 结束日期: {}", startDate, endDate);
        return "Schedule report from " + startDate + " to " + endDate;
    }
}