package cn.aiedge.transaction.cdc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Serializes and deserializes DatabaseChangeEvent objects
 */
@Component
public class EventSerializer {
    
    private final ObjectMapper objectMapper;
    
    public EventSerializer() {
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Serialize a DatabaseChangeEvent to JSON string
     * @param event the event to serialize
     * @return JSON string representation of the event
     * @throws JsonProcessingException if serialization fails
     */
    public String serialize(DatabaseChangeEvent event) throws JsonProcessingException {
        return objectMapper.writeValueAsString(event);
    }
    
    /**
     * Deserialize a JSON string to DatabaseChangeEvent
     * @param json the JSON string to deserialize
     * @return DatabaseChangeEvent object
     * @throws JsonProcessingException if deserialization fails
     */
    public DatabaseChangeEvent deserialize(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, DatabaseChangeEvent.class);
    }
    
    /**
     * Serialize a DatabaseChangeEvent to byte array
     * @param event the event to serialize
     * @return byte array representation of the event
     * @throws JsonProcessingException if serialization fails
     */
    public byte[] serializeToBytes(DatabaseChangeEvent event) throws JsonProcessingException {
        return objectMapper.writeValueAsBytes(event);
    }
    
    /**
     * Deserialize a byte array to DatabaseChangeEvent
     * @param bytes the byte array to deserialize
     * @return DatabaseChangeEvent object
     * @throws JsonProcessingException if deserialization fails
     */
    public DatabaseChangeEvent deserializeFromBytes(byte[] bytes) throws JsonProcessingException {
        return objectMapper.readValue(bytes, DatabaseChangeEvent.class);
    }
}