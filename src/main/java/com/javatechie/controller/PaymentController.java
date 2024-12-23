package com.javatechie.controller;

//import com.google.gson.JsonSyntaxException;
import com.javatechie.dto.BalanceDTO;
import com.javatechie.dto.ProductRequest;
import com.javatechie.dto.StripeResponse;
import com.javatechie.service.StripeService;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.net.ApiResource;
import com.stripe.net.ApiResource;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;

@Slf4j
@RestController
@Tag(name = "Stripe Controller")
@RequestMapping("/product/v1")
public class PaymentController {

     @Value("${stripe.endpointSecret}")
    private String stripeEndpointSecret;


    private final StripeService stripeService;

    public PaymentController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @PostMapping("/checkout")
    @Operation(summary = "Create Checkout Session", description = "This endpoint creates a Stripe checkout session")
    public ResponseEntity<StripeResponse> createSession(@RequestBody ProductRequest productRequest) {
        StripeResponse stripeResponse = stripeService.createCheckoutSession(productRequest);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(stripeResponse);
    }

    @GetMapping("balance")
    @Operation(summary = "Get Balance", description = "This endpoint retrieves merchant balance on Stripe")
    public ResponseEntity<BalanceDTO> getBalance() throws StripeException {
        return ResponseEntity.status(HttpStatus.OK).body(stripeService.getBalance());
    }

    public String getRequestBodyAsString(HttpServletRequest request) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        String line;

        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }

        return stringBuilder.toString();
    }

    @PostMapping("/webhook")
    @Operation(summary = "Listen For Events", description =
            "Endpoint to listen for events from Stripe when an action takes place in our Stripe account")
    public ResponseEntity<String> handleStripeWebhook(HttpServletRequest request) throws IOException, EventDataObjectDeserializationException {

        String endpointSecret = stripeEndpointSecret; // Replace with your actual Stripe secret
        String payload = getRequestBodyAsString(request);
        String sigHeader = request.getHeader("Stripe-Signature");
        Event event;


        try {
            // Construct the event from the payload and signature
            event = ApiResource.GSON.fromJson(payload, Event.class);
            log.info("Event: {}", event);

        } catch (Exception e) {
            log.error("Error while processing webhook: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
        }

        // Process the event based on its type
        switch (event.getType()) {
            case "customer.created":
                handleCustomerCreatedEvent(event);
                break;

            case "charge.captured":
                EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();

                StripeObject stripeObject = dataObjectDeserializer.deserializeUnsafe();

                if (stripeObject instanceof Charge charge) {
                    log.info("Charge Object: {}", charge.getObject());
                } else {
                    log.error("Failed to deserialize Customer object for event ID: {}", event.getId());
                }
                break;

            default:
                log.warn("Unhandled event type: {}", event.getType());
        }

        return ResponseEntity.ok("success");
    }

    private void handleCustomerCreatedEvent(Event event) throws EventDataObjectDeserializationException {
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();

        StripeObject stripeObject = dataObjectDeserializer.deserializeUnsafe();

        if (stripeObject instanceof Customer customer) {
            log.info("Customer Created: {}", customer.getObject());
        } else {
            log.error("Failed to deserialize Customer object for event ID: {}", event.getId());
        }
    }
}
