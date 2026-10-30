package net.houdaifa.accountservice.mcp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.houdaifa.accountservice.domain.AccountRequestDTO;
import net.houdaifa.accountservice.domain.AccountResponseDTO;
import net.houdaifa.accountservice.domain.AccountStatus;
import net.houdaifa.accountservice.domain.UpdateBalanceRequestDTO;
import net.houdaifa.accountservice.service.AccountService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountMcpTools {

    private final AccountService accountService;

    @Tool(description = "Obtenir les détails d'un compte bancaire par son identifiant UUID")
    public AccountResponseDTO getAccountById(UUID accountId) {
        log.info("MCP Tool: getAccountById called with id={}", accountId);
        return accountService.getAccountById(accountId);
    }

    @Tool(description = "Obtenir la liste complète de tous les comptes bancaires")
    public List<AccountResponseDTO> getAllAccounts() {
        log.info("MCP Tool: getAllAccounts called");
        return accountService.getAllAccounts();
    }

    @Tool(description = "Créer un nouveau compte bancaire avec sa référence client, sa devise et son solde initial")
    public AccountResponseDTO createAccount(String customerReference, String currency, double initialBalance) {
        log.info("MCP Tool: createAccount called for ref={}", customerReference);
        AccountRequestDTO dto = new AccountRequestDTO(customerReference, currency, initialBalance, AccountStatus.ACTIVE);
        return accountService.createAccount(dto);
    }

    @Tool(description = "Mettre à jour le solde d'un compte bancaire par débit ou crédit (operationType: DEBIT ou CREDIT)")
    public AccountResponseDTO updateAccountBalance(UUID accountId, double amount, String operationType) {
        log.info("MCP Tool: updateAccountBalance called for id={}, amount={}, op={}", accountId, amount, operationType);
        UpdateBalanceRequestDTO dto = new UpdateBalanceRequestDTO(amount, operationType);
        return accountService.updateBalance(accountId, dto);
    }

    @Tool(description = "Mettre à jour le statut d'un compte bancaire (ACTIVE ou BLOCKED)")
    public AccountResponseDTO updateAccountStatus(UUID accountId, String status) {
        log.info("MCP Tool: updateAccountStatus called for id={}, status={}", accountId, status);
        AccountStatus accountStatus = AccountStatus.valueOf(status.toUpperCase());
        return accountService.updateAccountStatus(accountId, accountStatus);
    }
}
