package com.example.azimbalife.Domain;

import android.view.View;

public class QuickAction {
    private String title;
    private String description;
    private int iconRes;
    private View.OnClickListener clickListener;

    public QuickAction() {
        // Default constructor
    }

    public QuickAction(String title, String description, int iconRes, View.OnClickListener clickListener) {
        this.title = title;
        this.description = description;
        this.iconRes = iconRes;
        this.clickListener = clickListener;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getIconRes() {
        return iconRes;
    }

    public void setIconRes(int iconRes) {
        this.iconRes = iconRes;
    }

    public View.OnClickListener getClickListener() {
        return clickListener;
    }

    public void setClickListener(View.OnClickListener clickListener) {
        this.clickListener = clickListener;
    }
}