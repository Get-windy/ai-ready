package cn.aiedge.transaction.cdc;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

class EventSerializerTest {
    
    private EventSerializer serializer;
    private DatabaseChangeEvent event;
    
    @BeforeEach
    void setUp() {
        serializer = new EventSerializer();
        event = createSampleEvent();
    }
    
    @Test
    void testSerializeDeserialize() throws JsonProcessingException {
        String json = serializer.serialize(event);
        assertNotNull(json);
        assertFalse(json.isEmpty());
        
        DatabaseChangeEvent deserializedEvent = serializer.deserialize(json);
        assertNotNull(deserializedEvent);
        
        // Compare key properties
        assertEquals(event.getEventId(), deserializedEvent.getEventId());
        assertEquals(event.getOperationType(), deserializedEvent.getOperationType());
        assertEquals(event.getTableName(), deserializedEvent.getTableName());
        assertEquals(event.getPrimaryKey(), deserializedEvent.getPrimaryKey());
        assertEquals(event.getBeforeValues(), deserializedEvent.getBeforeValues());
        assertEquals(event.getAfterValues(), deserializedEvent.getAfterValues());
        assertEquals(event.getSource(), deserializedEvent.getSource());
        assertEquals(event.getTransactionId(), deserializedEvent.getTransactionId());
    }
    
    @Test
    void testSerializeToBytesDeserializeFromBytes() throws JsonProcessingException {
        byte[] bytes = serializer.serializeToBytes(event);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
        
        DatabaseChangeEvent deserializedEvent = serializer.deserializeFromBytes(bytes);
        assertNotNull(deserializedEvent);
        
        // Compare key properties
        assertEquals(event.getEventId(), deserializedEvent.getEventId());
        assertEquals(event.getOperationType(), deserializedEvent.getOperationType());
        assertEquals(event.getTableName(), deserializedEvent.getTableName());
    }
    
    @Test
    void testSerializeNullEvent() {
        assertThrows(JsonProcessingException.class, () -> {
            serializer.serialize(null);
        });
    }
    
    @Test
    void testDeserializeInvalidJson() {
        assertThrows(com.fasterxml.jackson.core.JsonParseException.class, () -> {
            serializer.deserialize("invalid json");
        });
    }
    
    @Test
    void testRoundTripSerialization() throws JsonProcessingException {
        // Serialize to string
        String jsonString = serializer.serialize(event);
        assertNotNull(jsonString);
        
        // Deserialize from string
        DatabaseChangeEvent fromJson = serializer.deserialize(jsonString);
        assertNotNull(fromJson);
        
        // Serialize again
        String jsonString2 = serializer.serialize(fromJson);
        assertNotNull(jsonString2);
        
        // Both serialized strings should be equivalent
        assertEquals(jsonString, jsonString2);
    }
    
    private DatabaseChangeEvent createSampleEvent() {
        DatabaseChangeEvent event = new DatabaseChangeEvent();
        event.setEventId("test-event-123");
        event.setOperationType(DatabaseChangeEvent.OperationType.UPDATE);
        event.setTableName("users");
        
        Map<String, Object> primaryKey = new HashMap<>();
        primaryKey.put("id", 123);
        event.setPrimaryKey(primaryKey);
        
        Map<String, Object> beforeValues = new HashMap<>();
        beforeValues.put("name", "John Doe");
        beforeValues.put("email", "john@example.com");
        event.setBeforeValues(beforeValues);
        
        Map<String, Object> afterValues = new HashMap<>();
        afterValues.put("name", "Jane Doe");
        afterValues.put("email", "jane@example.com");
        event.setAfterValues(afterValues);
        
        event.setTimestamp(LocalDateTime.now());
        event.setSource("test-source");
        event.setTransactionId("tx-456");
        
        return event;
    }
}