package vn.vnpay.enums;

public enum Route {
    TOKEN_KEY("/payment/token-key", "create tokenLey"),
    CREATE_PAYMENT("/payment", "create payment"),
    WELCOME("/api/welcome", "Api demo");
    private final String path;
    private final String message;
    Route(String path, String message) {
        this.path = path;
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public String getMessage() {
        return message;
    }
}
