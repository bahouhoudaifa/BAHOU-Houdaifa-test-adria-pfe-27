package net.houdaifa.accountservice.web.graphql;

import lombok.RequiredArgsConstructor;
import net.houdaifa.accountservice.domain.AccountRequestDTO;
import net.houdaifa.accountservice.domain.AccountResponseDTO;
import net.houdaifa.accountservice.domain.AccountStatus;
import net.houdaifa.accountservice.domain.UpdateBalanceRequestDTO;
import net.houdaifa.accountservice.service.AccountService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class AccountGraphQLController {

    private final AccountService accountService;

    @QueryMapping
    public List<AccountResponseDTO> accounts() {
        return accountService.getAllAccounts();
    }

    @QueryMapping
    public AccountResponseDTO accountById(@Argument UUID id) {
        return accountService.getAccountById(id);
    }

    @MutationMapping
    public AccountResponseDTO createAccount(@Argument AccountInput input) {
        AccountRequestDTO dto = new AccountRequestDTO(
                input.customerReference(),
                input.currency(),
                input.initialBalance(),
                input.status()
        );
        return accountService.createAccount(dto);
    }

    @MutationMapping
    public AccountResponseDTO updateBalance(@Argument BalanceUpdateInput input) {
        UpdateBalanceRequestDTO dto = new UpdateBalanceRequestDTO(
                input.amount(),
                input.operationType()
        );
        return accountService.updateBalance(input.accountId(), dto);
    }

    @MutationMapping
    public AccountResponseDTO updateStatus(@Argument UUID id, @Argument AccountStatus status) {
        return accountService.updateAccountStatus(id, status);
    }

    public record AccountInput(
            String customerReference,
            String currency,
            double initialBalance,
            AccountStatus status
    ) {}

    public record BalanceUpdateInput(
            UUID accountId,
            double amount,
            String operationType
    ) {}
}
