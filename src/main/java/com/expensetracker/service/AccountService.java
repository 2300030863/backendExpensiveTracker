package com.expensetracker.service;

import com.expensetracker.entity.Account;
import com.expensetracker.entity.User;
import com.expensetracker.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    public List<Account> getAllAccounts(User user) {
        return accountRepository.findByUserAndIsActiveTrueOrderByName(user);
    }

    public Account getAccount(User user, Long id) {
        return accountRepository.findById(id)
                .filter(account -> account.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    public Account createAccount(User user, Account account) {
        account.setUser(user);
        return accountRepository.save(account);
    }

    public Account updateAccount(User user, Long id, Account accountDetails) {
        Account account = getAccount(user, id);
        
        account.setName(accountDetails.getName());
        account.setDescription(accountDetails.getDescription());
        account.setType(accountDetails.getType());
        account.setIsActive(accountDetails.getIsActive());
        account.setBalance(accountDetails.getBalance());
        
        return accountRepository.save(account);
    }

    public void deleteAccount(User user, Long id) {
        Account account = getAccount(user, id);
        account.setIsActive(false);
        accountRepository.save(account);
    }
}



