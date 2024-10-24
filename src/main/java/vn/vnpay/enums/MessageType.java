package vn.vnpay.enums;

import lombok.Getter;

@Getter
public enum MessageType {
    SUCCESS("1", "Thanh cong")
    ;
    private final String code;
    private final String message;

    MessageType(String code, String message) {
        this.code = code;
        this.message = message;
    }

}
