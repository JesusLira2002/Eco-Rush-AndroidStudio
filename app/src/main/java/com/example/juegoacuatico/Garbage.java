package com.example.juegoacuatico;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

public class Garbage {
    private Bitmap bitmap;
    public int x, y;
    public static int BASE_SPEED = 20; // Velocidad inicial
    public static int speed = 20;
    private Rect collisionRect;

    public Garbage(Bitmap bitmap, int x, int y) {
        this.bitmap = bitmap;
        this.x = x;
        this.y = y;
        this.collisionRect = new Rect(x, y, x + bitmap.getWidth(), y + bitmap.getHeight());
    }

    public void update() {
        x -= speed;
        collisionRect.set(x, y, x + bitmap.getWidth(), y + bitmap.getHeight());
    }

    public void draw(Canvas canvas) {
        canvas.drawBitmap(bitmap, x, y, null);
    }

    public Rect getCollisionRect() {
        return collisionRect;
    }

    public boolean isOffscreen() {
        return x < -bitmap.getWidth();
    }
}