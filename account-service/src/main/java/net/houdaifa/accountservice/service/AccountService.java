package net.houdaifa.accountservice.service;

import net.houdaifa.accountservice.domain.AccountRequestDTO;
import net.houdaifa.accountservice.domain.AccountResponseDTO;
import net.houdaifa.accountservice.domain.AccountStatus;
import net.houdaifa.accountservice.domain.UpdateBalanceRequestDTO;

import java.util.List;
import java.util.UUID;

public interface AccountService {
    AccountResponseDTO createAccount(AccountRequestDTO request);
    AccountResponseDTO getAccountById(UUID id);
    List<AccountResponseDTO> getAllAccounts();
    AccountResponseDTO updateBalance(UUID id, UpdateBalanceRequestDTO request);
    AccountResponseDTO updateAccountStatus(UUID id, AccountStatus status);
}
