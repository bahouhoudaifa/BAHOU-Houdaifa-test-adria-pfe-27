package net.houdaifa.transactionservice.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequestDTO(
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String currency,
        String merchantCategory,
        String country
) {
}
