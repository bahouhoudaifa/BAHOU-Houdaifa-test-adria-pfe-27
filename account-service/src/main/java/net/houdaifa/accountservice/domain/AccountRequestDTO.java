package net.houdaifa.accountservice.domain;

public record AccountRequestDTO(
        String customerReference,
        String currency,
        double initialBalance,
        AccountStatus status
) {
}
