package net.houdaifa.accountservice;

import net.houdaifa.accountservice.domain.AccountRequestDTO;
import net.houdaifa.accountservice.domain.AccountStatus;
import net.houdaifa.accountservice.service.AccountService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner initData(AccountService accountService) {
        return args -> {
            accountService.createAccount(new AccountRequestDTO("CUST-1001", "MAD", 15000.0, AccountStatus.ACTIVE));
            accountService.createAccount(new AccountRequestDTO("CUST-1002", "MAD", 5000.0, AccountStatus.ACTIVE));
            accountService.createAccount(new AccountRequestDTO("CUST-1003", "EUR", 2500.0, AccountStatus.ACTIVE));
            accountService.createAccount(new AccountRequestDTO("CUST-1004", "MAD", 1000.0, AccountStatus.BLOCKED));
        };
    }
}
