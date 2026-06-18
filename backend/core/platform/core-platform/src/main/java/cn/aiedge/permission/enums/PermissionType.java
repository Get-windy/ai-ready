package cn.aiedge.permission.enums;

public enum PermissionType {
    READ(1, "读取"),
    WRITE(2, "写入"),
    CREATE(4, "创建"),
    DELETE(8, "删除");

    private final int code;
    private final String name;

    PermissionType(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static int combine(PermissionType... types) {
        int result = 0;
        for (PermissionType type : types) {
            result |= type.code;
        }
        return result;
    }

    public static boolean hasPermission(int permissions, PermissionType type) {
        return (permissions & type.code) != 0;
    }
}