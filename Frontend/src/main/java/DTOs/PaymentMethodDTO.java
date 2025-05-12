package DTOs;

public class PaymentMethodDTO {
    private String methodName;
    private String methodType;
    private String methodDetails;

    public PaymentMethodDTO(String methodName, String methodType, String methodDetails) {
        this.methodName = methodName;
        this.methodType = methodType;
        this.methodDetails = methodDetails;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodType() {
        return methodType;
    }

    public void setMethodType(String methodType) {
        this.methodType = methodType;
    }

    public String getMethodDetails() {
        return methodDetails;
    }

    public void setMethodDetails(String methodDetails) {
        this.methodDetails = methodDetails;
    }

}
