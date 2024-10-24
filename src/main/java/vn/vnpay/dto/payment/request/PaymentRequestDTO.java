package vn.vnpay.dto.payment.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
public class PaymentRequestDTO {
    private String tokenKey;
    private String apiId;
    private String mobile;
    private String bankCode;
    private String privateKey;
    private String accountNo;
    private String payDate;
    private String additionalData;
    private BigDecimal debitAmount;
    private String respCode;
    private String respDesc;
    private String traceTransfer;
    private String messageType;
    private String checkSum;
    private String oderCode;
    private String userName;
    private BigDecimal realAmount;
    private String promotionCode;
    private AddValueDTO addValue;

}
