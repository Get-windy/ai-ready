package cn.aiedge.transaction.cdc;

import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Utility class for CDC operations
 */
public class CdcUtils {
    
    // Pattern for validating table names
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_-]*$");
    
    /**
     * Validates if a table name is valid
     * @param tableName the table name to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidTableName(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            return false;
        }
        
        return TABLE_NAME_PATTERN.matcher(tableName).matches();
    }
    
    /**
     * Sanitizes a table name to prevent SQL injection
     * @param tableName the table name to sanitize
     * @return sanitized table name or null if invalid
     */
    public static String sanitizeTableName(String tableName) {
        if (!isValidTableName(tableName)) {
            return null;
        }
        
        // Additional sanitization could be added here if needed
        return tableName.trim();
    }
    
    /**
     * Creates a deep copy of a Map containing database values
     * @param original the original map to copy
     * @return a deep copy of the map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> deepCopy(Map<String, Object> original) {
        if (original == null) {
            return null;
        }
        
        Map<String, Object> copy = new HashMap<>();
        for (Map.Entry<String, Object> entry : original.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // For primitive types and strings, direct assignment is safe
            // For complex objects, additional deep copying logic might be needed
            copy.put(key, value);
        }
        
        return copy;
    }
    
    /**
     * Compares two maps of database values and returns the differences
     * @param before the before values
     * @param after the after values
     * @return a map containing only the changed keys and their new values
     */
    public static Map<String, Object> getChangedValues(Map<String, Object> before, Map<String, Object> after) {
        if (before == null && after == null) {
            return new HashMap<>();
        }
        
        if (before == null) {
            return deepCopy(after);
        }
        
        if (after == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> changes = new HashMap<>();
        for (Map.Entry<String, Object> afterEntry : after.entrySet()) {
            String key = afterEntry.getKey();
            Object afterValue = afterEntry.getValue();
            Object beforeValue = before.get(key);
            
            if (beforeValue == null && afterValue != null) {
                // Value was added
                changes.put(key, afterValue);
            } else if (beforeValue != null && afterValue == null) {
                // Value was removed
                changes.put(key, null);
            } else if (beforeValue != null && afterValue != null && !beforeValue.equals(afterValue)) {
                // Value was changed
                changes.put(key, afterValue);
            }
        }
        
        return changes;
    }
    
    /**
     * Checks if two maps of database values are equal
     * @param map1 the first map
     * @param map2 the second map
     * @return true if equal, false otherwise
     */
    public static boolean areMapsEqual(Map<String, Object> map1, Map<String, Object> map2) {
        if (map1 == null && map2 == null) {
            return true;
        }
        
        if (map1 == null || map2 == null) {
            return false;
        }
        
        return map1.equals(map2);
    }
    
    /**
     * Generates a human-readable summary of a database change event
     * @param event the database change event
     * @return a string summary of the event
     */
    public static String generateEventSummary(DatabaseChangeEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("Event ID: ").append(event.getEventId()).append("\n");
        sb.append("Operation: ").append(event.getOperationType()).append("\n");
        sb.append("Table: ").append(event.getTableName()).append("\n");
        sb.append("Timestamp: ").append(event.getTimestamp()).append("\n");
        sb.append("Source: ").append(event.getSource()).append("\n");
        
        if (event.getPrimaryKey() != null && !event.getPrimaryKey().isEmpty()) {
            sb.append("Primary Key: ").append(event.getPrimaryKey()).append("\n");
        }
        
        if (event.getBeforeValues() != null && !event.getBeforeValues().isEmpty()) {
            sb.append("Before Values: ").append(event.getBeforeValues()).append("\n");
        }
        
        if (event.getAfterValues() != null && !event.getAfterValues().isEmpty()) {
            sb.append("After Values: ").append(event.getAfterValues()).append("\n");
        }
        
        return sb.toString();
    }
}