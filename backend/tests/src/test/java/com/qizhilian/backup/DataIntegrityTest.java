package com.qizhilian.backup;

import com.qizhilian.backup.tool.MockBackupTool;
import com.qizhilian.backup.model.IntegrityResult;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据完整性验证测试类
 * TC-BR-009 至 TC-BR-012
 */
@DisplayName("数据完整性验证测试")
public class DataIntegrityTest {
    
    private MockBackupTool backupTool;
    
    @BeforeEach
    void setUp() {
        backupTool = new MockBackupTool();
    }
    
    @Test
    @DisplayName("TC-BR-009: 备份文件校验和验证")
    @Tag("integrity")
    void testBackupIntegrityChecksum() {
        String backupFile = "backup_20260415.sql.gz";
        IntegrityResult result = backupTool.verifyChecksum(backupFile);
        
        assertTrue(result.isPassed(), "校验和验证应该通过");
        assertTrue(result.isChecksumMatch(), "校验和应该匹配");
        assertEquals("SHA256", result.getChecksumAlgorithm(), "校验和算法应该是SHA256");
        
        System.out.println("备份文件校验和验证通过，算法: " + result.getChecksumAlgorithm());
    }
    
    @Test
    @DisplayName("TC-BR-010: 数据行数验证")
    @Tag("integrity")
    void testRowCountVerification() {
        Map<String, Object> sourceDb = new HashMap<>();
        sourceDb.put("type", "mysql");
        sourceDb.put("database", "qizhilian_prod");
        
        Map<String, Object> restoredDb = new HashMap<>();
        restoredDb.put("type", "mysql");
        restoredDb.put("database", "qizhilian_restored");
        
        IntegrityResult result = backupTool.verifyRowCount(sourceDb, restoredDb);
        
        assertTrue(result.isPassed(), "数据行数验证应该通过");
        assertEquals(result.getSourceRows(), result.getRestoredRows(), "源数据和恢复数据行数应该一致");
        
        System.out.println("数据行数验证通过，源数据行数: " + result.getSourceRows() + "，恢复数据行数: " + result.getRestoredRows());
    }
    
    @Test
    @DisplayName("TC-BR-011: 表结构完整性验证")
    @Tag("integrity")
    void testTableStructureIntegrity() {
        Map<String, Object> sourceDb = new HashMap<>();
        sourceDb.put("type", "postgresql");
        sourceDb.put("database", "qizhilian_prod");
        
        Map<String, Object> restoredDb = new HashMap<>();
        restoredDb.put("type", "postgresql");
        restoredDb.put("database", "qizhilian_restored");
        
        IntegrityResult result = backupTool.verifyTableStructure(sourceDb, restoredDb);
        
        assertTrue(result.isPassed(), "表结构完整性验证应该通过");
        assertTrue(result.getTablesCount() > 0, "表数量应该大于0");
        
        System.out.println("表结构完整性验证通过，表数量: " + result.getTablesCount());
    }
    
    @Test
    @DisplayName("TC-BR-012: 外键约束完整性验证")
    @Tag("integrity")
    void testForeignKeyIntegrity() {
        Map<String, Object> restoredDb = new HashMap<>();
        restoredDb.put("type", "mysql");
        restoredDb.put("database", "qizhilian_restored");
        
        IntegrityResult result = backupTool.verifyForeignKeys(restoredDb);
        
        assertTrue(result.isPassed(), "外键约束完整性验证应该通过");
        assertTrue(result.getForeignKeysChecked() > 0, "检查的外键数量应该大于0");
        assertEquals(0, result.getViolations(), "违规记录数量应该为0");
        
        System.out.println("外键约束完整性验证通过，检查外键数: " + result.getForeignKeysChecked() + "，违规记录: " + result.getViolations());
    }
}