package cn.aiedge.transaction.cdc;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

class CdcUtilsTest {
    
    @Test
    void testIsValidTableName() {
        assertTrue(CdcUtils.isValidTableName("users"));
        assertTrue(CdcUtils.isValidTableName("user_data"));
        assertTrue(CdcUtils.isValidTableName("user123"));
        assertTrue(CdcUtils.isValidTableName("User_Data_123"));
        assertTrue(CdcUtils.isValidTableName("a")); // minimum valid name
        
        assertFalse(CdcUtils.isValidTableName(null));
        assertFalse(CdcUtils.isValidTableName(""));
        assertFalse(CdcUtils.isValidTableName(" "));
        assertFalse(CdcUtils.isValidTableName("user table")); // contains space
        assertFalse(CdcUtils.isValidTableName("user-table")); // contains hyphen (though underscore is allowed)
        assertFalse(CdcUtils.isValidTableName("123user")); // starts with number
        assertFalse(CdcUtils.isValidTableName("user;DROP TABLE users;")); // SQL injection attempt
    }
    
    @Test
    void testSanitizeTableName() {
        assertEquals("users", CdcUtils.sanitizeTableName("users"));
        assertEquals("user_data", CdcUtils.sanitizeTableName("user_data"));
        assertEquals("user123", CdcUtils.sanitizeTableName("user123"));
        
        assertNull(CdcUtils.sanitizeTableName(null));
        assertNull(CdcUtils.sanitizeTableName(""));
        assertNull(CdcUtils.sanitizeTableName("user table")); // invalid input returns null
    }
    
    @Test
    void testDeepCopy() {
        Map<String, Object> original = new HashMap<>();
        original.put("id", 1);
        original.put("name", "John");
        original.put("active", true);
        
        Map<String, Object> copy = CdcUtils.deepCopy(original);
        
        assertNotNull(copy);
        assertEquals(original.size(), copy.size());
        assertEquals(original.get("id"), copy.get("id"));
        assertEquals(original.get("name"), copy.get("name"));
        assertEquals(original.get("active"), copy.get("active"));
        
        // Modify original and ensure copy is unaffected
        original.put("id", 2);
        assertNotEquals(original.get("id"), copy.get("id"));
    }
    
    @Test
    void testDeepCopyWithNull() {
        assertNull(CdcUtils.deepCopy(null));
    }
    
    @Test
    void testGetChangedValues() {
        Map<String, Object> before = new HashMap<>();
        before.put("id", 1);
        before.put("name", "John");
        before.put("email", "john@example.com");
        
        Map<String, Object> after = new HashMap<>();
        after.put("id", 1);
        after.put("name", "Jane"); // Changed
        after.put("email", "john@example.com"); // Same
        after.put("age", 30); // Added
        
        Map<String, Object> changes = CdcUtils.getChangedValues(before, after);
        
        assertEquals(2, changes.size());
        assertEquals("Jane", changes.get("name")); // Changed value
        assertEquals(30, changes.get("age")); // Added value
        assertNull(changes.get("email")); // Unchanged value not in changes
    }
    
    @Test
    void testGetChangedValuesBothNull() {
        Map<String, Object> changes = CdcUtils.getChangedValues(null, null);
        assertNotNull(changes);
        assertTrue(changes.isEmpty());
    }
    
    @Test
    void testGetChangedValuesBeforeNull() {
        Map<String, Object> after = new HashMap<>();
        after.put("name", "John");
        
        Map<String, Object> changes = CdcUtils.getChangedValues(null, after);
        assertEquals(after, changes);
    }
    
    @Test
    void testGetChangedValuesAfterNull() {
        Map<String, Object> before = new HashMap<>();
        before.put("name", "John");
        
        Map<String, Object> changes = CdcUtils.getChangedValues(before, null);
        assertNotNull(changes);
        assertTrue(changes.isEmpty());
    }
    
    @Test
    void testAreMapsEqual() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("id", 1);
        map1.put("name", "John");
        
        Map<String, Object> map2 = new HashMap<>();
        map2.put("id", 1);
        map2.put("name", "John");
        
        Map<String, Object> map3 = new HashMap<>();
        map3.put("id", 1);
        map3.put("name", "Jane");
        
        assertTrue(CdcUtils.areMapsEqual(map1, map2)); // Equal maps
        assertFalse(CdcUtils.areMapsEqual(map1, map3)); // Different maps
        assertTrue(CdcUtils.areMapsEqual(null, null)); // Both null
        assertFalse(CdcUtils.areMapsEqual(map1, null)); // One null
        assertFalse(CdcUtils.areMapsEqual(null, map2)); // One null
    }
    
    @Test
    void testGenerateEventSummary() {
        DatabaseChangeEvent event = new DatabaseChangeEvent();
        event.setEventId("event-123");
        event.setOperationType(DatabaseChangeEvent.OperationType.INSERT);
        event.setTableName("users");
        event.setSource("test-source");
        
        String summary = CdcUtils.generateEventSummary(event);
        
        assertNotNull(summary);
        assertTrue(summary.contains("Event ID: event-123"));
        assertTrue(summary.contains("Operation: INSERT"));
        assertTrue(summary.contains("Table: users"));
        assertTrue(summary.contains("Source: test-source"));
    }
}