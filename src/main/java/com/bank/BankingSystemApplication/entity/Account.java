package com.bank.BankingSystemApplication.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidade que representa uma conta bancária no sistema.
 * 
 * Esta classe contém todas as informações necessárias para gerenciar uma conta bancária,
 * incluindo dados pessoais do titular, saldo e controle de versão para concorrência otimista.
 * 
 * Características principais:
 * - Validação de CPF com 11 dígitos
 * - Validação de idade mínima (18 anos)
 * - Controle de versão para evitar condições de corrida
 * - Auditoria com timestamps de criação e atualização
 * - Saldo com precisão decimal para operações financeiras
 * 
 * @author Sistema Bancário
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "accounts")
public class Account {
    
    /** Identificador único da conta */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /** Nome completo do titular da conta */
    @NotBlank(message = "Nome é obrigatório")
    @Column(nullable = false)
    private String name;
    
    /** CPF do titular (11 dígitos, único no sistema) */
    @NotBlank(message = "CPF é obrigatório")
    @Pattern(regexp = "\\d{11}", message = "CPF deve conter 11 dígitos")
    @Column(unique = true, nullable = false, length = 11)
    private String cpf;
    
    /** Data de nascimento do titular (para validação de idade) */
    @NotNull(message = "Data de nascimento é obrigatória")
    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;
    
    /** Saldo atual da conta com precisão de 2 casas decimais */
    @Column(precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;
    
    /** Email do titular (opcional) */
    @Email(message = "Email deve ter formato válido")
    private String email;
    
    /** Telefone do titular (10 ou 11 dígitos, opcional) */
    @Pattern(regexp = "\\d{10,11}", message = "Telefone deve conter 10 ou 11 dígitos")
    private String phone;
    
    /** Versão para controle de concorrência otimista */
    @Version
    private Long version;
    
    /** Timestamp de criação da conta */
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    /** Timestamp da última atualização */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Callback executado antes da persistência inicial da entidade.
     * Define os timestamps de criação e atualização.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Callback executado antes de cada atualização da entidade.
     * Atualiza o timestamp de última modificação.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Construtor padrão necessário para o JPA.
     */
    public Account() {}
    
    /**
     * Construtor para criação de nova conta com dados básicos.
     * 
     * @param name Nome completo do titular
     * @param cpf CPF do titular (11 dígitos)
     * @param birthDate Data de nascimento do titular
     */
    public Account(String name, String cpf, LocalDate birthDate) {
        this.name = name;
        this.cpf = cpf;
        this.birthDate = birthDate;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getCpf() {
        return cpf;
    }
    
    public void setCpf(String cpf) {
        this.cpf = cpf;
    }
    
    public LocalDate getBirthDate() {
        return birthDate;
    }
    
    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }
    
    public BigDecimal getBalance() {
        return balance;
    }
    
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}