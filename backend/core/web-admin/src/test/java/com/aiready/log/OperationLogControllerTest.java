package com.aiready.log;

import com.aiready.log.dto.OperationLogQueryRequest;
import com.aiready.log.entity.OperationLog;
import com.aiready.log.service.OperationLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class OperationLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OperationLogService operationLogService;

    @Test
    void testQueryLogs() throws Exception {
        // 创建查询请求
        OperationLogQueryRequest request = new OperationLogQueryRequest();
        request.setPageNum(1);
        request.setPageSize(10);

        // 执行POST请求
        mockMvc.perform(post("/api/log/operation/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testGetLogDetail() throws Exception {
        // 先创建一个日志
        OperationLog log = new OperationLog();
        log.setModule("Test Module");
        log.setOperationType("CREATE");
        log.setOperationDesc("Test operation description");
        log.setOperatorName("Test User");
        log.setOperatorIp("127.0.0.1");
        log.setStatus(1);

        operationLogService.saveLog(log);
        Long logId = log.getId();
        assertNotNull(logId);

        // 测试获取日志详情
        mockMvc.perform(get("/api/log/operation/detail/" + logId))
                .andExpect(status().isOk());

        // 清理数据
        operationLogService.removeById(logId);
    }

    @Test
    void testDeleteLog() throws Exception {
        // 先创建一个日志
        OperationLog log = new OperationLog();
        log.setModule("Test Module");
        log.setOperationType("DELETE");
        log.setOperationDesc("Test delete operation");
        log.setOperatorName("Test User");
        log.setOperatorIp("127.0.0.1");
        log.setStatus(1);

        operationLogService.saveLog(log);
        Long logId = log.getId();
        assertNotNull(logId);

        // 测试删除日志
        mockMvc.perform(delete("/api/log/operation/delete/" + logId))
                .andExpect(status().isOk());

        // 验证日志已被删除
        var retrievedLog = operationLogService.getLogDetail(logId);
        assertNull(retrievedLog);
    }

    @Test
    void testGetOperationTypeStats() throws Exception {
        mockMvc.perform(get("/api/log/operation/stats/type"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetModuleStats() throws Exception {
        mockMvc.perform(get("/api/log/operation/stats/module"))
                .andExpect(status().isOk());
    }
}