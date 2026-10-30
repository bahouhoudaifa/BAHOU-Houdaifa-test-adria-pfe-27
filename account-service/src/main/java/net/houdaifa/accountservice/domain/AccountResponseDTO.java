package net.houdaifa.accountservice.domain;

import java.util.UUID;

public record AccountResponseDTO(
        UUID id,
        String customerReference,
        AccountStatus status,
        String currency,
        double balance
) {
    public static AccountResponseDTO fromEntity(Account account) {
        return new AccountResponseDTO(
                account.getId(),
                account.getCustomerReference(),
                account.getStatus(),
                account.getCurrency(),
                account.getBalance()
        );
    }
}
