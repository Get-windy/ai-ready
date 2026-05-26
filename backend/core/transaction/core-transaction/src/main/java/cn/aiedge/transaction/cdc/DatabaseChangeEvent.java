package cn.aiedge.transaction.cdc;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Database Change Event - Represents a database change captured by CDC
 */
@Data
public class DatabaseChangeEvent {
    
    /**
     * Unique identifier for the change event
     */
    private String eventId;
    
    /**
     * Type of database operation (INSERT, UPDATE, DELETE)
     */
    private OperationType operationType;
    
    /**
     * Name of the table where the change occurred
     */
    private String tableName;
    
    /**
     * Primary key value(s) of the affected record
     */
    private Map<String, Object> primaryKey;
    
    /**
     * Old values before the change (for UPDATE/DELETE operations)
     */
    private Map<String, Object> beforeValues;
    
    /**
     * New values after the change (for INSERT/UPDATE operations)
     */
    private Map<String, Object> afterValues;
    
    /**
     * Timestamp when the change was captured
     */
    private LocalDateTime timestamp;
    
    /**
     * Source of the change (database name, instance, etc.)
     */
    private String source;
    
    /**
     * Transaction ID associated with the change
     */
    private String transactionId;
    
    public enum OperationType {
        INSERT,
        UPDATE,
        DELETE,
        TRUNCATE
    }
}