package vn.vnpay.enums;

public enum QueueName {
    MY_QUEUE("myQueue")
    ;
    private final String name;

    QueueName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
