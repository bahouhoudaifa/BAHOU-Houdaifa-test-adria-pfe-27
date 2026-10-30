package net.houdaifa.transactionservice.mcp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.houdaifa.transactionservice.domain.TransactionRequestDTO;
import net.houdaifa.transactionservice.domain.TransactionResponseDTO;
import net.houdaifa.transactionservice.domain.TransferRequestDTO;
import net.houdaifa.transactionservice.domain.TransferResponseDTO;
import net.houdaifa.transactionservice.service.TransactionService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionMcpTools {

    private final TransactionService transactionService;

    @Tool(description = "Enregistrer une nouvelle transaction bancaire pour un compte (débit avec consultation et mise à jour du solde)")
    public TransactionResponseDTO createTransaction(
            UUID accountId,
            double amount,
            String currency,
            String merchantCategory,
            String country
    ) {
        log.info("MCP Tool: createTransaction called for accountId={}, amount={}", accountId, amount);
        TransactionRequestDTO dto = new TransactionRequestDTO(
                accountId,
                BigDecimal.valueOf(amount),
                currency,
                merchantCategory,
                country
        );
        return transactionService.createTransaction(dto);
    }

    @Tool(description = "Effectuer un virement bancaire entre deux comptes avec vérification des soldes et devises")
    public TransferResponseDTO executeTransfer(
            UUID sourceAccountId,
            UUID destinationAccountId,
            double amount,
            String currency,
            String merchantCategory,
            String country
    ) {
        log.info("MCP Tool: executeTransfer called from {} to {}, amount={}", sourceAccountId, destinationAccountId, amount);
        TransferRequestDTO dto = new TransferRequestDTO(
                sourceAccountId,
                destinationAccountId,
                BigDecimal.valueOf(amount),
                currency,
                merchantCategory,
                country
        );
        return transactionService.transfer(dto);
    }

    @Tool(description = "Lister toutes les transactions enregistrées")
    public List<TransactionResponseDTO> getAllTransactions() {
        log.info("MCP Tool: getAllTransactions called");
        return transactionService.getAllTransactions();
    }

    @Tool(description = "Obtenir une transaction spécifique par son identifiant UUID")
    public TransactionResponseDTO getTransactionById(UUID id) {
        log.info("MCP Tool: getTransactionById called with id={}", id);
        return transactionService.getTransactionById(id);
    }

    @Tool(description = "Lister toutes les transactions associées à un compte bancaire")
    public List<TransactionResponseDTO> getTransactionsByAccountId(UUID accountId) {
        log.info("MCP Tool: getTransactionsByAccountId called with accountId={}", accountId);
        return transactionService.getTransactionsByAccountId(accountId);
    }
}
