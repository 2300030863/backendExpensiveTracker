package com.expensetracker.controller;

import com.expensetracker.dto.AccountDto;
import com.expensetracker.entity.Account;
import com.expensetracker.entity.User;
import com.expensetracker.service.AccountService;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/accounts")
@CrossOrigin(origins = "*")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @GetMapping
    public ResponseEntity<List<AccountDto>> getAllAccounts(@AuthenticationPrincipal User user) {
        List<AccountDto> dtos = accountService.getAllAccounts(user).stream()
                .map(a -> new AccountDto(a.getId(), a.getName(), a.getDescription(),
                        a.getType() != null ? a.getType().name() : null, a.getBalance(), a.getIsActive()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDto> getAccount(@AuthenticationPrincipal User user, @PathVariable Long id) {
        Account a = accountService.getAccount(user, id);
        return ResponseEntity.ok(new AccountDto(a.getId(), a.getName(), a.getDescription(),
                a.getType() != null ? a.getType().name() : null, a.getBalance(), a.getIsActive()));
    }

    @PostMapping
    public ResponseEntity<AccountDto> createAccount(@AuthenticationPrincipal User user, @Valid @RequestBody Account account) {
        Account a = accountService.createAccount(user, account);
        return ResponseEntity.ok(new AccountDto(a.getId(), a.getName(), a.getDescription(),
                a.getType() != null ? a.getType().name() : null, a.getBalance(), a.getIsActive()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountDto> updateAccount(@AuthenticationPrincipal User user, @PathVariable Long id, @Valid @RequestBody Account account) {
        Account a = accountService.updateAccount(user, id, account);
        return ResponseEntity.ok(new AccountDto(a.getId(), a.getName(), a.getDescription(),
                a.getType() != null ? a.getType().name() : null, a.getBalance(), a.getIsActive()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal User user, @PathVariable Long id) {
        accountService.deleteAccount(user, id);
        return ResponseEntity.ok().build();
    }
}



