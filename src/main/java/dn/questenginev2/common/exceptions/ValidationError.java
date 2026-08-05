package dn.questenginev2.common.exceptions;

public record ValidationError(String field, String message) {}
