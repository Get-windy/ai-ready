package cn.aiedge.agent.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonRpcResponse {

    @JsonProperty("jsonrpc")
    private final String jsonrpc = "2.0";

    private Object result;

    private JsonRpcErrorDetail error;

    private Object id;

    public static JsonRpcResponse success(Object id, Object result) {
        return JsonRpcResponse.builder().id(id).result(result).build();
    }

    public static JsonRpcResponse error(Object id, int code, String message, Object data) {
        return JsonRpcResponse.builder()
                .id(id)
                .error(new JsonRpcErrorDetail(code, message, data))
                .build();
    }
}
