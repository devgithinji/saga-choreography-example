package com.densoft.saga.order.config;

import com.densoft.saga.commons.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
@RequiredArgsConstructor
public class EventConsumerConfig {
    private final OrderStatusUpdateHandler orderStatusUpdateHandler;
    @Bean
    public Consumer<PaymentEvent> paymentEventConsumer() {
        //listen payment-event-topic
        //will check payment status
        //if payment status completed -> complete the order
        //if payment status failed -> cancel the order
        return paymentEvent -> orderStatusUpdateHandler.updateOrder(paymentEvent.getPaymentRequestDto().getOrderId(), purchaseOrder -> {
            purchaseOrder.setPaymentStatus(paymentEvent.getPaymentStatus());
        });

    }
}
