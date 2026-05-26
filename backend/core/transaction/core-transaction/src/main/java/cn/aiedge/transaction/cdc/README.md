# Change Data Capture (CDC) Module

## Overview

The Change Data Capture (CDC) module provides functionality for capturing, processing, and managing database changes in real-time. It's designed to integrate with Debezium or Canal to capture database changes and process them according to business logic.

## Architecture

The CDC module consists of the following components:

### Core Classes

- **DatabaseChangeEvent**: Represents a database change event with details about the operation, table, values, etc.
- **CdcProcessor**: Interface for processing database change events
- **DefaultCdcProcessor**: Default implementation of the CDC processor
- **EventSerializer**: Handles serialization and deserialization of events
- **CdcEventPublisher**: Interface for publishing events to message queues
- **DefaultCdcEventPublisher**: Default implementation of the event publisher

### Integration Components

- **CdcEventListener**: Listens to database changes and processes them
- **CdcTransactionService**: Integrates CDC functionality with transaction management
- **CdcController**: Provides REST API endpoints for CDC functionality
- **CdcConfiguration**: Spring configuration for CDC beans
- **CdcUtils**: Utility methods for CDC operations

## Features

1. **Real-time Change Capture**: Captures INSERT, UPDATE, DELETE operations in real-time
2. **Event Processing**: Processes events with validation, enrichment, and error handling
3. **Serialization**: Supports JSON serialization of events for transmission
4. **Integration**: Seamlessly integrates with the transaction management system
5. **Monitoring**: Provides API endpoints for monitoring and controlling the CDC process

## Usage

### API Endpoints

- `GET /api/cdc/status` - Get the status of the CDC event listener
- `POST /api/cdc/start` - Start the CDC event listener
- `POST /api/cdc/stop` - Stop the CDC event listener
- `POST /api/cdc/process-change` - Process a manual database change
- `POST /api/cdc/process-transaction-logs` - Process transaction log changes

### Example API Request

```json
{
  "tableName": "distributed_transaction_log",
  "operationType": "INSERT",
  "primaryKey": {
    "id": 123
  },
  "beforeValues": {},
  "afterValues": {
    "transactionId": "tx-123",
    "status": "COMPLETED",
    "createTime": "2023-01-01T10:00:00"
  }
}
```

### Programmatic Usage

```java
@Autowired
private CdcTransactionService cdcTransactionService;

// Process a transaction log change
DistributedTransactionLog logEntry = ...;
cdcTransactionService.processTransactionLogChange(
    logEntry, 
    DatabaseChangeEvent.OperationType.INSERT
);

// Process a custom change
Map<String, Object> primaryKey = Map.of("id", 123);
Map<String, Object> afterValues = Map.of("status", "COMPLETED");
DatabaseChangeEvent event = cdcTransactionService.processCustomTransactionChange(
    "distributed_transaction_log",
    DatabaseChangeEvent.OperationType.UPDATE,
    primaryKey,
    null,  // beforeValues
    afterValues
);
```

## Configuration

The CDC module is automatically configured via Spring's component scanning. The main configuration class is `CdcConfiguration` which creates the necessary beans.

## Error Handling

The CDC module includes comprehensive error handling:

- Validation of incoming events
- Error logging and monitoring
- Dead letter queue support (in production implementations)
- Retry mechanisms (in production implementations)

## Integration with Transaction Management

The CDC module is tightly integrated with the transaction management system:

- Monitors transaction log changes
- Processes transaction-related database changes
- Maintains consistency between CDC events and transaction states
- Provides hooks for transaction compensation based on CDC events

## Testing

Unit tests should cover:

- Event creation and validation
- Event processing workflows
- Serialization/deserialization
- Error handling scenarios
- Integration with transaction management

## Deployment Considerations

- Ensure proper database permissions for CDC tools (Debezium/Canal)
- Configure message brokers (Kafka/RabbitMQ) for event publishing
- Monitor event processing lag
- Set up proper logging and alerting
- Plan for scalability based on expected change volume