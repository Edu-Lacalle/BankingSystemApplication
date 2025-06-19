package com.bank.BankingSystemApplication.dto;

public class TransactionResponse {

    private Status status;
    private String message;
    
    public TransactionResponse() {}
    
    public TransactionResponse(Status status, String message) {
        this.status = status;
        this.message = message;
    }
    
    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}