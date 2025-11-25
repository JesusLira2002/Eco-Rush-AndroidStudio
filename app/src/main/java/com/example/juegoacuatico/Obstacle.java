package com.example.juegoacuatico;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

public class Obstacle {
    private Bitmap bitmap;
    public int x, y;
    public static int BASE_SPEED = 25;
    public static int speed = 25;
    private Rect collisionRect;

    //VARIABLES DE MOVIMIENTO (NADO)
    private int originalY;      // La altura base donde apareció
    private float swimAngle;    // Ángulo para la función seno
    private float swimSpeed;    // Velocidad del aleteo
    private float swimAmplitude;// Qué tan amplio es el movimiento

    // Constructor (Sin Paint)
    public Obstacle(Bitmap bitmap, int x, int y) {
        this.bitmap = bitmap;
        this.x = x;
        this.y = y;

        //INICIALIZAR MOVIMIENTO
        this.originalY = y;

        // Valores aleatorios para que cada tiburón nade diferente
        this.swimAngle = (float) (Math.random() * Math.PI * 2);
        this.swimSpeed = (float) (Math.random() * 0.1f + 0.1f); // Velocidad entre 0.1 y 0.2
        this.swimAmplitude = (float) (Math.random() * 30 + 20); // Amplitud entre 20 y 50 px

        // Inicializar rectángulo
        this.collisionRect = new Rect(x, y, x + bitmap.getWidth(), y + bitmap.getHeight());
    }

    public void update() {
        //Movimiento hacia la izquierda (Scroll)
        x -= speed;

        //Movimiento vertical (Nado con Onda Senoidal)
        swimAngle += swimSpeed;
        y = originalY + (int) (Math.sin(swimAngle) * swimAmplitude);

        //Actualizar Hitbox (Con los márgenes ajustados para el tiburón)
        int paddingX = 50;
        int paddingY = 60;

        collisionRect.set(
                x + paddingX,
                y + paddingY,
                x + bitmap.getWidth() - paddingX,
                y + bitmap.getHeight() - paddingY
        );
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