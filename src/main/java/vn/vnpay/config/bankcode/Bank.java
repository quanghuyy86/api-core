package vn.vnpay.config.bankcode;

public class Bank {
    private String bankCode;
    private String privateKey;
    public Bank() {
    }
    public Bank(String bankCode, String privateKey) {
        this.bankCode = bankCode;
        this.privateKey = privateKey;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }
}
