package com.AquaSmart.dto;

import java.util.List;

public record ChatRequestDto(
        String question,
        String email,
        List<ChatMessageDto> history
) {
}