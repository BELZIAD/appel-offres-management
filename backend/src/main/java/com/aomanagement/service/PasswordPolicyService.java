package com.aomanagement.service;

import com.aomanagement.exception.ConflictException;
import org.springframework.stereotype.Service;

@Service
public class PasswordPolicyService {
    private static final String MESSAGE = "Le mot de passe doit contenir au moins 8 caracteres, une majuscule, une minuscule, un chiffre et un caractere special";

    public void validate(String password) {
        if (password == null || password.length() < 8
                || !password.matches(".*[A-Z].*")
                || !password.matches(".*[a-z].*")
                || !password.matches(".*\\d.*")
                || !password.matches(".*[^A-Za-z0-9].*")) {
            throw new ConflictException(MESSAGE);
        }
    }
}
