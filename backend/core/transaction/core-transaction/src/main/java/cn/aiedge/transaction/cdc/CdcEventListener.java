package cn.aiedge.transaction.cdc;

import cn.aiedge.transaction.entity.DistributedTransactionLog;
import cn.aiedge.transaction.mapper.DistributedTransactionLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Component that listens to database changes and processes them through the CDC module
 * This simulates integration with Debezium or Canal
 */
@Component
@Slf4j
public class CdcEventListener {
    
    @Autowired
    private CdcProcessor cdcProcessor;
    
    @Autowired
    private DistributedTransactionLogMapper transactionLogMapper;
    
    private boolean listening = false;
    
    @PostConstruct
    public void init() {
        log.info("Initializing CDC Event Listener");
        startListening();
    }
    
    @PreDestroy
    public void cleanup() {
        stopListening();
    }
    
    /**
     * Start listening for database changes
     */
    public void startListening() {
        if (!listening) {
            log.info("Starting CDC Event Listener");
            listening = true;
            
            // In a real implementation, this would connect to Debezium/Canal
            // For now, we'll simulate the connection
            log.info("CDC Event Listener connected to database change stream");
        }
    }
    
    /**
     * Stop listening for database changes
     */
    public void stopListening() {
        if (listening) {
            log.info("Stopping CDC Event Listener");
            listening = false;
            log.info("CDC Event Listener disconnected from database change stream");
        }
    }
    
    /**
     * Simulate receiving a database change event from Debezium/Canal
     * @param tableName the name of the table that changed
     * @param operationType the type of operation (INSERT, UPDATE, DELETE)
     * @param primaryKey the primary key of the affected record
     * @param beforeValues the values before the change
     * @param afterValues the values after the change
     * @return DatabaseChangeEvent object
     */
    public DatabaseChangeEvent createAndProcessChangeEvent(
            String tableName, 
            DatabaseChangeEvent.OperationType operationType,
            Map<String, Object> primaryKey,
            Map<String, Object> beforeValues,
            Map<String, Object> afterValues) {
        
        if (!listening) {
            log.warn("CDC Event Listener is not active, ignoring change event");
            return null;
        }
        
        // Create a database change event
        DatabaseChangeEvent event = new DatabaseChangeEvent();
        event.setEventId(java.util.UUID.randomUUID().toString());
        event.setOperationType(operationType);
        event.setTableName(tableName);
        event.setPrimaryKey(primaryKey);
        event.setBeforeValues(beforeValues);
        event.setAfterValues(afterValues);
        event.setTimestamp(LocalDateTime.now());
        event.setSource("CDC-Listener");
        
        // Process the event
        try {
            cdcProcessor.processEvent(event);
            log.debug("Successfully processed database change event for table: {}", tableName);
        } catch (Exception e) {
            log.error("Error processing database change event for table: {}", tableName, e);
            throw e;
        }
        
        return event;
    }
    
    /**
     * Specialized method to handle distributed transaction log changes
     * @param logEntry the transaction log entry that changed
     * @param operationType the type of operation performed
     */
    public void handleTransactionLogChange(DistributedTransactionLog logEntry, DatabaseChangeEvent.OperationType operationType) {
        if (logEntry == null) {
            log.warn("Cannot handle transaction log change: log entry is null");
            return;
        }
        
        // Create a map with the log entry data
        Map<String, Object> values = new HashMap<>();
        values.put("id", logEntry.getId());
        values.put("transactionId", logEntry.getTransactionId());
        values.put("participantId", logEntry.getParticipantId());
        values.put("status", logEntry.getStatus());
        values.put("operation", logEntry.getOperation());
        values.put("rollbackData", logEntry.getRollbackData());
        values.put("createTime", logEntry.getCreateTime());
        values.put("updateTime", logEntry.getUpdateTime());
        values.put("version", logEntry.getVersion());
        
        // Create primary key map
        Map<String, Object> primaryKey = new HashMap<>();
        primaryKey.put("id", logEntry.getId());
        
        // Determine before/after values based on operation type
        Map<String, Object> beforeValues = null;
        Map<String, Object> afterValues = null;
        
        if (operationType == DatabaseChangeEvent.OperationType.INSERT) {
            afterValues = values;
        } else if (operationType == DatabaseChangeEvent.OperationType.DELETE) {
            beforeValues = values;
        } else if (operationType == DatabaseChangeEvent.OperationType.UPDATE) {
            beforeValues = getPreviousValues(logEntry.getId()); // Retrieve previous values from DB
            afterValues = values;
        }
        
        // Create and process the change event
        createAndProcessChangeEvent(
            "distributed_transaction_log",
            operationType,
            primaryKey,
            beforeValues,
            afterValues
        );
    }
    
    /**
     * Retrieve previous values of a transaction log entry before an update
     * @param logId the ID of the log entry
     * @return map containing previous values
     */
    private Map<String, Object> getPreviousValues(Long logId) {
        try {
            QueryWrapper<DistributedTransactionLog> wrapper = new QueryWrapper<>();
            wrapper.eq("id", logId);
            DistributedTransactionLog previousLog = transactionLogMapper.selectOne(wrapper);
            
            if (previousLog != null) {
                Map<String, Object> values = new HashMap<>();
                values.put("id", previousLog.getId());
                values.put("transactionId", previousLog.getTransactionId());
                values.put("participantId", previousLog.getParticipantId());
                values.put("status", previousLog.getStatus());
                values.put("operation", previousLog.getOperation());
                values.put("rollbackData", previousLog.getRollbackData());
                values.put("createTime", previousLog.getCreateTime());
                values.put("updateTime", previousLog.getUpdateTime());
                values.put("version", previousLog.getVersion());
                
                return values;
            }
        } catch (Exception e) {
            log.error("Error retrieving previous values for log ID: {}", logId, e);
        }
        
        return new HashMap<>(); // Return empty map if retrieval fails
    }
    
    /**
     * Check if the listener is currently active
     * @return true if listening, false otherwise
     */
    public boolean isListening() {
        return listening;
    }
}