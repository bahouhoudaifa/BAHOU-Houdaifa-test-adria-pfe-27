package net.houdaifa.accountservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.houdaifa.accountservice.domain.*;
import net.houdaifa.accountservice.repository.AccountRepository;
import net.houdaifa.accountservice.service.exception.AccountBlockedException;
import net.houdaifa.accountservice.service.exception.AccountNotFoundException;
import net.houdaifa.accountservice.service.exception.InsufficientBalanceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    @Override
    public AccountResponseDTO createAccount(AccountRequestDTO request) {
        log.info("Creating new account for customerReference: {}", request.customerReference());
        AccountStatus status = request.status() != null ? request.status() : AccountStatus.ACTIVE;
        Account account = Account.builder()
                .customerReference(request.customerReference())
                .currency(request.currency() != null ? request.currency().toUpperCase() : "EUR")
                .balance(request.initialBalance())
                .status(status)
                .build();

        Account saved = accountRepository.save(account);
        return AccountResponseDTO.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponseDTO getAccountById(UUID id) {
        log.info("Fetching account by id: {}", id);
        return accountRepository.findById(id)
                .map(AccountResponseDTO::fromEntity)
                .orElseThrow(() -> new AccountNotFoundException("Compte non trouvé avec l'identifiant: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponseDTO> getAllAccounts() {
        log.info("Fetching all accounts");
        return accountRepository.findAll().stream()
                .map(AccountResponseDTO::fromEntity)
                .toList();
    }

    @Override
    public AccountResponseDTO updateBalance(UUID id, UpdateBalanceRequestDTO request) {
        log.info("Updating balance for account id: {}, amount: {}, operation: {}", id, request.amount(), request.operationType());
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Compte non trouvé avec l'identifiant: " + id));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountBlockedException("Le compte " + id + " n'est pas actif (statut: " + account.getStatus() + "). Opération refusée.");
        }

        if (request.amount() < 0) {
            throw new IllegalArgumentException("Le montant de la mise à jour doit être positif.");
        }

        String op = request.operationType() != null ? request.operationType().trim().toUpperCase() : "DEBIT";
        if ("DEBIT".equals(op)) {
            if (account.getBalance() < request.amount()) {
                throw new InsufficientBalanceException("Solde insuffisant sur le compte " + id + ". Solde actuel: " + account.getBalance() + ", Montant requis: " + request.amount());
            }
            account.setBalance(account.getBalance() - request.amount());
        } else if ("CREDIT".equals(op)) {
            account.setBalance(account.getBalance() + request.amount());
        } else {
            throw new IllegalArgumentException("Type d'opération non supporté: " + request.operationType() + ". Utilisez DEBIT ou CREDIT.");
        }

        Account updated = accountRepository.save(account);
        return AccountResponseDTO.fromEntity(updated);
    }

    @Override
    public AccountResponseDTO updateAccountStatus(UUID id, AccountStatus status) {
        log.info("Updating status for account id: {} to {}", id, status);
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Compte non trouvé avec l'identifiant: " + id));

        account.setStatus(status);
        Account updated = accountRepository.save(account);
        return AccountResponseDTO.fromEntity(updated);
    }
}
