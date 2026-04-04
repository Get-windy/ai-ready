package cn.aiedge.notification.template;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TemplateRenderer 单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
class TemplateRendererTest {

    private TemplateRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new TemplateRenderer();
    }

    @Test
    @DisplayName("渲染模板 - 简单变量")
    void testRender_SimpleVariable() {
        // Given
        String template = "Hello, ${name}!";
        Map<String, Object> variables = Map.of("name", "World");

        // When
        String result = renderer.render(template, variables);

        // Then
        assertEquals("Hello, World!", result);
    }

    @Test
    @DisplayName("渲染模板 - 多个变量")
    void testRender_MultipleVariables() {
        // Given
        String template = "Dear ${title} ${name}, your order ${orderId} has been shipped.";
        Map<String, Object> variables = Map.of(
                "title", "Mr.",
                "name", "Smith",
                "orderId", "ORD-12345"
        );

        // When
        String result = renderer.render(template, variables);

        // Then
        assertEquals("Dear Mr. Smith, your order ORD-12345 has been shipped.", result);
    }

    @Test
    @DisplayName("渲染模板 - 缺少变量")
    void testRender_MissingVariable() {
        // Given
        String template = "Hello, ${name}! Your balance is ${balance}.";
        Map<String, Object> variables = Map.of("name", "John");

        // When
        String result = renderer.render(template, variables);

        // Then
        assertEquals("Hello, John! Your balance is .", result);
    }

    @Test
    @DisplayName("渲染模板 - 空变量映射")
    void testRender_EmptyVariables() {
        // Given
        String template = "Hello, ${name}!";
        Map<String, Object> variables = Map.of();

        // When
        String result = renderer.render(template, variables);

        // Then
        assertEquals("Hello, !", result);
    }

    @Test
    @DisplayName("渲染模板 - null变量映射")
    void testRender_NullVariables() {
        // Given
        String template = "Hello, World!";

        // When
        String result = renderer.render(template, null);

        // Then
        assertEquals("Hello, World!", result);
    }

    @Test
    @DisplayName("渲染模板 - 空模板")
    void testRender_EmptyTemplate() {
        // When
        String result = renderer.render("", Map.of("name", "Test"));

        // Then
        assertEquals("", result);
    }

    @Test
    @DisplayName("渲染模板 - null模板")
    void testRender_NullTemplate() {
        // When
        String result = renderer.render(null, Map.of("name", "Test"));

        // Then
        assertEquals("", result);
    }

    @Test
    @DisplayName("渲染模板 - 特殊字符")
    void testRender_SpecialCharacters() {
        // Given
        String template = "Price: ${price}";
        Map<String, Object> variables = Map.of("price", "$100.00");

        // When
        String result = renderer.render(template, variables);

        // Then
        assertEquals("Price: $100.00", result);
    }

    @Test
    @DisplayName("渲染模板 - 数字类型变量")
    void testRender_NumberVariable() {
        // Given
        String template = "You have ${count} new messages.";
        Map<String, Object> variables = Map.of("count", 5);

        // When
        String result = renderer.render(template, variables);

        // Then
        assertEquals("You have 5 new messages.", result);
    }

    @Test
    @DisplayName("渲染模板 - 中文内容")
    void testRender_ChineseContent() {
        // Given
        String template = "尊敬的${name}，您好！您的订单${orderId}已发货。";
        Map<String, Object> variables = Map.of(
                "name", "张三",
                "orderId", "ORD-12345"
        );

        // When
        String result = renderer.render(template, variables);

        // Then
        assertEquals("尊敬的张三，您好！您的订单ORD-12345已发货。", result);
    }
}
