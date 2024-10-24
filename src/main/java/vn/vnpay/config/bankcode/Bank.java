package vn.vnpay.config.bankcode;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Bank {
    private String bankCode;
    private String privateKey;
    public Bank() {
    }
    public Bank(String bankCode, String privateKey) {
        this.bankCode = bankCode;
        this.privateKey = privateKey;
    }

}
