package com.example.azimbalife.Activity;

public class MessageModel {
    private String message;
    private boolean isUser;
    private long timestamp;

    public MessageModel() {
        // Empty constructor required by Firebase
    }

    public MessageModel(String message, boolean isUser) {
        this.message = message;
        this.isUser = isUser;
        this.timestamp = System.currentTimeMillis();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isUser() {
        return isUser;
    }

    public void setUser(boolean user) {
        this.isUser = user;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
