package com.example.juegoacuatico; // <<< Reemplaza esto

import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class GameThread extends Thread {

    private SurfaceHolder surfaceHolder;
    private GameView gameView;
    private boolean isRunning;
    private Canvas canvas;

    public GameThread(SurfaceHolder surfaceHolder, GameView gameView) {
        super();
        this.surfaceHolder = surfaceHolder;
        this.gameView = gameView;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    // <<< Este es el bucle "draw()" de Processing
    @Override
    public void run() {
        while (isRunning) {
            canvas = null;
            try {
                // Bloquea el lienzo para que podamos dibujar en él
                canvas = this.surfaceHolder.lockCanvas();
                synchronized(surfaceHolder) {
                    // Llama a la lógica y al dibujo
                    this.gameView.update();
                    this.gameView.draw(canvas);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (canvas != null) {
                    // Libera el lienzo y muestra el dibujo en pantalla
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }

            // Intenta correr a ~60 FPS
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}