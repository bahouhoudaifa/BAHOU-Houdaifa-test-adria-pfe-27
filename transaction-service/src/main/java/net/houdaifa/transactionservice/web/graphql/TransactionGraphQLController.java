package net.houdaifa.transactionservice.web.graphql;

import lombok.RequiredArgsConstructor;
import net.houdaifa.transactionservice.domain.TransactionRequestDTO;
import net.houdaifa.transactionservice.domain.TransactionResponseDTO;
import net.houdaifa.transactionservice.domain.TransferRequestDTO;
import net.houdaifa.transactionservice.domain.TransferResponseDTO;
import net.houdaifa.transactionservice.service.TransactionService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class TransactionGraphQLController {

    private final TransactionService transactionService;

    @QueryMapping
    public List<TransactionResponseDTO> transactions() {
        return transactionService.getAllTransactions();
    }

    @QueryMapping
    public TransactionResponseDTO transactionById(@Argument UUID id) {
        return transactionService.getTransactionById(id);
    }

    @QueryMapping
    public List<TransactionResponseDTO> transactionsByAccountId(@Argument UUID accountId) {
        return transactionService.getTransactionsByAccountId(accountId);
    }

    @MutationMapping
    public TransactionResponseDTO createTransaction(@Argument TransactionInput input) {
        TransactionRequestDTO dto = new TransactionRequestDTO(
                input.accountId(),
                BigDecimal.valueOf(input.amount()),
                input.currency(),
                input.merchantCategory(),
                input.country()
        );
        return transactionService.createTransaction(dto);
    }

    @MutationMapping
    public TransferResponseDTO executeTransfer(@Argument TransferInput input) {
        TransferRequestDTO dto = new TransferRequestDTO(
                input.sourceAccountId(),
                input.destinationAccountId(),
                BigDecimal.valueOf(input.amount()),
                input.currency(),
                input.merchantCategory(),
                input.country()
        );
        return transactionService.transfer(dto);
    }

    public record TransactionInput(
            UUID accountId,
            double amount,
            String currency,
            String merchantCategory,
            String country
    ) {}

    public record TransferInput(
            UUID sourceAccountId,
            UUID destinationAccountId,
            double amount,
            String currency,
            String merchantCategory,
            String country
    ) {}
}
