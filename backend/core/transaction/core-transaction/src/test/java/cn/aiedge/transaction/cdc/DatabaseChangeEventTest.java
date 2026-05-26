package cn.aiedge.transaction.cdc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseChangeEventTest {
    
    private DatabaseChangeEvent event;
    private Map<String, Object> primaryKey;
    private Map<String, Object> beforeValues;
    private Map<String, Object> afterValues;
    
    @BeforeEach
    void setUp() {
        event = new DatabaseChangeEvent();
        primaryKey = new HashMap<>();
        beforeValues = new HashMap<>();
        afterValues = new HashMap<>();
        
        primaryKey.put("id", 1);
        beforeValues.put("name", "John");
        afterValues.put("name", "Jane");
    }
    
    @Test
    void testEventCreation() {
        event.setEventId("test-id");
        event.setOperationType(DatabaseChangeEvent.OperationType.INSERT);
        event.setTableName("users");
        event.setPrimaryKey(primaryKey);
        event.setBeforeValues(beforeValues);
        event.setAfterValues(afterValues);
        event.setTimestamp(LocalDateTime.now());
        event.setSource("test-source");
        event.setTransactionId("tx-123");
        
        assertEquals("test-id", event.getEventId());
        assertEquals(DatabaseChangeEvent.OperationType.INSERT, event.getOperationType());
        assertEquals("users", event.getTableName());
        assertEquals(primaryKey, event.getPrimaryKey());
        assertEquals(beforeValues, event.getBeforeValues());
        assertEquals(afterValues, event.getAfterValues());
        assertNotNull(event.getTimestamp());
        assertEquals("test-source", event.getSource());
        assertEquals("tx-123", event.getTransactionId());
    }
    
    @Test
    void testOperationTypeEnum() {
        DatabaseChangeEvent.OperationType[] values = DatabaseChangeEvent.OperationType.values();
        
        assertEquals(4, values.length);
        assertAll(
            () -> assertTrue(contains(values, DatabaseChangeEvent.OperationType.INSERT)),
            () -> assertTrue(contains(values, DatabaseChangeEvent.OperationType.UPDATE)),
            () -> assertTrue(contains(values, DatabaseChangeEvent.OperationType.DELETE)),
            () -> assertTrue(contains(values, DatabaseChangeEvent.OperationType.TRUNCATE))
        );
    }
    
    private boolean contains(DatabaseChangeEvent.OperationType[] array, DatabaseChangeEvent.OperationType value) {
        for (DatabaseChangeEvent.OperationType type : array) {
            if (type == value) {
                return true;
            }
        }
        return false;
    }
}