package cn.aiedge.transaction.cdc;

import cn.aiedge.transaction.entity.DistributedTransactionLog;
import cn.aiedge.transaction.model.TransactionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service that integrates CDC functionality with transaction management
 * Monitors transaction-related database changes and processes them accordingly
 */
@Service
@Slf4j
public class CdcTransactionService {
    
    @Autowired
    private CdcEventListener cdcEventListener;
    
    @Autowired
    private CdcProcessor cdcProcessor;
    
    /**
     * Monitor and process changes to transaction logs
     * @param logEntries the transaction log entries that changed
     * @param operationType the type of operation performed
     */
    public void processTransactionLogChanges(List<DistributedTransactionLog> logEntries, 
                                           DatabaseChangeEvent.OperationType operationType) {
        log.info("Processing {} transaction log changes of type: {}", 
                 logEntries.size(), operationType);
        
        for (DistributedTransactionLog logEntry : logEntries) {
            try {
                cdcEventListener.handleTransactionLogChange(logEntry, operationType);
            } catch (Exception e) {
                log.error("Error processing transaction log change for ID: {}", 
                         logEntry.getId(), e);
            }
        }
    }
    
    /**
     * Process a single transaction log change
     * @param logEntry the transaction log entry that changed
     * @param operationType the type of operation performed
     */
    public void processTransactionLogChange(DistributedTransactionLog logEntry, 
                                          DatabaseChangeEvent.OperationType operationType) {
        log.debug("Processing transaction log change - ID: {}, Operation: {}", 
                  logEntry.getId(), operationType);
        
        try {
            cdcEventListener.handleTransactionLogChange(logEntry, operationType);
        } catch (Exception e) {
            log.error("Error processing transaction log change for ID: {}", 
                     logEntry.getId(), e);
            throw e;
        }
    }
    
    /**
     * Process a custom database change event related to transactions
     * @param tableName the name of the table that changed
     * @param operationType the type of operation performed
     * @param primaryKey the primary key of the affected record
     * @param beforeValues the values before the change
     * @param afterValues the values after the change
     */
    public DatabaseChangeEvent processCustomTransactionChange(
            String tableName,
            DatabaseChangeEvent.OperationType operationType,
            Map<String, Object> primaryKey,
            Map<String, Object> beforeValues,
            Map<String, Object> afterValues) {
        
        log.info("Processing custom transaction change - Table: {}, Operation: {}", 
                 tableName, operationType);
        
        return cdcEventListener.createAndProcessChangeEvent(
            tableName,
            operationType,
            primaryKey,
            beforeValues,
            afterValues
        );
    }
    
    /**
     * Process changes related to a specific transaction context
     * @param context the transaction context
     * @param operationType the type of operation performed
     */
    public void processTransactionContextChange(TransactionContext context, 
                                             DatabaseChangeEvent.OperationType operationType) {
        if (context == null) {
            log.warn("Cannot process transaction context change: context is null");
            return;
        }
        
        log.debug("Processing transaction context change - Transaction ID: {}", 
                  context.getTransactionId());
        
        // Create a map with context data
        Map<String, Object> values = Map.of(
            "transactionId", context.getTransactionId(),
            "participantId", context.getParticipantId(),
            "serviceName", context.getServiceName(),
            "operation", context.getOperation(),
            "compensationMethod", context.getCompensationMethod(),
            "compensationParams", context.getCompensationParams(),
            "status", context.getStatus()
        );
        
        // Create primary key map
        Map<String, Object> primaryKey = Map.of("transactionId", context.getTransactionId());
        
        // Process based on operation type
        Map<String, Object> beforeValues = null;
        Map<String, Object> afterValues = null;
        
        if (operationType == DatabaseChangeEvent.OperationType.INSERT || 
            operationType == DatabaseChangeEvent.OperationType.UPDATE) {
            afterValues = values;
        } else if (operationType == DatabaseChangeEvent.OperationType.DELETE) {
            beforeValues = values;
        }
        
        // Create and process the change event
        cdcEventListener.createAndProcessChangeEvent(
            "transaction_context",
            operationType,
            primaryKey,
            beforeValues,
            afterValues
        );
    }
    
    /**
     * Check if CDC event listener is active
     * @return true if active, false otherwise
     */
    public boolean isCdcActive() {
        return cdcEventListener.isListening();
    }
    
    /**
     * Start the CDC event listener
     */
    public void startCdc() {
        cdcEventListener.startListening();
        log.info("CDC event listener started");
    }
    
    /**
     * Stop the CDC event listener
     */
    public void stopCdc() {
        cdcEventListener.stopListening();
        log.info("CDC event listener stopped");
    }
}