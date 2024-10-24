package vn.vnpay.common.response;

import lombok.Getter;
import lombok.Setter;
import vn.vnpay.common.enums.PaymentResponseCode;
import vn.vnpay.common.util.CheckSumUtil;
import vn.vnpay.common.util.ResponseTimeUtil;
import vn.vnpay.dto.payment.request.AddValueDTO;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
public class PaymentResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String code;
    private String message;
    private String responseId;
    private String responseTime;
    private String checkSum;
    private AddValueDTO addValue;

    public PaymentResponse() {
    }

    public PaymentResponse(String code, String message, String responseId, String responseTime, String checkSum, AddValueDTO addValue) {
        this.code = code;
        this.message = message;
        this.responseId = responseId;
        this.responseTime = responseTime;
        this.checkSum = checkSum;
        this.addValue = addValue;
    }

    public PaymentResponse(String code, String message, String responseId, String checkSum) {
        this.code = code;
        this.message = message;
        this.responseId = responseId;
        this.responseTime = ResponseTimeUtil.getCurrentTime();
        this.checkSum = checkSum;
    }

    public static PaymentResponse success(String privateKey, AddValueDTO addValue){
        String code = PaymentResponseCode.SUCCESS.getCode();
        String message = PaymentResponseCode.SUCCESS.getMessage();
        String responseId = generateResponseId();
        String checkSum = CheckSumUtil.calculateCheckSum(code + message + responseId + privateKey + addValue);
        String responseTime = ResponseTimeUtil.getCurrentTime();
        return new PaymentResponse(code, message, responseId, checkSum, responseTime, addValue);
    }

    public static PaymentResponse unsuccessful(String code, String message, String privateKey){
        String responseId = generateResponseId();
        String responseTime = ResponseTimeUtil.getCurrentTime();
        String checkSum = CheckSumUtil.calculateCheckSum(code + message + responseId + responseTime + privateKey);
        return new PaymentResponse(code, message, responseId, checkSum);
    }

    private static String generateResponseId() {
        return UUID.randomUUID().toString().replace("-", "");
    }


}
