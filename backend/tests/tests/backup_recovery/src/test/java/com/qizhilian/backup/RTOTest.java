package com.qizhilian.backup;

import com.qizhilian.backup.tool.MockBackupTool;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 恢复时间目标(RTO)测试类
 * TC-BR-013 至 TC-BR-015
 */
@DisplayName("恢复时间目标(RTO)测试")
public class RTOTest {
    
    private MockBackupTool backupTool;
    
    // RTO目标值（秒）
    private static final long FULL_RESTORE_RTO_TARGET = 3600;  // 1小时
    private static final long INCREMENTAL_RESTORE_RTO_TARGET = 600;  // 10分钟
    private static final long DISASTER_RECOVERY_RTO_TARGET = 7200;  // 2小时
    
    @BeforeEach
    void setUp() {
        backupTool = new MockBackupTool();
    }
    
    @Test
    @DisplayName("TC-BR-013: 全量恢复RTO测试")
    @Tag("rto")
    void testRTOFullRestore() {
        Map<String, Object> targetDb = new HashMap<>();
        targetDb.put("type", "mysql");
        targetDb.put("host", "localhost");
        
        long actualTime = backupTool.measureFullRestoreRTO(targetDb);
        
        assertTrue(actualTime <= FULL_RESTORE_RTO_TARGET, 
            "全量恢复时间应该满足RTO目标。目标: " + FULL_RESTORE_RTO_TARGET + "s，实际: " + actualTime + "s");
        
        System.out.println("全量恢复RTO达标，目标: " + FULL_RESTORE_RTO_TARGET + "s，实际: " + actualTime + "s");
    }
    
    @Test
    @DisplayName("TC-BR-014: 增量恢复RTO测试")
    @Tag("rto")
    void testRTOIncrementalRestore() {
        Map<String, Object> targetDb = new HashMap<>();
        targetDb.put("type", "mysql");
        targetDb.put("host", "localhost");
        
        long actualTime = backupTool.measureIncrementalRestoreRTO(targetDb);
        
        assertTrue(actualTime <= INCREMENTAL_RESTORE_RTO_TARGET, 
            "增量恢复时间应该满足RTO目标。目标: " + INCREMENTAL_RESTORE_RTO_TARGET + "s，实际: " + actualTime + "s");
        
        System.out.println("增量恢复RTO达标，目标: " + INCREMENTAL_RESTORE_RTO_TARGET + "s，实际: " + actualTime + "s");
    }
    
    @Test
    @DisplayName("TC-BR-015: 灾难恢复RTO测试")
    @Tag("rto")
    void testRTODisasterRecovery() {
        long actualTime = backupTool.measureDisasterRecoveryRTO();
        
        assertTrue(actualTime <= DISASTER_RECOVERY_RTO_TARGET, 
            "灾难恢复时间应该满足RTO目标。目标: " + DISASTER_RECOVERY_RTO_TARGET + "s，实际: " + actualTime + "s");
        
        System.out.println("灾难恢复RTO达标，目标: " + DISASTER_RECOVERY_RTO_TARGET + "s，实际: " + actualTime + "s");
    }
}