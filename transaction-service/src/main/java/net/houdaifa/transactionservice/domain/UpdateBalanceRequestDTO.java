package net.houdaifa.transactionservice.domain;

public record UpdateBalanceRequestDTO(
        double amount,
        String operationType
) {
}
