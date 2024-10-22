package vn.vnpay.enums;
public enum ExchangeName {
    MY_EXCHANGE("myExchange")
    ;
    private final String name;

    ExchangeName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
