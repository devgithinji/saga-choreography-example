package com.densoft.saga.payment.service;

import com.densoft.saga.commons.dto.OrderRequestDto;
import com.densoft.saga.commons.dto.PaymentRequestDto;
import com.densoft.saga.commons.event.OrderEvent;
import com.densoft.saga.commons.event.PaymentEvent;
import com.densoft.saga.commons.event.PaymentStatus;
import com.densoft.saga.payment.entity.UserBalance;
import com.densoft.saga.payment.entity.UserTransaction;
import com.densoft.saga.payment.repository.UserBalanceRepository;
import com.densoft.saga.payment.repository.UserTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.persistence.Table;
import javax.transaction.Transactional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final UserBalanceRepository userBalanceRepository;
    private final UserTransactionRepository userTransactionRepository;

    @PostConstruct
    private void initUserBalanceInDb() {
        userBalanceRepository.saveAll(Stream.of(
                new UserBalance(101, 5000),
                new UserBalance(102, 3000),
                new UserBalance(103, 4200),
                new UserBalance(104, 20000),
                new UserBalance(105, 999)
        ).toList());
    }

    //    get the user id
    //    check the balance availability
    //    if balance is sufficient -> Payment completed and deduct amount from DB
    //    if payment not sufficient -> cancel the order event and update amount in DB
    @Transactional
    public PaymentEvent newOrderEvent(OrderEvent orderEvent) {
        OrderRequestDto orderRequestDto = orderEvent.getOrderRequestDto();
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(
                orderRequestDto.getOrderId(),
                orderRequestDto.getUserId(),
                orderRequestDto.getAmount());
        return userBalanceRepository.findById(orderRequestDto.getUserId())
                .filter(userBalance -> userBalance.getPrice() >= orderRequestDto.getAmount())
                .map(userBalance -> {
                    userBalance.setPrice(userBalance.getPrice() - orderRequestDto.getAmount());
                    userTransactionRepository.save(new UserTransaction(orderRequestDto.getOrderId(), orderRequestDto.getUserId(), orderRequestDto.getAmount()));
                    return new PaymentEvent(paymentRequestDto, PaymentStatus.PAYMENT_COMPLETED);
                }).orElse(new PaymentEvent(paymentRequestDto, PaymentStatus.PAYMENT_FAILED));
    }

    @Transactional
    public void cancelOrderEvent(OrderEvent orderEvent) {
        userTransactionRepository.findById(orderEvent.getOrderRequestDto().getOrderId()).ifPresent(userTransaction -> {
            userTransactionRepository.delete(userTransaction);
            userTransactionRepository.findById(userTransaction.getUserId())
                    .ifPresent(userBalance -> userBalance.setAmount(userBalance.getAmount() + userTransaction.getAmount()));
        });
    }
}
