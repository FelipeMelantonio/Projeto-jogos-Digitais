package com.felipemelantonio.motorunneriot.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import com.felipemelantonio.motorunneriot.MotoRunnerGame;
import com.felipemelantonio.motorunneriot.entities.Moto;
import com.felipemelantonio.motorunneriot.entities.Carro;
import com.felipemelantonio.motorunneriot.entities.Background;
import com.felipemelantonio.motorunneriot.utils.LevelManager;

public class GameScreen implements Screen {
    private final MotoRunnerGame game;
    private SpriteBatch batch;
    private Moto moto;
    private Array<Carro> carros;
    private LevelManager levelManager;
    private Background background;
    private BitmapFont font;
    private float distanciaPercorrida;
    private long lastSpawnTime;

    private static final float SPAWN_INTERVAL = 1.5f;

    public GameScreen(MotoRunnerGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        moto = new Moto();
        carros = new Array<>();
        levelManager = new LevelManager();
        background = new Background();
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        distanciaPercorrida = 0;
        lastSpawnTime = TimeUtils.nanoTime();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        background.update(delta);
        moto.update(delta);

        if (TimeUtils.timeSinceNanos(lastSpawnTime) > (long) (SPAWN_INTERVAL * 1_000_000_000L)) {
            spawnCar();
            lastSpawnTime = TimeUtils.nanoTime();
        }

        for (int i = carros.size - 1; i >= 0; i--) {
            Carro c = carros.get(i);
            c.update(delta);
            if (c.getBounds().y + c.getBounds().height < 0) {
                c.dispose();
                carros.removeIndex(i);
            }
        }

        distanciaPercorrida += moto.getVelocidade() * delta;
        levelManager.update(delta, carros);

        for (Carro c : carros) {
            if (moto.getBounds().overlaps(c.getBounds())) {
                game.setScreen(new GameOverScreen(game, distanciaPercorrida));
                dispose();
                return;
            }
        }

        batch.begin();
        background.draw(batch, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        for (Carro c : carros) c.draw(batch);
        moto.draw(batch);

        font.draw(batch, "Velocidade: " + (int) moto.getVelocidade() + " km/h", 10, Gdx.graphics.getHeight() - 10);
        font.draw(batch, "Distância: " + (int) distanciaPercorrida + " m", 10, Gdx.graphics.getHeight() - 30);
        font.draw(batch, "Nível: " + levelManager.getNivelAtual(), 10, Gdx.graphics.getHeight() - 50);
        batch.end();
    }

    private void spawnCar() {
        int nivel = levelManager.getNivelAtual();
        Carro novo = Carro.spawnSafe(carros, nivel);
        if (novo != null) carros.add(novo);
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        moto.dispose();
        background.dispose();
        for (Carro c : carros) c.dispose();
    }
}
