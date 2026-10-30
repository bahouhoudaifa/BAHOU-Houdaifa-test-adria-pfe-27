package net.houdaifa.accountservice.service;

import net.houdaifa.accountservice.domain.*;
import net.houdaifa.accountservice.repository.AccountRepository;
import net.houdaifa.accountservice.service.exception.AccountBlockedException;
import net.houdaifa.accountservice.service.exception.AccountNotFoundException;
import net.houdaifa.accountservice.service.exception.InsufficientBalanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    private Account activeAccount;
    private Account blockedAccount;
    private UUID activeId;
    private UUID blockedId;

    @BeforeEach
    void setUp() {
        activeId = UUID.randomUUID();
        blockedId = UUID.randomUUID();

        activeAccount = Account.builder()
                .id(activeId)
                .customerReference("CUST-1")
                .currency("MAD")
                .balance(5000.0)
                .status(AccountStatus.ACTIVE)
                .build();

        blockedAccount = Account.builder()
                .id(blockedId)
                .customerReference("CUST-2")
                .currency("MAD")
                .balance(1000.0)
                .status(AccountStatus.BLOCKED)
                .build();
    }

    @Test
    void testCreateAccount() {
        when(accountRepository.save(any(Account.class))).thenReturn(activeAccount);

        AccountRequestDTO request = new AccountRequestDTO("CUST-1", "MAD", 5000.0, AccountStatus.ACTIVE);
        AccountResponseDTO response = accountService.createAccount(request);

        assertThat(response).isNotNull();
        assertThat(response.customerReference()).isEqualTo("CUST-1");
        assertThat(response.balance()).isEqualTo(5000.0);
        assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void testGetAccountByIdSuccess() {
        when(accountRepository.findById(activeId)).thenReturn(Optional.of(activeAccount));

        AccountResponseDTO response = accountService.getAccountById(activeId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(activeId);
        assertThat(response.currency()).isEqualTo("MAD");
    }

    @Test
    void testGetAccountByIdNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(accountRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccountById(unknownId))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Compte non trouvé");
    }

    @Test
    void testGetAllAccounts() {
        when(accountRepository.findAll()).thenReturn(List.of(activeAccount, blockedAccount));

        List<AccountResponseDTO> accounts = accountService.getAllAccounts();

        assertThat(accounts).hasSize(2);
    }

    @Test
    void testUpdateBalanceDebitSuccess() {
        when(accountRepository.findById(activeId)).thenReturn(Optional.of(activeAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateBalanceRequestDTO request = new UpdateBalanceRequestDTO(1000.0, "DEBIT");
        AccountResponseDTO response = accountService.updateBalance(activeId, request);

        assertThat(response.balance()).isEqualTo(4000.0);
    }

    @Test
    void testUpdateBalanceDebitInsufficientBalance() {
        when(accountRepository.findById(activeId)).thenReturn(Optional.of(activeAccount));

        UpdateBalanceRequestDTO request = new UpdateBalanceRequestDTO(10000.0, "DEBIT");

        assertThatThrownBy(() -> accountService.updateBalance(activeId, request))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Solde insuffisant");
    }

    @Test
    void testUpdateBalanceCreditSuccess() {
        when(accountRepository.findById(activeId)).thenReturn(Optional.of(activeAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateBalanceRequestDTO request = new UpdateBalanceRequestDTO(2000.0, "CREDIT");
        AccountResponseDTO response = accountService.updateBalance(activeId, request);

        assertThat(response.balance()).isEqualTo(7000.0);
    }

    @Test
    void testUpdateBalanceOnBlockedAccount() {
        when(accountRepository.findById(blockedId)).thenReturn(Optional.of(blockedAccount));

        UpdateBalanceRequestDTO request = new UpdateBalanceRequestDTO(100.0, "DEBIT");

        assertThatThrownBy(() -> accountService.updateBalance(blockedId, request))
                .isInstanceOf(AccountBlockedException.class)
                .hasMessageContaining("n'est pas actif");
    }

    @Test
    void testUpdateStatus() {
        when(accountRepository.findById(activeId)).thenReturn(Optional.of(activeAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponseDTO response = accountService.updateAccountStatus(activeId, AccountStatus.BLOCKED);

        assertThat(response.status()).isEqualTo(AccountStatus.BLOCKED);
    }
}
