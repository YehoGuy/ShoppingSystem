package com.example.app.PresentationLayer.DTO.Purchase;

import com.example.app.ApplicationLayer.Purchase.PaymentMethod;

public record PaymentMethodDTO(
        /*  ——— base receipt fields ——— */
        String paymentDetails) {
    /* -------- Domain → DTO -------- */
    public static PaymentMethodDTO fromDomain(PaymentMethod p) {
        return new PaymentMethodDTO(p.getDetails());
    }
}
