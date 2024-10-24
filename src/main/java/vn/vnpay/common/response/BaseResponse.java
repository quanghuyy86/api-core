package vn.vnpay.common.response;

import lombok.Getter;
import lombok.Setter;
import vn.vnpay.common.enums.PaymentResponseCode;
@Getter
@Setter
public class BaseResponse<T> {
    private String code;
    private String message;
    private T data;

    public static BaseResponse<Void> success(){
        return new BaseResponse<>(PaymentResponseCode.SUCCESS.getCode(), PaymentResponseCode.SUCCESS.getMessage());
    }

    public static <D> BaseResponse<D> success(D data){
        return new BaseResponse<>(PaymentResponseCode.SUCCESS.getCode(), PaymentResponseCode.SUCCESS.getMessage(), data);
    }

    public static <D> BaseResponse<D> unsuccessful(String message){
        return new BaseResponse<>(PaymentResponseCode.UNKNOWN_ERROR.getCode(), message, null);
    }

    public BaseResponse() {
    }

    public BaseResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public BaseResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }


}
