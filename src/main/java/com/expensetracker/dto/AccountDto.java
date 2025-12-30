package com.expensetracker.dto;

import java.math.BigDecimal;

public class AccountDto {
    private Long id;
    private String name;
    private String description;
    private String type;
    private BigDecimal balance;
    private Boolean isActive;

    public AccountDto() {}

    public AccountDto(Long id, String name, String description, String type, BigDecimal balance, Boolean isActive) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.balance = balance;
        this.isActive = isActive;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}


