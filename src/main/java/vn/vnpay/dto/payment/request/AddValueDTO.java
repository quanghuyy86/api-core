package vn.vnpay.dto.payment.request;

public class AddValueDTO {
    private String payMethod;
    private Integer payMethodMMS;

    public String getPayMethod() {
        return payMethod;
    }

    public void setPayMethod(String payMethod) {
        this.payMethod = payMethod;
    }

    public Integer getPayMethodMMS() {
        return payMethodMMS;
    }

    public void setPayMethodMMS(Integer payMethodMMS) {
        this.payMethodMMS = payMethodMMS;
    }
}
