package vn.vnpay.enums;

import lombok.Getter;

@Getter
public enum ExchangeName {
    MY_EXCHANGE("myExchange")
    ;
    private final String name;

    ExchangeName(String name) {
        this.name = name;
    }

}
