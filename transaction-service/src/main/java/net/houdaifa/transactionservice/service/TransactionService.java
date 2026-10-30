package net.houdaifa.transactionservice.service;

import net.houdaifa.transactionservice.domain.TransactionRequestDTO;
import net.houdaifa.transactionservice.domain.TransactionResponseDTO;
import net.houdaifa.transactionservice.domain.TransferRequestDTO;
import net.houdaifa.transactionservice.domain.TransferResponseDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface TransactionService {
    TransactionResponseDTO createTransaction(TransactionRequestDTO request);
    TransferResponseDTO transfer(TransferRequestDTO request);
    List<TransactionResponseDTO> getAllTransactions();
    TransactionResponseDTO getTransactionById(UUID id);
    List<TransactionResponseDTO> getTransactionsByAccountId(UUID accountId);
    BigDecimal getMaxTransactionLimit();
}
