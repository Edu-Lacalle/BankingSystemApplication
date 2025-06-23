package com.bank.BankingSystemApplication.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Resposta padrão para erros da API")
public class ErrorResponse {
    
    @Schema(description = "Código do erro", example = "ACCOUNT_NOT_FOUND")
    private String error;
    
    @Schema(description = "Mensagem descritiva do erro", example = "Conta não encontrada para o ID fornecido")
    private String message;
    
    @Schema(description = "Timestamp do erro")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;
    
    @Schema(description = "Caminho da requisição que gerou o erro", example = "/api/gateway/accounts/123")
    private String path;
    
    @Schema(description = "ID único da requisição para rastreamento", example = "req_abc123def456")
    private String requestId;
    
    @Schema(description = "Detalhes adicionais do erro")
    private Map<String, String> details;

    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(String error, String message) {
        this();
        this.error = error;
        this.message = message;
    }

    public ErrorResponse(String error, String message, String path, String requestId) {
        this(error, message);
        this.path = path;
        this.requestId = requestId;
    }

    public ErrorResponse(String error, String message, String path, String requestId, Map<String, String> details) {
        this(error, message, path, requestId);
        this.details = details;
    }

    // Getters and Setters
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }
}