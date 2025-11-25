package com.example.juegoacuatico;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

    private GameView gameView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Crea una instancia de tu vista de juego
        gameView = new GameView(this);

        // Haz que la vista de juego ocupe toda la pantalla
        setContentView(gameView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.resumeMusic();
    }
    @Override
    protected void onPause() {
        super.onPause();
        gameView.pauseMusic(); // Pausa la música al salir
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cuando la app se cierra totalmente, aseguramos liberar memoria
        gameView.releaseSounds();
    }
}


