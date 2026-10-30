package net.houdaifa.accountservice.web.rest;

import lombok.RequiredArgsConstructor;
import net.houdaifa.accountservice.domain.AccountRequestDTO;
import net.houdaifa.accountservice.domain.AccountResponseDTO;
import net.houdaifa.accountservice.domain.UpdateBalanceRequestDTO;
import net.houdaifa.accountservice.domain.UpdateStatusRequestDTO;
import net.houdaifa.accountservice.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountRestController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponseDTO> createAccount(@RequestBody AccountRequestDTO request) {
        AccountResponseDTO created = accountService.createAccount(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponseDTO> getAccountById(@PathVariable UUID id) {
        return ResponseEntity.ok(accountService.getAccountById(id));
    }

    @GetMapping
    public ResponseEntity<List<AccountResponseDTO>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @PutMapping("/{id}/balance")
    public ResponseEntity<AccountResponseDTO> updateBalance(
            @PathVariable UUID id,
            @RequestBody UpdateBalanceRequestDTO request
    ) {
        return ResponseEntity.ok(accountService.updateBalance(id, request));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<AccountResponseDTO> updateStatus(
            @PathVariable UUID id,
            @RequestBody UpdateStatusRequestDTO request
    ) {
        return ResponseEntity.ok(accountService.updateAccountStatus(id, request.status()));
    }
}
