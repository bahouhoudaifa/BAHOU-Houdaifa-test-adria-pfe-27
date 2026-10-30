package net.houdaifa.transactionservice.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.houdaifa.transactionservice.domain.*;
import net.houdaifa.transactionservice.feign.AccountRestClient;
import net.houdaifa.transactionservice.repository.TransactionRepository;
import net.houdaifa.transactionservice.service.exception.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRestClient accountRestClient;

    @Value("${bank.transaction.max-amount:${transaction.max-limit:1000.00}}")
    private BigDecimal maxTransactionLimit;

    @Override
    public BigDecimal getMaxTransactionLimit() {
        return maxTransactionLimit;
    }

    @Override
    @CircuitBreaker(name = "accountServiceCircuitBreaker")
    @Retry(name = "accountServiceRetry")
    public TransactionResponseDTO createTransaction(TransactionRequestDTO request) {
        log.info("Processing transaction request for account: {}, amount: {} {}",
                request.accountId(), request.amount(), request.currency());

        // 1. Validation du montant strictement positif
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Le montant de la transaction doit être strictement positif.");
        }

        // 2. Validation du respect du plafond configuré
        if (request.amount().compareTo(maxTransactionLimit) > 0) {
            throw new TransactionLimitExceededException(
                    "Le montant dépasse le plafond autorisé de " + maxTransactionLimit + " " + request.currency());
        }

        // 3. Consultation du compte via OpenFeign
        AccountDTO account = accountRestClient.getAccountById(request.accountId());
        if (account == null) {
            throw new AccountNotFoundException("Compte introuvable avec l'identifiant: " + request.accountId());
        }

        // 4. Vérification que le compte est actif
        if (!"ACTIVE".equalsIgnoreCase(account.status())) {
            throw new AccountBlockedException(
                    "Le compte " + request.accountId() + " n'est pas actif (statut: " + account.status() + "). Transaction refusée.");
        }

        // 5. Vérification que la devise correspond à celle du compte
        if (!account.currency().equalsIgnoreCase(request.currency())) {
            throw new CurrencyMismatchException(
                    "La devise de la transaction (" + request.currency() + ") ne correspond pas à celle du compte (" + account.currency() + ").");
        }

        // 6. Consultation du solde du compte et vérification
        if (BigDecimal.valueOf(account.balance()).compareTo(request.amount()) < 0) {
            throw new InsufficientBalanceException(
                    "Solde insuffisant (" + account.balance() + " " + account.currency() + ") pour effectuer un débit de " + request.amount());
        }

        // 7. Enregistrement de la transaction
        Transaction transaction = Transaction.builder()
                .accountId(request.accountId())
                .amount(request.amount())
                .currency(request.currency().toUpperCase())
                .merchantCategory(request.merchantCategory() != null ? request.merchantCategory() : "GENERAL")
                .country(request.country() != null ? request.country().toUpperCase() : "MA")
                .createdAt(Instant.now())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Transaction saved with id: {}", savedTransaction.getId());

        // 8. Mise à jour du solde du compte après enregistrement
        accountRestClient.updateBalance(
                request.accountId(),
                new UpdateBalanceRequestDTO(request.amount().doubleValue(), "DEBIT")
        );
        log.info("Account {} balance updated successfully (debited {})", request.accountId(), request.amount());

        return TransactionResponseDTO.fromEntity(savedTransaction);
    }

    @Override
    @CircuitBreaker(name = "accountServiceCircuitBreaker")
    @Retry(name = "accountServiceRetry")
    public TransferResponseDTO transfer(TransferRequestDTO request) {
        log.info("Processing transfer from {} to {}, amount: {} {}",
                request.sourceAccountId(), request.destinationAccountId(), request.amount(), request.currency());

        // 1. Validation du montant strictement positif
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Le montant du virement doit être strictement positif.");
        }

        // 2. Validation du plafond
        if (request.amount().compareTo(maxTransactionLimit) > 0) {
            throw new TransactionLimitExceededException(
                    "Le montant dépasse le plafond autorisé de " + maxTransactionLimit + " " + request.currency());
        }

        // 3. Comptes source et destination distincts
        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new IllegalArgumentException("Les comptes source et destination doivent être différents.");
        }

        // 4. Consultation et vérification du compte source
        AccountDTO sourceAccount = accountRestClient.getAccountById(request.sourceAccountId());
        if (sourceAccount == null) {
            throw new AccountNotFoundException("Compte source introuvable: " + request.sourceAccountId());
        }
        if (!"ACTIVE".equalsIgnoreCase(sourceAccount.status())) {
            throw new AccountBlockedException("Le compte source n'est pas actif (statut: " + sourceAccount.status() + ").");
        }
        if (!sourceAccount.currency().equalsIgnoreCase(request.currency())) {
            throw new CurrencyMismatchException("La devise du virement ne correspond pas à la devise du compte source.");
        }
        if (BigDecimal.valueOf(sourceAccount.balance()).compareTo(request.amount()) < 0) {
            throw new InsufficientBalanceException("Solde insuffisant sur le compte source pour effectuer le virement.");
        }

        // 5. Consultation et vérification du compte destination
        AccountDTO destAccount = accountRestClient.getAccountById(request.destinationAccountId());
        if (destAccount == null) {
            throw new AccountNotFoundException("Compte destination introuvable: " + request.destinationAccountId());
        }
        if (!"ACTIVE".equalsIgnoreCase(destAccount.status())) {
            throw new AccountBlockedException("Le compte destination n'est pas actif (statut: " + destAccount.status() + ").");
        }
        if (!destAccount.currency().equalsIgnoreCase(request.currency())) {
            throw new CurrencyMismatchException("La devise du virement ne correspond pas à la devise du compte destination.");
        }

        Instant now = Instant.now();

        // 6. Enregistrement transaction débit sur compte source
        Transaction debitTx = Transaction.builder()
                .accountId(request.sourceAccountId())
                .amount(request.amount())
                .currency(request.currency().toUpperCase())
                .merchantCategory(request.merchantCategory() != null ? request.merchantCategory() : "TRANSFER_OUT")
                .country(request.country() != null ? request.country().toUpperCase() : "MA")
                .createdAt(now)
                .build();
        Transaction savedDebit = transactionRepository.save(debitTx);

        // 7. Enregistrement transaction crédit sur compte destination
        Transaction creditTx = Transaction.builder()
                .accountId(request.destinationAccountId())
                .amount(request.amount())
                .currency(request.currency().toUpperCase())
                .merchantCategory(request.merchantCategory() != null ? request.merchantCategory() : "TRANSFER_IN")
                .country(request.country() != null ? request.country().toUpperCase() : "MA")
                .createdAt(now)
                .build();
        Transaction savedCredit = transactionRepository.save(creditTx);

        // 8. Mise à jour des soldes sur les deux comptes via Feign
        accountRestClient.updateBalance(
                request.sourceAccountId(),
                new UpdateBalanceRequestDTO(request.amount().doubleValue(), "DEBIT")
        );
        accountRestClient.updateBalance(
                request.destinationAccountId(),
                new UpdateBalanceRequestDTO(request.amount().doubleValue(), "CREDIT")
        );

        log.info("Transfer completed successfully from {} to {}", request.sourceAccountId(), request.destinationAccountId());

        return new TransferResponseDTO(
                TransactionResponseDTO.fromEntity(savedDebit),
                TransactionResponseDTO.fromEntity(savedCredit),
                "Virement de " + request.amount() + " " + request.currency() + " effectué avec succès."
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponseDTO> getAllTransactions() {
        return transactionRepository.findAll().stream()
                .map(TransactionResponseDTO::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponseDTO getTransactionById(UUID id) {
        return transactionRepository.findById(id)
                .map(TransactionResponseDTO::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException("Transaction introuvable avec l'identifiant: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponseDTO> getTransactionsByAccountId(UUID accountId) {
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId).stream()
                .map(TransactionResponseDTO::fromEntity)
                .toList();
    }
}
