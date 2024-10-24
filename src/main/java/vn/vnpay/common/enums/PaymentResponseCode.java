package vn.vnpay.common.enums;

import lombok.Getter;

@Getter
public enum PaymentResponseCode {
    SUCCESS("00", "Success"),
    FIELD_ERROR("01", "Field error"),
    UNKNOWN_ERROR("99", "Unknown error"),
    NOT_FOUND_ERROR("404", "Not Found Error");

    private final String code;
    private final String message;

    PaymentResponseCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

}
