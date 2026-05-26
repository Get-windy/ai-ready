package cn.aiedge.transaction.cdc;

import cn.aiedge.transaction.entity.DistributedTransactionLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing CDC (Change Data Capture) functionality
 */
@RestController
@RequestMapping("/api/cdc")
@Slf4j
public class CdcController {
    
    @Autowired
    private CdcTransactionService cdcTransactionService;
    
    @Autowired
    private CdcProcessor cdcProcessor;
    
    @Autowired
    private CdcEventListener cdcEventListener;
    
    /**
     * Get the status of the CDC event listener
     * @return ResponseEntity with status information
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getCdcStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("active", cdcEventListener.isListening());
        status.put("processorName", cdcProcessor.getName());
        status.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * Start the CDC event listener
     * @return ResponseEntity indicating success or failure
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startCdc() {
        try {
            cdcTransactionService.startCdc();
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "CDC event listener started successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error starting CDC event listener", e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to start CDC event listener: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Stop the CDC event listener
     * @return ResponseEntity indicating success or failure
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, String>> stopCdc() {
        try {
            cdcTransactionService.stopCdc();
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "CDC event listener stopped successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error stopping CDC event listener", e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to stop CDC event listener: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Process a manual database change event
     * @param requestBody the request body containing event details
     * @return ResponseEntity indicating success or failure
     */
    @PostMapping("/process-change")
    public ResponseEntity<Map<String, String>> processManualChange(@RequestBody Map<String, Object> requestBody) {
        try {
            String tableName = (String) requestBody.get("tableName");
            String operationTypeStr = (String) requestBody.get("operationType");
            Map<String, Object> primaryKey = (Map<String, Object>) requestBody.get("primaryKey");
            Map<String, Object> beforeValues = (Map<String, Object>) requestBody.get("beforeValues");
            Map<String, Object> afterValues = (Map<String, Object>) requestBody.get("afterValues");
            
            if (tableName == null || operationTypeStr == null) {
                Map<String, String> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "tableName and operationType are required");
                return ResponseEntity.badRequest().body(response);
            }
            
            DatabaseChangeEvent.OperationType operationType = 
                DatabaseChangeEvent.OperationType.valueOf(operationTypeStr.toUpperCase());
            
            // Use default values if not provided
            if (primaryKey == null) primaryKey = new HashMap<>();
            if (beforeValues == null) beforeValues = new HashMap<>();
            if (afterValues == null) afterValues = new HashMap<>();
            
            cdcTransactionService.processCustomTransactionChange(
                tableName, operationType, primaryKey, beforeValues, afterValues
            );
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Database change processed successfully");
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid operation type provided", e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Invalid operation type: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error processing manual database change", e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to process database change: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Simulate processing of transaction log changes
     * @param logEntries list of transaction log entries
     * @param operationType the type of operation
     * @return ResponseEntity indicating success or failure
     */
    @PostMapping("/process-transaction-logs")
    public ResponseEntity<Map<String, String>> processTransactionLogs(
            @RequestBody List<DistributedTransactionLog> logEntries,
            @RequestParam(defaultValue = "UPDATE") String operationType) {
        
        try {
            DatabaseChangeEvent.OperationType opType = 
                DatabaseChangeEvent.OperationType.valueOf(operationType.toUpperCase());
            
            cdcTransactionService.processTransactionLogChanges(logEntries, opType);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Processed " + logEntries.size() + " transaction log changes");
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid operation type provided", e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Invalid operation type: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error processing transaction logs", e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to process transaction logs: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}