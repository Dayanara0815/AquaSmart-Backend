package com.AquaSmart.dto;

public record CurrentUserDto(
        Long id,
        String fullName,
        String email,
        String rol,
        String fotoPerfil
) {
}
