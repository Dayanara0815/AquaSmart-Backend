package com.AquaSmart.dto;

public record MedidorDto(
        String id,
        String address,
        String flow,
        String pressure,
        String valve,
        String status) {
}
