package com.example.azimbalife.ViewModel;



public class MessageModel {
    private String message;
    private boolean isUser;

    public MessageModel(String message, boolean isUser) {
        this.message = message;
        this.isUser = isUser;
    }

    public String getMessage() {
        return message;
    }

    public boolean isUser() {
        return isUser;
    }
}
