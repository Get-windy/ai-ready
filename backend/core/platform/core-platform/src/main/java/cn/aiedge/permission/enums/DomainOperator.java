package cn.aiedge.permission.enums;

public enum DomainOperator {
    EQUALS("="),
    NOT_EQUALS("!="),
    GREATER_THAN(">"),
    LESS_THAN("<"),
    GREATER_THAN_OR_EQUAL(">="),
    LESS_THAN_OR_EQUAL("<="),
    LIKE("like"),
    ILIKE("ilike"),
    IN("in"),
    NOT_IN("not in"),
    IS_NULL("is null"),
    IS_NOT_NULL("is not null"),
    CHILD_OF("child_of");

    private final String symbol;

    DomainOperator(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}