package vn.vnpay.enums;

import lombok.Getter;

@Getter
public enum QueueName {
    MY_QUEUE("myQueue")
    ;
    private final String name;

    QueueName(String name) {
        this.name = name;
    }

}
