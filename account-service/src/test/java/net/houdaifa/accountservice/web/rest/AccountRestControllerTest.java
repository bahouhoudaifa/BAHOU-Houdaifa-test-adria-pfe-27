package net.houdaifa.accountservice.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.houdaifa.accountservice.domain.AccountRequestDTO;
import net.houdaifa.accountservice.domain.AccountResponseDTO;
import net.houdaifa.accountservice.domain.AccountStatus;
import net.houdaifa.accountservice.domain.UpdateBalanceRequestDTO;
import net.houdaifa.accountservice.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountRestController.class)
class AccountRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountService accountService;

    @Test
    void testCreateAccount() throws Exception {
        UUID id = UUID.randomUUID();
        AccountResponseDTO response = new AccountResponseDTO(id, "CUST-10", AccountStatus.ACTIVE, "EUR", 1000.0);
        when(accountService.createAccount(any(AccountRequestDTO.class))).thenReturn(response);

        AccountRequestDTO request = new AccountRequestDTO("CUST-10", "EUR", 1000.0, AccountStatus.ACTIVE);

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.customerReference").value("CUST-10"))
                .andExpect(jsonPath("$.balance").value(1000.0));
    }

    @Test
    void testGetAccountById() throws Exception {
        UUID id = UUID.randomUUID();
        AccountResponseDTO response = new AccountResponseDTO(id, "CUST-10", AccountStatus.ACTIVE, "EUR", 1000.0);
        when(accountService.getAccountById(id)).thenReturn(response);

        mockMvc.perform(get("/api/accounts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    void testGetAllAccounts() throws Exception {
        UUID id = UUID.randomUUID();
        AccountResponseDTO response = new AccountResponseDTO(id, "CUST-10", AccountStatus.ACTIVE, "EUR", 1000.0);
        when(accountService.getAllAccounts()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerReference").value("CUST-10"));
    }

    @Test
    void testUpdateBalance() throws Exception {
        UUID id = UUID.randomUUID();
        AccountResponseDTO response = new AccountResponseDTO(id, "CUST-10", AccountStatus.ACTIVE, "EUR", 800.0);
        UpdateBalanceRequestDTO request = new UpdateBalanceRequestDTO(200.0, "DEBIT");

        when(accountService.updateBalance(eq(id), any(UpdateBalanceRequestDTO.class))).thenReturn(response);

        mockMvc.perform(put("/api/accounts/{id}/balance", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(800.0));
    }
}
