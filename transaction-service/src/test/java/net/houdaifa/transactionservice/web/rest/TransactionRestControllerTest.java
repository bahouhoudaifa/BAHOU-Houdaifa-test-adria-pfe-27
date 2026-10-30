package net.houdaifa.transactionservice.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.houdaifa.transactionservice.domain.TransactionRequestDTO;
import net.houdaifa.transactionservice.domain.TransactionResponseDTO;
import net.houdaifa.transactionservice.domain.TransferRequestDTO;
import net.houdaifa.transactionservice.domain.TransferResponseDTO;
import net.houdaifa.transactionservice.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionRestController.class)
class TransactionRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    void testCreateTransaction() throws Exception {
        UUID txId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        TransactionResponseDTO response = new TransactionResponseDTO(
                txId, accountId, new BigDecimal("120.50"), "MAD", "RESTAURANT", "MA", Instant.now()
        );

        when(transactionService.createTransaction(any(TransactionRequestDTO.class))).thenReturn(response);

        TransactionRequestDTO request = new TransactionRequestDTO(
                accountId, new BigDecimal("120.50"), "MAD", "RESTAURANT", "MA"
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(txId.toString()))
                .andExpect(jsonPath("$.amount").value(120.50))
                .andExpect(jsonPath("$.currency").value("MAD"));
    }

    @Test
    void testTransfer() throws Exception {
        UUID txId1 = UUID.randomUUID();
        UUID txId2 = UUID.randomUUID();
        UUID acc1 = UUID.randomUUID();
        UUID acc2 = UUID.randomUUID();

        TransactionResponseDTO debit = new TransactionResponseDTO(
                txId1, acc1, new BigDecimal("500.00"), "MAD", "TRANSFER_OUT", "MA", Instant.now()
        );
        TransactionResponseDTO credit = new TransactionResponseDTO(
                txId2, acc2, new BigDecimal("500.00"), "MAD", "TRANSFER_IN", "MA", Instant.now()
        );
        TransferResponseDTO transferResponse = new TransferResponseDTO(debit, credit, "Virement effectué avec succès");

        when(transactionService.transfer(any(TransferRequestDTO.class))).thenReturn(transferResponse);

        TransferRequestDTO request = new TransferRequestDTO(
                acc1, acc2, new BigDecimal("500.00"), "MAD", "TRANSFER", "MA"
        );

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Virement effectué avec succès"))
                .andExpect(jsonPath("$.debitTransaction.id").value(txId1.toString()))
                .andExpect(jsonPath("$.creditTransaction.id").value(txId2.toString()));
    }

    @Test
    void testGetAllTransactions() throws Exception {
        UUID txId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        TransactionResponseDTO response = new TransactionResponseDTO(
                txId, accountId, new BigDecimal("100.00"), "MAD", "SHOPPING", "MA", Instant.now()
        );

        when(transactionService.getAllTransactions()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].merchantCategory").value("SHOPPING"));
    }

    @Test
    void testGetTransactionsByAccountId() throws Exception {
        UUID txId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        TransactionResponseDTO response = new TransactionResponseDTO(
                txId, accountId, new BigDecimal("100.00"), "MAD", "SHOPPING", "MA", Instant.now()
        );

        when(transactionService.getTransactionsByAccountId(accountId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/transactions/account/{accountId}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountId").value(accountId.toString()));
    }
}
