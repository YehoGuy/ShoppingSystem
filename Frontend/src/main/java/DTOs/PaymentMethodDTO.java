package DTOs;

public class PaymentMethodDTO {

    private String methodDetails;

    // Constructor
    public PaymentMethodDTO() {
        methodDetails = null;
    }

    public PaymentMethodDTO(String methodDetails) {
        this.methodDetails = methodDetails; // to be implemented when we imolement the payment method in domain
    }

    public String getMethodDetails() {
        return methodDetails;
    }

    public void setMethodDetails(String methodDetails) {
        this.methodDetails = methodDetails;
    }

}
