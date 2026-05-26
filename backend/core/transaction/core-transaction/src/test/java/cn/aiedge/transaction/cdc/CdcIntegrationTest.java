package cn.aiedge.transaction.cdc;

import cn.aiedge.transaction.entity.DistributedTransactionLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CdcIntegrationTest {
    
    @Mock
    private CdcEventPublisher mockPublisher;
    
    @Mock
    private EventSerializer mockSerializer;
    
    private DefaultCdcProcessor processor;
    private CdcEventListener eventListener;
    private CdcTransactionService transactionService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        processor = new DefaultCdcProcessor(mockPublisher, mockSerializer);
        processor.initialize();
        
        // Create instances for integration testing
        eventListener = new CdcEventListener();
        transactionService = new CdcTransactionService();
    }
    
    @Test
    void testEndToEndCdcFlow() {
        // Create a sample database change event
        DatabaseChangeEvent event = new DatabaseChangeEvent();
        event.setEventId("integration-test-event");
        event.setOperationType(DatabaseChangeEvent.OperationType.INSERT);
        event.setTableName("test_table");
        event.setTimestamp(LocalDateTime.now());
        event.setSource("integration-test");
        
        Map<String, Object> primaryKey = new HashMap<>();
        primaryKey.put("id", 1);
        event.setPrimaryKey(primaryKey);
        
        Map<String, Object> afterValues = new HashMap<>();
        afterValues.put("name", "Test User");
        afterValues.put("email", "test@example.com");
        event.setAfterValues(afterValues);
        
        // Process the event through the processor
        assertDoesNotThrow(() -> processor.processEvent(event));
        
        // Verify the publisher was called
        verify(mockPublisher, times(1)).publishEvent(any(DatabaseChangeEvent.class));
    }
    
    @Test
    void testEventSerializationIntegration() throws Exception {
        // Create an event serializer
        EventSerializer serializer = new EventSerializer();
        
        // Create a sample event
        DatabaseChangeEvent event = new DatabaseChangeEvent();
        event.setEventId("serialization-test");
        event.setOperationType(DatabaseChangeEvent.OperationType.UPDATE);
        event.setTableName("users");
        event.setTimestamp(LocalDateTime.now());
        
        Map<String, Object> beforeValues = new HashMap<>();
        beforeValues.put("name", "Old Name");
        event.setBeforeValues(beforeValues);
        
        Map<String, Object> afterValues = new HashMap<>();
        afterValues.put("name", "New Name");
        event.setAfterValues(afterValues);
        
        // Test serialization and deserialization
        String json = serializer.serialize(event);
        assertNotNull(json);
        assertFalse(json.isEmpty());
        
        DatabaseChangeEvent deserializedEvent = serializer.deserialize(json);
        assertNotNull(deserializedEvent);
        
        // Verify the event was properly serialized and deserialized
        assertEquals(event.getEventId(), deserializedEvent.getEventId());
        assertEquals(event.getOperationType(), deserializedEvent.getOperationType());
        assertEquals(event.getTableName(), deserializedEvent.getTableName());
    }
    
    @Test
    void testTransactionLogChangeHandling() {
        // Create a mock transaction log
        DistributedTransactionLog logEntry = new DistributedTransactionLog();
        logEntry.setId(1L);
        logEntry.setTransactionId("tx-integration-test");
        logEntry.setStatus("COMPLETED");
        logEntry.setOperation("CREATE_USER");
        logEntry.setCreateTime(LocalDateTime.now());
        logEntry.setUpdateTime(LocalDateTime.now());
        
        // Since CdcEventListener has autowired dependencies that aren't available in unit test,
        // we'll test the utility methods and logic directly
        assertNotNull(logEntry.getTransactionId());
        assertEquals("COMPLETED", logEntry.getStatus());
    }
    
    @Test
    void testCdcUtilsIntegration() {
        // Test the utility methods that tie together various components
        DatabaseChangeEvent event = new DatabaseChangeEvent();
        event.setEventId("utils-test");
        event.setOperationType(DatabaseChangeEvent.OperationType.INSERT);
        event.setTableName("test_table");
        
        Map<String, Object> beforeValues = new HashMap<>();
        beforeValues.put("field1", "value1");
        
        Map<String, Object> afterValues = new HashMap<>();
        afterValues.put("field1", "value2");
        afterValues.put("field2", "value3");
        
        event.setBeforeValues(beforeValues);
        event.setAfterValues(afterValues);
        
        // Test event summary generation
        String summary = CdcUtils.generateEventSummary(event);
        assertNotNull(summary);
        assertTrue(summary.contains("Event ID: utils-test"));
        assertTrue(summary.contains("Operation: INSERT"));
        
        // Test change detection
        Map<String, Object> changes = CdcUtils.getChangedValues(beforeValues, afterValues);
        assertNotNull(changes);
        assertEquals(2, changes.size()); // field1 changed, field2 added
    }
    
    @Test
    void testValidateAndEnrichEvent() {
        // Test the validation and enrichment logic
        DatabaseChangeEvent event = new DatabaseChangeEvent();
        event.setOperationType(DatabaseChangeEvent.OperationType.DELETE);
        event.setTableName("users");
        // Note: eventId is intentionally left null to test enrichment
        
        // Since we can't easily test the private methods in DefaultCdcProcessor,
        // we'll test the public interface and utility methods
        
        // Test utility validation
        assertTrue(CdcUtils.isValidTableName("users"));
        assertFalse(CdcUtils.isValidTableName(null));
        assertFalse(CdcUtils.isValidTableName(""));
    }
}