package net.houdaifa.accountservice.domain;

public record UpdateBalanceRequestDTO(
        double amount,
        String operationType // "DEBIT" or "CREDIT"
) {
}
