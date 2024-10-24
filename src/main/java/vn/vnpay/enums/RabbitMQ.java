package vn.vnpay.enums;

import lombok.Getter;

@Getter
public enum RabbitMQ {
    POOL_SIZE("pool", 10),
    PORT("rabbitMQ-port", 5672),
    HOST("rabbitMq-host", "127.0.0.1"),
    USER_NAME("rabbitMQ-userName", "guest"),
    PASSWORD("rabbitMQ-password", "guest");
    private final String name;
    private final Object value;

    RabbitMQ(String name, String value) {
        this.name = name;
        this.value = value;
    }

    RabbitMQ(String name, int value) {
        this.name = name;
        this.value = value;
    }


    // Trả về giá trị dưới dạng String
    public String getStringValue() {
        if (value instanceof String) {
            return (String) value;
        } else {
            throw new IllegalArgumentException("Value is not a String");
        }
    }

    // Trả về giá trị dưới dạng int
    public int getIntValue() {
        if (value instanceof Integer) {
            return (int) value;
        } else {
            throw new IllegalArgumentException("Value is not an Integer");
        }
    }

    // Kiểm tra kiểu dữ liệu của giá trị
    public boolean isString() {
        return value instanceof String;
    }

    public boolean isInt() {
        return value instanceof Integer;
    }
}
