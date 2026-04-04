package cn.aiedge.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import lombok.extern.slf4j.Slf4j;

/**
 * API响应压缩配置
 * 
 * 对大于1KB的JSON响应进行Gzip压缩，减少传输带宽
 * 预期效果：响应体积减少60%-80%
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class ResponseCompressionConfig {

    private static final int MIN_COMPRESS_SIZE = 1024; // 1KB以上才压缩
    private static final String GZIP_ENCODING = "gzip";
    
    /**
     * Gzip响应压缩过滤器
     */
    @Bean
    public FilterRegistrationBean<GzipCompressionFilter> gzipCompressionFilter() {
        FilterRegistrationBean<GzipCompressionFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new GzipCompressionFilter());
        registration.addUrlPatterns("/api/*");
        registration.setName("gzipCompressionFilter");
        registration.setOrder(2); // 在TimingFilter之后
        return registration;
    }

    /**
     * Gzip压缩过滤器实现
     */
    public static class GzipCompressionFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, 
                                        HttpServletResponse response, 
                                        FilterChain filterChain) 
                throws ServletException, IOException {
            
            // 检查客户端是否支持Gzip
            String acceptEncoding = request.getHeader("Accept-Encoding");
            boolean supportsGzip = acceptEncoding != null && acceptEncoding.contains(GZIP_ENCODING);
            
            if (!supportsGzip) {
                // 客户端不支持压缩，直接放行
                filterChain.doFilter(request, response);
                return;
            }
            
            // 检查响应类型是否需要压缩（仅压缩JSON、文本等）
            String contentType = response.getContentType();
            boolean shouldCompress = contentType != null && 
                (contentType.contains("application/json") || 
                 contentType.contains("text/") ||
                 contentType.contains("application/xml"));
            
            if (!shouldCompress) {
                filterChain.doFilter(request, response);
                return;
            }
            
            // 使用压缩响应包装器
            GzipResponseWrapper wrappedResponse = new GzipResponseWrapper(response);
            try {
                filterChain.doFilter(request, wrappedResponse);
            } finally {
                // 完成压缩
                wrappedResponse.finishCompression();
            }
        }
    }

    /**
     * Gzip响应包装器
     * 
     * 收集响应内容，达到阈值后进行压缩
     */
    static class GzipResponseWrapper extends HttpServletResponseWrapper {
        
        private ByteArrayOutputStream buffer;
        private GZIPOutputStream gzipStream;
        private PrintWriter writer;
        private boolean compressionStarted = false;
        
        public GzipResponseWrapper(HttpServletResponse response) {
            super(response);
            this.buffer = new ByteArrayOutputStream();
        }
        
        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return new GzipServletOutputStream(buffer, this);
        }
        
        @Override
        public PrintWriter getWriter() throws IOException {
            if (writer == null) {
                OutputStreamWriter osw = new OutputStreamWriter(buffer, StandardCharsets.UTF_8);
                writer = new PrintWriter(osw);
            }
            return writer;
        }
        
        @Override
        public void setContentLength(int len) {
            // 不设置原始长度，压缩后长度会改变
        }
        
        @Override
        public void setContentLengthLong(long len) {
            // 不设置原始长度
        }
        
        /**
         * 完成压缩，将压缩后的内容写入原始响应
         */
        public void finishCompression() throws IOException {
            if (writer != null) {
                writer.flush();
            }
            
            byte[] content = buffer.toByteArray();
            int originalSize = content.length;
            
            // 如果内容太小，不压缩直接输出
            if (originalSize < MIN_COMPRESS_SIZE) {
                getResponse().setHeader("Content-Encoding", "identity");
                getResponse().getOutputStream().write(content);
                getResponse().setContentLength(originalSize);
                return;
            }
            
            // 压缩内容
            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            try (GZIPOutputStream gz = new GZIPOutputStream(compressed)) {
                gz.write(content);
            }
            
            byte[] compressedContent = compressed.toByteArray();
            int compressedSize = compressedContent.length;
            
            // 设置压缩响应头
            getResponse().setHeader("Content-Encoding", GZIP_ENCODING);
            getResponse().setHeader("Vary", "Accept-Encoding");
            getResponse().setContentLength(compressedSize);
            
            // 记录压缩效果
            double ratio = 100.0 * (originalSize - compressedSize) / originalSize;
            log.debug("[GZIP] Original: {} bytes, Compressed: {} bytes, Saved: {}%", 
                     originalSize, compressedSize, String.format("%.1f", ratio));
            
            // 输出压缩内容
            getResponse().getOutputStream().write(compressedContent);
        }
    }
    
    /**
     * 自定义ServletOutputStream，收集数据到buffer
     */
    static class GzipServletOutputStream extends ServletOutputStream {
        
        private final ByteArrayOutputStream buffer;
        private final GzipResponseWrapper wrapper;
        
        public GzipServletOutputStream(ByteArrayOutputStream buffer, GzipResponseWrapper wrapper) {
            this.buffer = buffer;
            this.wrapper = wrapper;
        }
        
        @Override
        public void write(int b) throws IOException {
            buffer.write(b);
        }
        
        @Override
        public void write(byte[] b) throws IOException {
            buffer.write(b);
        }
        
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            buffer.write(b, off, len);
        }
        
        @Override
        public boolean isReady() {
            return true;
        }
        
        @Override
        public void setWriteListener(jakarta.servlet.WriteListener writeListener) {
            // 不支持异步写入
        }
    }
}