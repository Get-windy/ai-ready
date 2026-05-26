package cn.aiedge.transaction.cdc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DefaultCdcProcessorTest {
    
    @Mock
    private CdcEventPublisher mockPublisher;
    
    @Mock
    private EventSerializer mockSerializer;
    
    private DefaultCdcProcessor processor;
    private DatabaseChangeEvent sampleEvent;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        processor = new DefaultCdcProcessor(mockPublisher, mockSerializer);
        processor.initialize();
        
        // Create a sample event
        sampleEvent = new DatabaseChangeEvent();
        sampleEvent.setEventId("test-event-123");
        sampleEvent.setOperationType(DatabaseChangeEvent.OperationType.INSERT);
        sampleEvent.setTableName("users");
        sampleEvent.setTimestamp(LocalDateTime.now());
        sampleEvent.setSource("test-source");
    }
    
    @Test
    void testInitialize() {
        processor.initialize();
        assertTrue(true); // Initialization should complete without exception
    }
    
    @Test
    void testProcessEventValid() {
        // Process a valid event
        processor.processEvent(sampleEvent);
        
        // Verify that the publisher was called
        verify(mockPublisher, times(1)).publishEvent(any(DatabaseChangeEvent.class));
    }
    
    @Test
    void testProcessEventWithValidation() {
        // Test with a minimal valid event that should be enriched
        DatabaseChangeEvent event = new DatabaseChangeEvent();
        event.setOperationType(DatabaseChangeEvent.OperationType.UPDATE);
        event.setTableName("test_table");
        
        processor.processEvent(event);
        
        // Verify the event was published
        verify(mockPublisher, times(1)).publishEvent(any(DatabaseChangeEvent.class));
    }
    
    @Test
    void testProcessEventNullValidation() {
        // Test with null event (should fail validation)
        DatabaseChangeEvent nullEvent = null;
        
        assertThrows(Exception.class, () -> {
            processor.processEvent(nullEvent);
        });
    }
    
    @Test
    void testProcessEventsBatch() {
        // Create a list of events
        List<DatabaseChangeEvent> events = Arrays.asList(
            createSampleEvent("event-1"),
            createSampleEvent("event-2"),
            createSampleEvent("event-3")
        );
        
        // Process the batch
        processor.processEvents(events);
        
        // Verify that the publisher was called for each event
        verify(mockPublisher, times(3)).publishEvent(any(DatabaseChangeEvent.class));
    }
    
    @Test
    void testGetName() {
        String name = processor.getName();
        assertEquals("DefaultCdcProcessor", name);
    }
    
    @Test
    void testCleanup() {
        processor.cleanup();
        // Just ensure it doesn't throw an exception
        assertTrue(true);
    }
    
    @Test
    void testProcessEventWithErrorHandling() {
        // Configure the mock to throw an exception
        doThrow(new RuntimeException("Test exception")).when(mockPublisher).publishEvent(any(DatabaseChangeEvent.class));
        
        // This should not throw an exception but should handle it internally
        assertDoesNotThrow(() -> processor.processEvent(sampleEvent));
    }
    
    private DatabaseChangeEvent createSampleEvent(String eventId) {
        DatabaseChangeEvent event = new DatabaseChangeEvent();
        event.setEventId(eventId);
        event.setOperationType(DatabaseChangeEvent.OperationType.INSERT);
        event.setTableName("users");
        event.setTimestamp(LocalDateTime.now());
        event.setSource("test-source");
        
        Map<String, Object> primaryKey = new HashMap<>();
        primaryKey.put("id", 1);
        event.setPrimaryKey(primaryKey);
        
        return event;
    }
}