package ru.itmo.is1.security;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IssuedToken {
    private final String value;
    private final Instant expiresAt;
}
