package com.example.juegoacuatico;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.content.res.AssetFileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private GameThread gameThread;
    private Player player;

    private final int STATE_MENU = 0;
    private final int STATE_PLAYING = 1;
    private final int STATE_GAMEOVER = 2;
    private final int STATE_PAUSE = 3;
    private final int STATE_CONTINUE = 4;
    private final int STATE_COUNTDOWN = 5;
    private final int STATE_HIGHSCORE = 6;
    private final int STATE_SETTINGS = 7;

    private int gameState = STATE_MENU;
    private final int CONTINUE_COST = 10;
    private Obstacle hitObstacle = null;

    private Rect continueYesRect, continueNoRect, newGameButtonRect;
    private Rect pauseButtonRect, pauseContinueRect, pauseMenuRect;
    private Rect startButtonRect, recordButtonRect, settingsButtonRect;
    private Rect changeSkinButtonRect, backButtonRect;

    private Paint scorePaint, warningPaint, titlePaint, disabledPaint;
    private Paint disabledTextPaint, pauseButtonPaint, pauseLinePaint;
    private Paint menuBgPaint, buttonPaint;

    private MediaPlayer musicPlayer;
    private SoundPool soundPool;
    private int sndNadar, sndRecolectar, sndMordida;

    private long countdownEndTime = 0;
    float volumenJuego = 0.5f;
    float sliderX, sliderY;
    float sliderAncho = 200;

    private ArrayList<Obstacle> obstacles;
    private ArrayList<Garbage> garbageItems;

    private int screenWidth, screenHeight;
    private Random random;

    private int garbageCount = 0;
    private long distanceScore = 0;
    private long highScore = 0;
    private int spawnTimer = 0;
    private int nextSpeedUpMilestone = 30;

    private Bitmap[] playerBitmaps;
    private int selectedSkinIndex = 0;
    private Bitmap obstacleBitmapScaled, garbageBitmapScaled, menuBgBitmap;
    private Bitmap bgFarBitmap, bgMidBitmap, bgNearBitmap;

    public float bgFarX1, bgFarX2, bgMidX1, bgMidX2, bgNearX1, bgNearX2;
    public float bgMidY, bgNearY;
    public float bgFarSpeed = 1f, bgMidSpeed = 2f, bgNearSpeed = 3f;

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        gameThread = new GameThread(getHolder(), this);
        random = new Random();

        scorePaint = new Paint();
        scorePaint.setColor(Color.WHITE);
        scorePaint.setTextSize(60);
        scorePaint.setTextAlign(Paint.Align.LEFT);

        warningPaint = new Paint();
        warningPaint.setColor(Color.YELLOW);
        warningPaint.setTextSize(60);
        warningPaint.setTextAlign(Paint.Align.CENTER);
        warningPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        titlePaint = new Paint();
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextSize(100);
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setAntiAlias(false);

        disabledPaint = new Paint();
        disabledPaint.setColor(Color.GRAY);
        disabledPaint.setStyle(Paint.Style.FILL);

        disabledTextPaint = new Paint();
        disabledTextPaint.setColor(Color.BLACK);
        disabledTextPaint.setTextSize(60);
        disabledTextPaint.setTextAlign(Paint.Align.CENTER);

        pauseButtonPaint = new Paint();
        pauseButtonPaint.setColor(Color.rgb(50, 200, 180));
        pauseButtonPaint.setStyle(Paint.Style.FILL);

        pauseLinePaint = new Paint();
        pauseLinePaint.setColor(Color.WHITE);
        pauseLinePaint.setStrokeWidth(15);

        menuBgPaint = new Paint();
        menuBgPaint.setColor(Color.argb(200, 0, 0, 0));

        buttonPaint = new Paint();
        buttonPaint.setStyle(Paint.Style.FILL);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build();

        try {
            AssetFileDescriptor afd = context.getAssets().openFd("nadar.mp3");
            sndNadar = soundPool.load(afd, 1);
            afd = context.getAssets().openFd("recolectar.mp3");
            sndRecolectar = soundPool.load(afd, 1);
            afd = context.getAssets().openFd("mordida.mp3");
            sndMordida = soundPool.load(afd, 1);

            musicPlayer = new MediaPlayer();
            afd = context.getAssets().openFd("musica_fondo.mp3");
            musicPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            musicPlayer.prepare();
            musicPlayer.setLooping(true);
            musicPlayer.setVolume(1.0f, 1.0f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void resetGame() {
        garbageCount = 0;
        distanceScore = 0;
        player.reset(screenHeight / 2);
        obstacles.clear();
        garbageItems.clear();

        Obstacle.speed = Obstacle.BASE_SPEED;
        Garbage.speed = Garbage.BASE_SPEED;
        nextSpeedUpMilestone = 30;

        if (musicPlayer != null && !musicPlayer.isPlaying()) {
            musicPlayer.start();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        screenWidth = getWidth();
        screenHeight = getHeight();

        int[] skinResources = {
                R.drawable.player_diver,
                R.drawable.player_diver2,
                R.drawable.player_diver3
        };
        playerBitmaps = new Bitmap[skinResources.length];

        for (int i = 0; i < skinResources.length; i++) {
            Bitmap original = BitmapFactory.decodeResource(getResources(), skinResources[i]);
            int playerNuevoAncho = 200;
            if (i == 1) playerNuevoAncho = 300;

            float aspectRatio = (float) original.getHeight() / original.getWidth();
            int playerNuevoAlto = (int) (playerNuevoAncho * aspectRatio);
            playerBitmaps[i] = Bitmap.createScaledBitmap(original, playerNuevoAncho, playerNuevoAlto, true);
        }

        Bitmap originalMenuBg = BitmapFactory.decodeResource(getResources(), R.drawable.menu_bg);
        menuBgBitmap = Bitmap.createScaledBitmap(originalMenuBg, screenWidth, screenHeight, false);

        Bitmap originalBgFar = BitmapFactory.decodeResource(getResources(), R.drawable.background_far);
        int farTargetHeight = screenHeight;
        float farRatio = (float) originalBgFar.getWidth() / originalBgFar.getHeight();
        int farAncho = (int) (farTargetHeight * farRatio);
        if (farAncho < screenWidth) farAncho = screenWidth + 50;
        bgFarBitmap = Bitmap.createScaledBitmap(originalBgFar, farAncho, farTargetHeight, false);
        bgFarX1 = 0;
        bgFarX2 = bgFarBitmap.getWidth() - 2;

        Bitmap originalBgMid = BitmapFactory.decodeResource(getResources(), R.drawable.background_mid);
        int midTargetHeight = (int)(screenHeight * 0.6f);
        float midRatio = (float) originalBgMid.getWidth() / originalBgMid.getHeight();
        int midAncho = (int) (midTargetHeight * midRatio);
        if (midAncho < screenWidth) midAncho = screenWidth + 50;
        bgMidBitmap = Bitmap.createScaledBitmap(originalBgMid, midAncho, midTargetHeight, false);
        bgMidX1 = 0;
        bgMidX2 = bgMidBitmap.getWidth() - 2;
        bgMidY = screenHeight - bgMidBitmap.getHeight();

        Bitmap originalBgNear = BitmapFactory.decodeResource(getResources(), R.drawable.background_near);
        int nearTargetHeight = (int)(screenHeight * 0.5f);
        float nearRatio = (float) originalBgNear.getWidth() / originalBgNear.getHeight();
        int nearAncho = (int) (nearTargetHeight * nearRatio);
        if (nearAncho < screenWidth) nearAncho = screenWidth + 50;
        bgNearBitmap = Bitmap.createScaledBitmap(originalBgNear, nearAncho, nearTargetHeight, false);
        bgNearX1 = 0;
        bgNearX2 = bgNearBitmap.getWidth() - 2;
        bgNearY = screenHeight - bgNearBitmap.getHeight();

        Bitmap originalObstacleBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.shark_1);
        int obstacleNuevoAncho = 350;
        float obstacleAspectRatio = (float) originalObstacleBitmap.getHeight() / originalObstacleBitmap.getWidth();
        int obstacleNuevoAlto = (int) (obstacleNuevoAncho * obstacleAspectRatio);
        obstacleBitmapScaled = Bitmap.createScaledBitmap(originalObstacleBitmap, obstacleNuevoAncho, obstacleNuevoAlto, true);

        Bitmap originalGarbageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.garbage);
        int garbageNuevoAncho = 150;
        float garbageAspectRatio = (float) originalGarbageBitmap.getHeight() / originalGarbageBitmap.getWidth();
        int garbageNuevoAlto = (int) (garbageNuevoAncho * garbageAspectRatio);
        garbageBitmapScaled = Bitmap.createScaledBitmap(originalGarbageBitmap, garbageNuevoAncho, garbageNuevoAlto, true);

        player = new Player(playerBitmaps[selectedSkinIndex], 100, screenHeight / 2);
        obstacles = new ArrayList<>();
        garbageItems = new ArrayList<>();

        int buttonWidth = 350;
        int buttonHeight = 100;
        int yesButtonX = (screenWidth / 2) - buttonWidth - 20;
        int noButtonX = (screenWidth / 2) + 20;
        int buttonY = (screenHeight / 2) + 50;

        continueYesRect = new Rect(yesButtonX, buttonY, yesButtonX + buttonWidth, buttonY + buttonHeight);
        continueNoRect = new Rect(noButtonX, buttonY, noButtonX + buttonWidth, buttonY + buttonHeight);

        int newGameButtonWidth = 700;
        int newGameButtonHeight = 120;
        int newGameButtonX = (screenWidth / 2) - (newGameButtonWidth / 2);
        int newGameButtonY = (screenHeight / 2);
        newGameButtonRect = new Rect(newGameButtonX, newGameButtonY, newGameButtonX + newGameButtonWidth, newGameButtonY + newGameButtonHeight);

        int pauseSize = 100;
        int pauseX = (screenWidth / 2) - (pauseSize / 2);
        int pauseY = 20;
        pauseButtonRect = new Rect(pauseX, pauseY, pauseX + pauseSize, pauseY + pauseSize);

        int pauseMenuButtonWidth = 400;
        int pauseMenuButtonHeight = 100;
        int pauseMenuButtonX = (screenWidth / 2) - (pauseMenuButtonWidth / 2);

        pauseContinueRect = new Rect(pauseMenuButtonX, (screenHeight / 2) - 120,
                pauseMenuButtonX + pauseMenuButtonWidth, (screenHeight / 2) - 20);
        pauseMenuRect = new Rect(pauseMenuButtonX, (screenHeight / 2) + 20,
                pauseMenuButtonX + pauseMenuButtonWidth, (screenHeight / 2) + 120);

        int menuButtonWidth = 400;
        int menuButtonHeight = 100;
        int menuButtonX = (screenWidth / 2) - (menuButtonWidth / 2);

        startButtonRect = new Rect(menuButtonX, (screenHeight / 2) - 70, menuButtonX + menuButtonWidth, (screenHeight / 2) + 30);
        recordButtonRect = new Rect(menuButtonX, (screenHeight / 2) + 60, menuButtonX + menuButtonWidth, (screenHeight / 2) + 160);

        int skinBtnY = recordButtonRect.bottom + 40;
        changeSkinButtonRect = new Rect(menuButtonX, skinBtnY, menuButtonX + menuButtonWidth, skinBtnY + 100);
        backButtonRect = new Rect(20, screenHeight - 120, 220, screenHeight - 20);

        sliderX = (screenWidth / 2) - (sliderAncho / 2);
        sliderY = (screenHeight / 2);

        gameThread.setRunning(true);
        gameThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        while (retry) {
            try {
                gameThread.setRunning(false);
                gameThread.join();
                retry = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void update() {
        if (gameState == STATE_COUNTDOWN) {
            if (System.currentTimeMillis() >= countdownEndTime) {
                gameState = STATE_PLAYING;
            }
            return;
        }
        if (gameState != STATE_PLAYING) return;

        float overlap = 10f;

        bgFarX1 -= bgFarSpeed;
        bgFarX2 -= bgFarSpeed;
        if (bgFarX1 + bgFarBitmap.getWidth() < 0) {
            bgFarX1 = bgFarX2 + bgFarBitmap.getWidth() - overlap;
        }
        if (bgFarX2 + bgFarBitmap.getWidth() < 0) {
            bgFarX2 = bgFarX1 + bgFarBitmap.getWidth() - overlap;
        }

        bgMidX1 -= bgMidSpeed;
        bgMidX2 -= bgMidSpeed;
        if (bgMidX1 + bgMidBitmap.getWidth() < 0) {
            bgMidX1 = bgMidX2 + bgMidBitmap.getWidth() - overlap;
        }
        if (bgMidX2 + bgMidBitmap.getWidth() < 0) {
            bgMidX2 = bgMidX1 + bgMidBitmap.getWidth() - overlap;
        }

        bgNearX1 -= bgNearSpeed;
        bgNearX2 -= bgNearSpeed;
        if (bgNearX1 + bgNearBitmap.getWidth() < 0) {
            bgNearX1 = bgNearX2 + bgNearBitmap.getWidth() - overlap;
        }
        if (bgNearX2 + bgNearBitmap.getWidth() < 0) {
            bgNearX2 = bgNearX1 + bgNearBitmap.getWidth() - overlap;
        }

        distanceScore++;
        long displayDistance = distanceScore / 10;
        if (displayDistance > nextSpeedUpMilestone) {
            Obstacle.speed++;
            Garbage.speed++;
            nextSpeedUpMilestone += 15;
        }

        player.update(screenHeight);
        spawnTimer++;

        if (spawnTimer > 50) {
            int minY = 0;
            int maxY = screenHeight - obstacleBitmapScaled.getHeight();
            if (maxY < 0) maxY = 0;

            int yPos;
            if (minY == maxY) yPos = minY;
            else yPos = random.nextInt(maxY - minY + 1) + minY;

            if (random.nextFloat() < 0.7) {
                garbageItems.add(new Garbage(garbageBitmapScaled, screenWidth + 200, yPos));
            } else {
                obstacles.add(new Obstacle(obstacleBitmapScaled, screenWidth + 200, yPos));
            }
            spawnTimer = 0;
        }

        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Obstacle obs = obstacles.get(i);
            obs.update();
            if (obs.isOffscreen()) obstacles.remove(i);

            if (Rect.intersects(player.getCollisionRect(), obs.getCollisionRect())) {
                hitObstacle = obs;
                gameState = STATE_CONTINUE;
                if (soundPool != null) soundPool.play(sndMordida, 1, 1, 0, 0, 1);
                return;
            }
        }

        for (int i = garbageItems.size() - 1; i >= 0; i--) {
            Garbage item = garbageItems.get(i);
            item.update();
            if (item.isOffscreen()) garbageItems.remove(i);

            if (Rect.intersects(player.getCollisionRect(), item.getCollisionRect())) {
                garbageCount++;
                if (soundPool != null) soundPool.play(sndRecolectar, 1, 1, 0, 0, 1);
                garbageItems.remove(i);
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas == null) return;

        if (gameState == STATE_MENU) {
            if (menuBgBitmap != null) canvas.drawBitmap(menuBgBitmap, 0, 0, null);
            else canvas.drawColor(Color.BLACK);
            drawMenu(canvas);
            return;
        }

        canvas.drawBitmap(bgFarBitmap, bgFarX1, 0, null);
        canvas.drawBitmap(bgFarBitmap, bgFarX2, 0, null);
        canvas.drawBitmap(bgMidBitmap, bgMidX1, bgMidY, null);
        canvas.drawBitmap(bgMidBitmap, bgMidX2, bgMidY, null);
        canvas.drawBitmap(bgNearBitmap, bgNearX1, bgNearY, null);
        canvas.drawBitmap(bgNearBitmap, bgNearX2, bgNearY, null);

        player.draw(canvas);
        for (Obstacle obs : obstacles) obs.draw(canvas);
        for (Garbage item : garbageItems) item.draw(canvas);

        if (gameState == STATE_PLAYING) {
            scorePaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("Basura: " + garbageCount, 50, 80, scorePaint);
            long displayDistance = distanceScore / 10;
            scorePaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("Distancia: " + displayDistance + " M", screenWidth - 50, 80, scorePaint);
            scorePaint.setTextAlign(Paint.Align.LEFT);
            drawPauseButton(canvas);
        } else if (gameState == STATE_GAMEOVER) {
            drawGameOverMenu(canvas);
        } else if (gameState == STATE_CONTINUE) {
            drawContinueMenu(canvas);
        } else if (gameState == STATE_PAUSE) {
            drawPauseMenu(canvas);
        } else if (gameState == STATE_COUNTDOWN) {
            drawCountdown(canvas);
        } else if (gameState == STATE_HIGHSCORE) {
            drawHighscoreScreen(canvas);
        } else if (gameState == STATE_SETTINGS) {
            drawSettingsScreen(canvas);
        }
    }

    private void drawMenu(Canvas canvas) {
        float titleY = (screenHeight / 2) - 350;
        if (titleY < 100) titleY = 100;

        titlePaint.setColor(Color.BLACK);
        titlePaint.setStyle(Paint.Style.STROKE);
        titlePaint.setStrokeWidth(12);
        canvas.drawText("ECO RUSH", screenWidth / 2, titleY, titlePaint);
        titlePaint.setColor(Color.WHITE);
        titlePaint.setStyle(Paint.Style.FILL);
        titlePaint.setStrokeWidth(0);
        canvas.drawText("ECO RUSH", screenWidth / 2, titleY, titlePaint);

        Bitmap currentSkin = playerBitmaps[selectedSkinIndex];
        float previewX = screenWidth / 2 - currentSkin.getWidth() / 2;
        float previewY = startButtonRect.top - currentSkin.getHeight() - 20;
        canvas.drawBitmap(currentSkin, previewX, previewY, null);

        scorePaint.setTextAlign(Paint.Align.CENTER);
        scorePaint.setTextSize(50);

        buttonPaint.setColor(Color.rgb(0, 200, 170));
        canvas.drawRect(startButtonRect, buttonPaint);
        scorePaint.setColor(Color.WHITE);
        float textYStart = startButtonRect.centerY() - ((scorePaint.descent() + scorePaint.ascent()) / 2);
        canvas.drawText("Iniciar Juego", startButtonRect.centerX(), textYStart, scorePaint);

        buttonPaint.setColor(Color.rgb(255, 127, 80));
        canvas.drawRect(recordButtonRect, buttonPaint);
        scorePaint.setColor(Color.WHITE);
        float textYRecord = recordButtonRect.centerY() - ((scorePaint.descent() + scorePaint.ascent()) / 2);
        canvas.drawText("Récord", recordButtonRect.centerX(), textYRecord, scorePaint);

        buttonPaint.setColor(Color.rgb(150, 50, 255));
        canvas.drawRect(changeSkinButtonRect, buttonPaint);
        scorePaint.setColor(Color.WHITE);
        float textYSkin = changeSkinButtonRect.centerY() - ((scorePaint.descent() + scorePaint.ascent()) / 2);
        canvas.drawText("Cambiar Buzo", changeSkinButtonRect.centerX(), textYSkin, scorePaint);

        scorePaint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawHighscoreScreen(Canvas canvas) {
        canvas.drawRect(0, 0, screenWidth, screenHeight, menuBgPaint);
        scorePaint.setTextAlign(Paint.Align.CENTER);
        scorePaint.setColor(Color.WHITE);
        scorePaint.setTextSize(80);
        canvas.drawText("Mejor Puntuación", screenWidth / 2, screenHeight / 2 - 100, scorePaint);
        scorePaint.setColor(Color.YELLOW);
        scorePaint.setTextSize(120);
        long displayHighScore = highScore / 10;
        canvas.drawText(displayHighScore + " M", screenWidth / 2, screenHeight / 2 + 50, scorePaint);

        buttonPaint.setColor(Color.GRAY);
        canvas.drawRect(backButtonRect, buttonPaint);
        scorePaint.setColor(Color.WHITE);
        scorePaint.setTextSize(60);
        canvas.drawText("Atrás", backButtonRect.centerX(), backButtonRect.centerY() + 20, scorePaint);
        scorePaint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawSettingsScreen(Canvas canvas) {
        canvas.drawRect(0, 0, screenWidth, screenHeight, menuBgPaint);
        scorePaint.setTextAlign(Paint.Align.CENTER);
        scorePaint.setColor(Color.WHITE);
        scorePaint.setTextSize(80);
        canvas.drawText("Configuración", screenWidth / 2, screenHeight / 2 - 200, scorePaint);

        scorePaint.setTextSize(40);
        canvas.drawText("VOLUMEN", sliderX + sliderAncho / 2, sliderY - 50, scorePaint);

        buttonPaint.setColor(Color.GRAY);
        canvas.drawRect(sliderX, sliderY, sliderX + sliderAncho, sliderY + 20, buttonPaint);

        float handleX = map(volumenJuego, 0.0f, 1.0f, sliderX, sliderX + sliderAncho);
        buttonPaint.setColor(Color.WHITE);
        canvas.drawCircle(handleX, sliderY + 10, 30, buttonPaint);

        buttonPaint.setColor(Color.GRAY);
        canvas.drawRect(backButtonRect, buttonPaint);
        scorePaint.setColor(Color.WHITE);
        scorePaint.setTextSize(60);
        canvas.drawText("Atrás", backButtonRect.centerX(), backButtonRect.centerY() + 20, scorePaint);
        scorePaint.setTextAlign(Paint.Align.LEFT);
    }

    private float map(float value, float start1, float stop1, float start2, float stop2) {
        return start2 + (stop2 - start2) * ((value - start1) / (stop1 - start1));
    }

    private void drawContinueMenu(Canvas canvas) {
        canvas.drawColor(Color.argb(200, 0, 0, 0));
        scorePaint.setTextAlign(Paint.Align.CENTER);
        scorePaint.setColor(Color.WHITE);
        scorePaint.setTextSize(60);

        scorePaint.setTextSize(100);
        scorePaint.setShadowLayer(5.0f, 3.0f, 3.0f, Color.BLACK);
        canvas.drawText("¡UPS!", screenWidth / 2, screenHeight / 2 - 200, scorePaint);
        scorePaint.setShadowLayer(0f, 0f, 0f, Color.BLACK);

        boolean canContinue = (garbageCount >= CONTINUE_COST);

        if (canContinue) {
            scorePaint.setTextSize(60);
            canvas.drawText("¿Quieres gastar " + CONTINUE_COST + " de basura", screenWidth / 2, screenHeight / 2 - 100, scorePaint);
            canvas.drawText("para continuar?", screenWidth / 2, screenHeight / 2 - 40, scorePaint);

            buttonPaint.setColor(Color.rgb(0, 200, 170));
            canvas.drawRect(continueYesRect, buttonPaint);
            scorePaint.setColor(Color.WHITE);
            float textYYes = continueYesRect.centerY() - ((scorePaint.descent() + scorePaint.ascent()) / 2);
            canvas.drawText("Sí (Usar " + CONTINUE_COST + ")", continueYesRect.centerX(), textYYes, scorePaint);

            buttonPaint.setColor(Color.rgb(235, 90, 90));
            canvas.drawRect(continueNoRect, buttonPaint);
            scorePaint.setColor(Color.WHITE);
            float textYNo = continueNoRect.centerY() - ((scorePaint.descent() + scorePaint.ascent()) / 2);
            canvas.drawText("No", continueNoRect.centerX(), textYNo, scorePaint);
        } else {
            scorePaint.setTextSize(60);
            canvas.drawText("No recogiste suficiente basura", screenWidth / 2, screenHeight / 2 - 100, scorePaint);

            buttonPaint.setColor(Color.rgb(0, 200, 170));
            canvas.drawRect(newGameButtonRect, buttonPaint);
            scorePaint.setColor(Color.WHITE);
            float textYPlayAgain = newGameButtonRect.centerY() - ((scorePaint.descent() + scorePaint.ascent()) / 2);
            canvas.drawText("Jugar de Nuevo", newGameButtonRect.centerX(), textYPlayAgain, scorePaint);
        }
        scorePaint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawPauseButton(Canvas canvas) {
        canvas.drawCircle(pauseButtonRect.centerX(), pauseButtonRect.centerY(), pauseButtonRect.width() / 2f, pauseButtonPaint);
        int barHeight = 40;
        int barWidth = 12;
        int separation = 12;
        float top = pauseButtonRect.centerY() - (barHeight / 2);
        float bottom = pauseButtonRect.centerY() + (barHeight / 2);
        float left_bar_left = pauseButtonRect.centerX() - separation - barWidth;
        float left_bar_right = pauseButtonRect.centerX() - separation;
        canvas.drawRect(left_bar_left, top, left_bar_right, bottom, pauseLinePaint);
        float right_bar_left = pauseButtonRect.centerX() + separation;
        float right_bar_right = pauseButtonRect.centerX() + separation + barWidth;
        canvas.drawRect(right_bar_left, top, right_bar_right, bottom, pauseLinePaint);
    }

    private void drawPauseMenu(Canvas canvas) {
        canvas.drawColor(Color.argb(200, 0, 0, 0));
        scorePaint.setTextAlign(Paint.Align.CENTER);
        scorePaint.setColor(Color.WHITE);
        scorePaint.setTextSize(100);
        scorePaint.setShadowLayer(5.0f, 3.0f, 3.0f, Color.BLACK);
        canvas.drawText("PAUSA", screenWidth / 2, screenHeight / 2 - 180, scorePaint);
        scorePaint.setShadowLayer(0f, 0f, 0f, Color.BLACK);

        buttonPaint.setColor(Color.rgb(0, 200, 170));
        canvas.drawRect(pauseContinueRect, buttonPaint);
        scorePaint.setColor(Color.WHITE);
        scorePaint.setTextSize(60);
        float textYCont = pauseContinueRect.centerY() - ((scorePaint.descent() + scorePaint.ascent()) / 2);
        canvas.drawText("Continuar", pauseContinueRect.centerX(), textYCont, scorePaint);

        buttonPaint.setColor(Color.rgb(235, 90, 90));
        canvas.drawRect(pauseMenuRect, buttonPaint);
        scorePaint.setColor(Color.WHITE);
        float textYExit = pauseMenuRect.centerY() - ((scorePaint.descent() + scorePaint.ascent()) / 2);
        canvas.drawText("Salir al Menú", pauseMenuRect.centerX(), textYExit, scorePaint);
        scorePaint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawCountdown(Canvas canvas) {
        canvas.drawRect(0, 0, screenWidth, screenHeight, menuBgPaint);
        long timeLeft = (countdownEndTime - System.currentTimeMillis()) / 1000;
        int numberToShow = (int) timeLeft + 1;
        scorePaint.setTextSize(300);
        scorePaint.setTextAlign(Paint.Align.CENTER);
        scorePaint.setColor(Color.WHITE);
        canvas.drawText(String.valueOf(numberToShow), screenWidth / 2, screenHeight / 2 + 100, scorePaint);
        scorePaint.setTextSize(60);
        scorePaint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawGameOverMenu(Canvas canvas) {
        canvas.drawRect(0, 0, screenWidth, screenHeight, menuBgPaint);
        scorePaint.setTextAlign(Paint.Align.CENTER);
        scorePaint.setColor(Color.WHITE);
        canvas.drawText("¡JUEGO TERMINADO!", screenWidth / 2, screenHeight / 2 - 100, scorePaint);
        buttonPaint.setColor(Color.rgb(0, 150, 255));
        canvas.drawRect(newGameButtonRect, buttonPaint);
        scorePaint.setColor(Color.WHITE);
        canvas.drawText("Juego Nuevo", newGameButtonRect.centerX(), newGameButtonRect.centerY() + 20, scorePaint);
        scorePaint.setTextAlign(Paint.Align.LEFT);
    }

    private void actualizarVolumenConMouse(int touchX) {
        volumenJuego = map(touchX, sliderX, sliderX + sliderAncho, 0.0f, 1.0f);
        volumenJuego = Math.max(0.0f, Math.min(volumenJuego, 1.0f));
        if (musicPlayer != null) musicPlayer.setVolume(volumenJuego, volumenJuego);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        int touchX = (int) event.getX();
        int touchY = (int) event.getY();

        if (gameState == STATE_SETTINGS) {
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                if (touchX >= sliderX && touchX <= sliderX + sliderAncho &&
                        touchY >= sliderY - 20 && touchY <= sliderY + 40) {
                    actualizarVolumenConMouse(touchX);
                }
            }
        }

        if (action == MotionEvent.ACTION_DOWN) {
            switch (gameState) {
                case STATE_PLAYING:
                    if (pauseButtonRect.contains(touchX, touchY)) {
                        gameState = STATE_PAUSE;
                    } else {
                        player.dive(true);
                        if (soundPool != null) soundPool.play(sndNadar, 1, 1, 0, 0, 1);
                    }
                    break;

                case STATE_MENU:
                    if (startButtonRect.contains(touchX, touchY)) {
                        resetGame();
                        gameState = STATE_PLAYING;
                    } else if (recordButtonRect.contains(touchX, touchY)) {
                        gameState = STATE_HIGHSCORE;
                    } else if (changeSkinButtonRect.contains(touchX, touchY)) {
                        selectedSkinIndex++;
                        if (selectedSkinIndex >= playerBitmaps.length) selectedSkinIndex = 0;
                        player = new Player(playerBitmaps[selectedSkinIndex], 100, screenHeight / 2);
                        if (soundPool != null) soundPool.play(sndRecolectar, 1, 1, 0, 0, 1);
                    }
                    break;

                case STATE_GAMEOVER:
                    if (newGameButtonRect.contains(touchX, touchY)) {
                        if (distanceScore > highScore) highScore = distanceScore;
                        gameState = STATE_MENU;
                    }
                    break;

                case STATE_CONTINUE:
                    boolean canContinue = (garbageCount >= CONTINUE_COST);
                    if (canContinue) {
                        if (continueYesRect.contains(touchX, touchY)) {
                            garbageCount -= CONTINUE_COST;
                            player.reset(screenHeight / 2);
                            if (hitObstacle != null) {
                                obstacles.remove(hitObstacle);
                                hitObstacle = null;
                            }
                            gameState = STATE_PLAYING;
                        } else if (continueNoRect.contains(touchX, touchY)) {
                            hitObstacle = null;
                            if (distanceScore > highScore) highScore = distanceScore;
                            gameState = STATE_GAMEOVER;
                        }
                    } else {
                        if (newGameButtonRect.contains(touchX, touchY)) {
                            hitObstacle = null;
                            if (distanceScore > highScore) highScore = distanceScore;
                            resetGame();
                            gameState = STATE_PLAYING;
                        }
                    }
                    break;

                case STATE_PAUSE:
                    if (pauseContinueRect.contains(touchX, touchY)) {
                        gameState = STATE_COUNTDOWN;
                        countdownEndTime = System.currentTimeMillis() + 3000;
                    } else if (pauseMenuRect.contains(touchX, touchY)) {
                        if (distanceScore > highScore) highScore = distanceScore;
                        gameState = STATE_MENU;
                    }
                    break;

                case STATE_HIGHSCORE:
                    if (backButtonRect.contains(touchX, touchY)) gameState = STATE_MENU;
                    break;

                case STATE_SETTINGS:
                    if (backButtonRect.contains(touchX, touchY)) gameState = STATE_MENU;
                    break;
            }
        } else if (action == MotionEvent.ACTION_UP) {
            if (gameState == STATE_PLAYING) player.dive(false);
        }
        return true;
    }

    public void releaseSounds() {
        if (musicPlayer != null) {
            musicPlayer.stop();
            musicPlayer.release();
            musicPlayer = null;
        }
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    public void pauseMusic() {
        if (musicPlayer != null && musicPlayer.isPlaying()) musicPlayer.pause();
    }

    public void resumeMusic() {
        if (musicPlayer != null && !musicPlayer.isPlaying()) musicPlayer.start();
    }
}