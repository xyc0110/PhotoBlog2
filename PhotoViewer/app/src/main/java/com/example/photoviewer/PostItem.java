package com.example.photoviewer;

import android.graphics.Bitmap;

public class PostItem {
    public String title;
    public Bitmap image;

    public PostItem(String title, Bitmap image) {
        this.title = title;
        this.image = image;
    }
}
