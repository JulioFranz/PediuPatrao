package com.umc.pediupatrao.entity;

import java.util.Locale;

public enum PerfilUsuario {
    ADMIN,
    GERENTE,
    ATENDENTE;

    public static PerfilUsuario fromRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }

        try {
            return valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
