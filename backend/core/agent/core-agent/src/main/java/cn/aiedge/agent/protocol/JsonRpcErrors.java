package cn.aiedge.agent.protocol;

public final class JsonRpcErrors {

    private JsonRpcErrors() {}

    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;

    public static final int AUTH_ERROR = -32001;
    public static final int CAPABILITY_NOT_FOUND = -32002;
    public static final int CAPABILITY_DISABLED = -32003;
    public static final int CAPABILITY_TIMEOUT = -32004;
    public static final int AGENT_DISABLED = -32005;
    public static final int RATE_LIMIT_EXCEEDED = -32006;

    public static String getMessage(int code) {
        return switch (code) {
            case PARSE_ERROR -> "Parse error";
            case INVALID_REQUEST -> "Invalid Request";
            case METHOD_NOT_FOUND -> "Method not found";
            case INVALID_PARAMS -> "Invalid params";
            case INTERNAL_ERROR -> "Internal error";
            case AUTH_ERROR -> "Authentication failed";
            case CAPABILITY_NOT_FOUND -> "Capability not found";
            case CAPABILITY_DISABLED -> "Capability disabled";
            case CAPABILITY_TIMEOUT -> "Capability execution timeout";
            case AGENT_DISABLED -> "Agent is disabled";
            case RATE_LIMIT_EXCEEDED -> "Rate limit exceeded";
            default -> "Unknown error";
        };
    }
}
