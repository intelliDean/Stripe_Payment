package com.javatechie.service;

import com.javatechie.dto.BalanceDTO;
import com.javatechie.dto.ProductRequest;
import com.javatechie.dto.StripeResponse;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Balance;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class StripeService {
//
//    @Value("${stripe.secretKey}")
//    private String secretKey;

    @Value("${stripe.successUrl}")
    private String successUrl;

    @Value("${stripe.cancelUrl}")
    private String cancelUrl;

    @Value("${app.name}")
    private String appName;


    public StripeResponse createCheckoutSession(ProductRequest productRequest) {
        // Set the Stripe API key
//        Stripe.apiKey = secretKey;

        // Determine currency, defaulting to USD
        final String CURRENCY = Optional.ofNullable(productRequest.getCurrency()).orElse("USD");

        // Build the session create parameters
        SessionCreateParams params = buildSessionCreateParams(productRequest, CURRENCY);

        // Configure idempotency to avoid duplicate requests
        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey(idempotencyKey())
                .build();

        try {
            Session session = Session.create(params, options);

            return StripeResponse.builder()
                    .status(String.valueOf(HttpStatus.CREATED))
                    .message("Payment session created successfully")
                    .sessionId(session.getId())
                    .sessionUrl(session.getUrl())
                    .build();

        } catch (StripeException e) {

            log.error("StripeException occurred: {}", e.getMessage(), e);

            return StripeResponse.builder()
                    .status(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR))
                    .message(e.getMessage())
                    .build();
        }
    }

    private SessionCreateParams buildSessionCreateParams(ProductRequest productRequest, final String CURRENCY) {
        return SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
//                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.PAYPAL)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(buildLineItem(productRequest, CURRENCY))
                .build();
    }

    private SessionCreateParams.LineItem buildLineItem(ProductRequest productRequest, final String CURRENCY) {
        return SessionCreateParams.LineItem.builder()
                .setQuantity(1L)
                .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(CURRENCY)
                                .setUnitAmount(productRequest.getAmount() * 100)
                                .setProductData(
                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                .setName(appName)
                                                .build()
                                ).build()
                ).build();
    }

    public BalanceDTO getBalance() throws StripeException {
//        Stripe.apiKey = secretKey;

        Balance balance = Balance.retrieve();
        return BalanceDTO.builder()
                .currency(balance.getAvailable().getFirst().getCurrency())
                .availableAmount(balance.getAvailable().getFirst().getAmount())
                .liveMode(balance.getLivemode())
                .build();
    }

    private static String idempotencyKey() {
        long timestamp = System.currentTimeMillis() % 1_000_000_000; // Use only the last 9 digits of the timestamp
        String shortUUID = UUID.randomUUID().toString().replace("-", "").substring(0, 8); // Single UUID (8 chars)
        return String.format("%s%09d", shortUUID, timestamp); // Combine short UUID and timestamp
    }
}