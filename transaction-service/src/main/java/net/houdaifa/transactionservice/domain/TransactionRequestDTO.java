package net.houdaifa.transactionservice.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionRequestDTO(
        UUID accountId,
        BigDecimal amount,
        String currency,
        String merchantCategory,
        String country
) {
}
