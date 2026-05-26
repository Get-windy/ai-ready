package cn.aiedge.ready.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RabbitMqProducer messageProducer;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testSendMessage() throws Exception {
        // Given
        String queueName = "test_queue";
        String message = "test_message";
        String jsonMessage = "{\"content\":\"test_message\"}";
        
        when(objectMapper.writeValueAsString(message)).thenReturn(jsonMessage);

        // When
        messageProducer.sendMessage(queueName, message);

        // Then
        verify(rabbitTemplate).convertAndSend(eq(queueName), eq(jsonMessage), any());
    }

    @Test
    void testSendToExchange() throws Exception {
        // Given
        String exchange = "test_exchange";
        String routingKey = "test_routing_key";
        String message = "test_message";
        String jsonMessage = "{\"content\":\"test_message\"}";
        
        when(objectMapper.writeValueAsString(message)).thenReturn(jsonMessage);

        // When
        messageProducer.sendToExchange(exchange, routingKey, message);

        // Then
        verify(rabbitTemplate).convertAndSend(eq(exchange), eq(routingKey), eq(jsonMessage), any());
    }
}