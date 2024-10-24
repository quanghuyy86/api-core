package vn.vnpay.dto.payment.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConsumerResponse {
    private String code;
    private String message;
    private Object data;


}
