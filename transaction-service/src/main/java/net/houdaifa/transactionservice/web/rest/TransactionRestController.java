package net.houdaifa.transactionservice.web.rest;

import lombok.RequiredArgsConstructor;
import net.houdaifa.transactionservice.domain.TransactionRequestDTO;
import net.houdaifa.transactionservice.domain.TransactionResponseDTO;
import net.houdaifa.transactionservice.domain.TransferRequestDTO;
import net.houdaifa.transactionservice.domain.TransferResponseDTO;
import net.houdaifa.transactionservice.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionRestController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponseDTO> createTransaction(@RequestBody TransactionRequestDTO request) {
        TransactionResponseDTO created = transactionService.createTransaction(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponseDTO> transfer(@RequestBody TransferRequestDTO request) {
        TransferResponseDTO response = transactionService.transfer(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponseDTO>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponseDTO> getTransactionById(@PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<TransactionResponseDTO>> getTransactionsByAccountId(@PathVariable UUID accountId) {
        return ResponseEntity.ok(transactionService.getTransactionsByAccountId(accountId));
    }

    @GetMapping("/config/max-amount")
    public ResponseEntity<Map<String, Object>> getMaxAmountConfig() {
        BigDecimal maxAmount = transactionService.getMaxTransactionLimit();
        return ResponseEntity.ok(Map.of(
                "parameter", "bank.transaction.max-amount",
                "value", maxAmount,
                "source", "config-service"
        ));
    }
}
