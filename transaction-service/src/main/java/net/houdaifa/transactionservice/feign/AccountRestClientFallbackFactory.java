package net.houdaifa.transactionservice.feign;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import net.houdaifa.transactionservice.domain.AccountDTO;
import net.houdaifa.transactionservice.domain.UpdateBalanceRequestDTO;
import net.houdaifa.transactionservice.service.exception.AccountNotFoundException;
import net.houdaifa.transactionservice.service.exception.AccountServiceUnavailableException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class AccountRestClientFallbackFactory implements FallbackFactory<AccountRestClient> {

    @Override
    public AccountRestClient create(Throwable cause) {
        return new AccountRestClient() {
            @Override
            public AccountDTO getAccountById(UUID id) {
                log.error("Fallback triggered for getAccountById({}), reason: {}", id, cause.getMessage());
                if (cause instanceof FeignException.NotFound) {
                    throw new AccountNotFoundException("Compte non trouvé avec l'identifiant: " + id);
                }
                throw new AccountServiceUnavailableException(
                        "Impossible de contacter account-service pour consulter le compte " + id + " : " + cause.getMessage(),
                        cause
                );
            }

            @Override
            public AccountDTO updateBalance(UUID id, UpdateBalanceRequestDTO request) {
                log.error("Fallback triggered for updateBalance({}), reason: {}", id, cause.getMessage());
                if (cause instanceof FeignException.NotFound) {
                    throw new AccountNotFoundException("Compte non trouvé pour mise à jour de solde: " + id);
                }
                throw new AccountServiceUnavailableException(
                        "Impossible de contacter account-service pour mettre à jour le solde du compte " + id + " : " + cause.getMessage(),
                        cause
                );
            }
        };
    }
}
