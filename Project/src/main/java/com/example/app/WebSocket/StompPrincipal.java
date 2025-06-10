package com.example.app.WebSocket;

import java.security.Principal;

// Custom Principal
public class StompPrincipal implements Principal {
    private String name;

    public StompPrincipal(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
