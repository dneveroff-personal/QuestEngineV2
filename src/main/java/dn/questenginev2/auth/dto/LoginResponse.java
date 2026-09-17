package dn.questenginev2.auth.dto;

/** Access + refresh pair (ADR-0015). {@code token} removed in favour of {@code accessToken}. */
public record LoginResponse(String publicName, String accessToken, String refreshToken) {}
