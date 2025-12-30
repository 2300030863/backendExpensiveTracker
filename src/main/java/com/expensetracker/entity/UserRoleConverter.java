package com.expensetracker.entity;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class UserRoleConverter implements AttributeConverter<UserRole, String> {

    @Override
    public String convertToDatabaseColumn(UserRole attribute) {
        if (attribute == null) {
            return UserRole.ROLE_USER.name();
        }
        return attribute.name();
    }

    @Override
    public UserRole convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return UserRole.ROLE_USER;
        }
        
        try {
            return UserRole.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            // If the value in DB is invalid, default to ROLE_USER
            return UserRole.ROLE_USER;
        }
    }
}
