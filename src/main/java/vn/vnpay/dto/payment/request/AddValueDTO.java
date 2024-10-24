package vn.vnpay.dto.payment.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddValueDTO {
    private String payMethod;
    private Integer payMethodMMS;

}
