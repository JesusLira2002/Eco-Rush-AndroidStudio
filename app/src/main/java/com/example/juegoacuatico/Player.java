package com.example.juegoacuatico;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

public class Player {

    private Bitmap bitmap;
    public int x, y;
    private float velocityY;

    //Física: Flota hacia arriba, bucea hacia abajo
    private final float BUOYANCY = -0.8f; // Fuerza hacia arriba
    private final float DIVE_FORCE = 1.2f;  // Fuerza hacia abajo
    private final float MAX_FALL_SPEED = 15;
    private final float MAX_RISE_SPEED = -10;

    private boolean isDiving = false;

    private Rect collisionRect;

    public Player(Bitmap bitmap, int x, int y) {
        this.bitmap = bitmap;
        this.x = x;
        this.y = y;
        this.velocityY = 0;
        this.collisionRect = new Rect(x, y, x + bitmap.getWidth(), y + bitmap.getHeight());
    }

    public void update(int screenHeight) {
        // Esta es la física del Jetpack, pero invertida
        if (isDiving) {
            velocityY += DIVE_FORCE; // Aplicar fuerza de buceo
        } else {
            velocityY += BUOYANCY; // Aplicar flotabilidad
        }

        // Limitar velocidades
        if (velocityY > MAX_FALL_SPEED) velocityY = MAX_FALL_SPEED;
        if (velocityY < MAX_RISE_SPEED) velocityY = MAX_RISE_SPEED;

        // Mover al jugador
        y += velocityY;

        // No dejar que se salga de la pantalla
        if (y < 0) {
            y = 0;
            velocityY = 0;
        }
        if (y > screenHeight - bitmap.getHeight()) {
            y = screenHeight - bitmap.getHeight();
            velocityY = 0;
        }

        // Actualizar el rectángulo de colisión
        int paddingX = 60;
        int paddingY = 50;

        collisionRect.set(
                x + paddingX,
                y + paddingY,
                x + bitmap.getWidth() - paddingX,
                y + bitmap.getHeight() - paddingY
        );
    }

    public void draw(Canvas canvas) {
        // Calcular el ángulo (multiplicamos por 2.5 para que se note más)
        float rotationAngle = velocityY * 2.5f;

        // Limitar la rotación (máximo 45 grados arriba o abajo)
        if (rotationAngle > 45) rotationAngle = 45;
        if (rotationAngle < -45) rotationAngle = -45;

        // Guardar el estado del canvas (Congelar)
        canvas.save();

        // Rotar sobre el CENTRO del buzo
        // (x + ancho/2, y + alto/2)
        canvas.rotate(rotationAngle, x + (bitmap.getWidth() / 2f), y + (bitmap.getHeight() / 2f));

        // Dibujar la imagen (ahora saldrá rotada)
        canvas.drawBitmap(bitmap, x, y, null);

        // Restaurar el estado del canvas (Descongelar)
        // ¡Muy importante para que no rote todo el juego!
        canvas.restore();
    }

    public void dive(boolean isDiving) {
        this.isDiving = isDiving;
    }

    public Rect getCollisionRect() {
        return collisionRect;
    }

    public void reset(int yPos) {
        this.y = yPos;
        this.velocityY = 0;
    }
}