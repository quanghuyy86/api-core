package vn.vnpay.enums;

public enum MessageType {
    SUCCESS("1", "Thanh cong")
    ;
    private final String code;
    private final String message;

    MessageType(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
