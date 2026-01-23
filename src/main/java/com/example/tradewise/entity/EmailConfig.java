package com.example.tradewise.entity;

import java.time.LocalDateTime;

public class EmailConfig {
    private Integer id;
    private String emailAddress;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public EmailConfig() {}

    public EmailConfig(String emailAddress) {
        this.emailAddress = emailAddress;
        this.enabled = true;
    }

    public EmailConfig(String emailAddress, Boolean enabled) {
        this.emailAddress = emailAddress;
        this.enabled = enabled;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
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