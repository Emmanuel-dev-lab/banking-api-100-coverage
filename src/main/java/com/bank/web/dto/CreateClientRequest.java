package com.bank.web.dto;

public record CreateClientRequest(String firstName, String lastName, String username, String password) {
}
