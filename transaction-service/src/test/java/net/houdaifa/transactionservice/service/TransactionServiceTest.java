package net.houdaifa.transactionservice.service;

import net.houdaifa.transactionservice.domain.*;
import net.houdaifa.transactionservice.feign.AccountRestClient;
import net.houdaifa.transactionservice.repository.TransactionRepository;
import net.houdaifa.transactionservice.service.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRestClient accountRestClient;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private UUID accountId1;
    private UUID accountId2;
    private AccountDTO activeAccount1;
    private AccountDTO activeAccount2;
    private AccountDTO blockedAccount;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(transactionService, "maxTransactionLimit", new BigDecimal("10000.00"));

        accountId1 = UUID.randomUUID();
        accountId2 = UUID.randomUUID();

        activeAccount1 = new AccountDTO(accountId1, "CUST-1", "ACTIVE", "MAD", 8000.0);
        activeAccount2 = new AccountDTO(accountId2, "CUST-2", "ACTIVE", "MAD", 3000.0);
        blockedAccount = new AccountDTO(accountId1, "CUST-1", "BLOCKED", "MAD", 8000.0);
    }

    @Test
    void testCreateTransactionSuccess() {
        when(accountRestClient.getAccountById(accountId1)).thenReturn(activeAccount1);

        Transaction savedTx = Transaction.builder()
                .id(UUID.randomUUID())
                .accountId(accountId1)
                .amount(new BigDecimal("1500.00"))
                .currency("MAD")
                .merchantCategory("SUPERMARKET")
                .country("MA")
                .createdAt(Instant.now())
                .build();
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);

        TransactionRequestDTO request = new TransactionRequestDTO(
                accountId1,
                new BigDecimal("1500.00"),
                "MAD",
                "SUPERMARKET",
                "MA"
        );

        TransactionResponseDTO response = transactionService.createTransaction(request);

        assertThat(response).isNotNull();
        assertThat(response.amount()).isEqualTo(new BigDecimal("1500.00"));
        assertThat(response.currency()).isEqualTo("MAD");
        verify(accountRestClient, times(1)).updateBalance(eq(accountId1), any(UpdateBalanceRequestDTO.class));
    }

    @Test
    void testCreateTransactionInvalidAmountNegativeOrZero() {
        TransactionRequestDTO requestZero = new TransactionRequestDTO(
                accountId1, BigDecimal.ZERO, "MAD", "GENERAL", "MA"
        );

        assertThatThrownBy(() -> transactionService.createTransaction(requestZero))
                .isInstanceOf(InvalidAmountException.class)
                .hasMessageContaining("strictement positif");
    }

    @Test
    void testCreateTransactionExceedsLimit() {
        TransactionRequestDTO requestExceeded = new TransactionRequestDTO(
                accountId1, new BigDecimal("15000.00"), "MAD", "GENERAL", "MA"
        );

        assertThatThrownBy(() -> transactionService.createTransaction(requestExceeded))
                .isInstanceOf(TransactionLimitExceededException.class)
                .hasMessageContaining("dépasse le plafond");
    }

    @Test
    void testCreateTransactionBlockedAccount() {
        when(accountRestClient.getAccountById(accountId1)).thenReturn(blockedAccount);

        TransactionRequestDTO request = new TransactionRequestDTO(
                accountId1, new BigDecimal("500.00"), "MAD", "GENERAL", "MA"
        );

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(AccountBlockedException.class)
                .hasMessageContaining("n'est pas actif");
    }

    @Test
    void testCreateTransactionCurrencyMismatch() {
        when(accountRestClient.getAccountById(accountId1)).thenReturn(activeAccount1);

        TransactionRequestDTO request = new TransactionRequestDTO(
                accountId1, new BigDecimal("500.00"), "EUR", "GENERAL", "FR"
        );

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(CurrencyMismatchException.class)
                .hasMessageContaining("devise de la transaction");
    }

    @Test
    void testCreateTransactionInsufficientBalance() {
        when(accountRestClient.getAccountById(accountId1)).thenReturn(activeAccount1); // balance = 8000

        TransactionRequestDTO request = new TransactionRequestDTO(
                accountId1, new BigDecimal("9500.00"), "MAD", "GENERAL", "MA"
        );

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Solde insuffisant");
    }

    @Test
    void testTransferSuccess() {
        when(accountRestClient.getAccountById(accountId1)).thenReturn(activeAccount1);
        when(accountRestClient.getAccountById(accountId2)).thenReturn(activeAccount2);

        Transaction debit = Transaction.builder()
                .id(UUID.randomUUID()).accountId(accountId1).amount(new BigDecimal("2000.00")).currency("MAD")
                .merchantCategory("TRANSFER_OUT").country("MA").createdAt(Instant.now()).build();
        Transaction credit = Transaction.builder()
                .id(UUID.randomUUID()).accountId(accountId2).amount(new BigDecimal("2000.00")).currency("MAD")
                .merchantCategory("TRANSFER_IN").country("MA").createdAt(Instant.now()).build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(debit).thenReturn(credit);

        TransferRequestDTO request = new TransferRequestDTO(
                accountId1, accountId2, new BigDecimal("2000.00"), "MAD", "TRANSFER", "MA"
        );

        TransferResponseDTO response = transactionService.transfer(request);

        assertThat(response).isNotNull();
        assertThat(response.debitTransaction()).isNotNull();
        assertThat(response.creditTransaction()).isNotNull();
        assertThat(response.message()).contains("effectué avec succès");
        verify(accountRestClient, times(2)).updateBalance(any(UUID.class), any(UpdateBalanceRequestDTO.class));
    }

    @Test
    void testTransferSameAccountThrows() {
        TransferRequestDTO request = new TransferRequestDTO(
                accountId1, accountId1, new BigDecimal("500.00"), "MAD", "TRANSFER", "MA"
        );

        assertThatThrownBy(() -> transactionService.transfer(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("différents");
    }

    @Test
    void testGetAllTransactions() {
        Transaction tx = Transaction.builder()
                .id(UUID.randomUUID()).accountId(accountId1).amount(new BigDecimal("100.00")).currency("MAD")
                .merchantCategory("GENERAL").country("MA").createdAt(Instant.now()).build();
        when(transactionRepository.findAll()).thenReturn(List.of(tx));

        List<TransactionResponseDTO> list = transactionService.getAllTransactions();

        assertThat(list).hasSize(1);
    }

    @Test
    void testGetTransactionsByAccountId() {
        Transaction tx = Transaction.builder()
                .id(UUID.randomUUID()).accountId(accountId1).amount(new BigDecimal("100.00")).currency("MAD")
                .merchantCategory("GENERAL").country("MA").createdAt(Instant.now()).build();
        when(transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId1)).thenReturn(List.of(tx));

        List<TransactionResponseDTO> list = transactionService.getTransactionsByAccountId(accountId1);

        assertThat(list).hasSize(1);
    }
}
