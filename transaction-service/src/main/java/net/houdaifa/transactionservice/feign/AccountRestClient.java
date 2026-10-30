package net.houdaifa.transactionservice.feign;

import net.houdaifa.transactionservice.domain.AccountDTO;
import net.houdaifa.transactionservice.domain.UpdateBalanceRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(
        name = "account-service",
        url = "${account-service.url:http://localhost:8081}",
        fallbackFactory = AccountRestClientFallbackFactory.class
)
public interface AccountRestClient {

    @GetMapping("/api/accounts/{id}")
    AccountDTO getAccountById(@PathVariable("id") UUID id);

    @PutMapping("/api/accounts/{id}/balance")
    AccountDTO updateBalance(@PathVariable("id") UUID id, @RequestBody UpdateBalanceRequestDTO request);
}
