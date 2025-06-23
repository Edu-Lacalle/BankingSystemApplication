package com.bank.BankingSystemApplication.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Resposta padrão para operações bem-sucedidas da API")
public class SuccessResponse<T> {
    
    @Schema(description = "Dados da resposta")
    private T data;
    
    @Schema(description = "Mensagem de sucesso", example = "Operação realizada com sucesso")
    private String message;
    
    @Schema(description = "Timestamp da resposta")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;
    
    @Schema(description = "ID único da requisição para rastreamento", example = "req_abc123def456")
    private String requestId;
    
    @Schema(description = "Status da operação", example = "SUCCESS")
    private String status;

    public SuccessResponse() {
        this.timestamp = LocalDateTime.now();
        this.status = "SUCCESS";
    }

    public SuccessResponse(T data) {
        this();
        this.data = data;
    }

    public SuccessResponse(T data, String message) {
        this(data);
        this.message = message;
    }

    public SuccessResponse(T data, String message, String requestId) {
        this(data, message);
        this.requestId = requestId;
    }

    // Static factory methods for common responses
    public static <T> SuccessResponse<T> created(T data, String message) {
        return new SuccessResponse<>(data, message);
    }

    public static <T> SuccessResponse<T> ok(T data) {
        return new SuccessResponse<>(data);
    }

    public static <T> SuccessResponse<T> ok(T data, String message) {
        return new SuccessResponse<>(data, message);
    }

    public static SuccessResponse<Void> noContent(String message) {
        return new SuccessResponse<>(null, message);
    }

    // Getters and Setters
    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
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

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}