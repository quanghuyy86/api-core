package vn.vnpay.enums;

import lombok.Getter;

@Getter
public enum RoutingKey {
    MY_ROUTING_KEY("myRoutingKey")
    ;
    private final String name;

    RoutingKey(String name) {
        this.name = name;
    }
}
