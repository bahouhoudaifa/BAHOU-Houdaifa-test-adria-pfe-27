package net.houdaifa.transactionservice.domain;

public record TransferResponseDTO(
        TransactionResponseDTO debitTransaction,
        TransactionResponseDTO creditTransaction,
        String message
) {
}
