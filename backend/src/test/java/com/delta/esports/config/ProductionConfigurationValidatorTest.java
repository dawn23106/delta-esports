package com.delta.esports.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionConfigurationValidatorTest {

    @Test
    void shouldAllowProductionConfigurationWhenPaymentIsDisabled() {
        PaymentProperties payment = new PaymentProperties();
        payment.setEnabled(false);
        ProductionConfigurationValidator validator = validator(payment, "https://admin.example.com");

        assertDoesNotThrow(() -> validator.run(null));
    }

    @Test
    void shouldRejectIncompleteRealPaymentConfiguration() {
        PaymentProperties payment = new PaymentProperties();
        payment.setEnabled(true);
        payment.setMockEnabled(false);
        ProductionConfigurationValidator validator = validator(payment, "https://admin.example.com");

        assertThrows(IllegalStateException.class, () -> validator.run(null));
    }

    @Test
    void shouldRejectLocalhostCorsInProduction() {
        PaymentProperties payment = new PaymentProperties();
        ProductionConfigurationValidator validator = validator(payment, "http://localhost:5173");

        assertThrows(IllegalStateException.class, () -> validator.run(null));
    }

    private ProductionConfigurationValidator validator(PaymentProperties payment, String origins) {
        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(payment);
        ReflectionTestUtils.setField(validator, "allowedOrigins", origins);
        return validator;
    }
}
