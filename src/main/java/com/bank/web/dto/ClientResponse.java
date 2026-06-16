package com.bank.web.dto;

import com.bank.domain.model.Client;

public record ClientResponse(String id, String firstName, String lastName) {
    public static ClientResponse from(Client client) {
        return new ClientResponse(client.id(), client.firstName(), client.lastName());
    }
}
