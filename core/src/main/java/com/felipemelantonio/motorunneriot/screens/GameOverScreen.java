package com.felipemelantonio.motorunneriot.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.felipemelantonio.motorunneriot.MotoRunnerGame;

public class GameOverScreen implements Screen {
    private final MotoRunnerGame game;
    private SpriteBatch batch;
    private BitmapFont font;
    private float distanciaFinal;

    public GameOverScreen(MotoRunnerGame game, float distancia) {
        this.game = game;
        this.distanciaFinal = distancia;
        batch = new SpriteBatch();
        font = new BitmapFont();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();
        font.draw(batch, "Game Over!", 350, 400);
        font.draw(batch, "Distância percorrida: " + (int) distanciaFinal + " m", 300, 360);
        font.draw(batch, "Press ENTER to Restart", 310, 320);
        font.draw(batch, "Press ESC to Exit", 330, 300);
        batch.end();
        if (Gdx.input.isKeyPressed(Input.Keys.ENTER)) {
            game.setScreen(new GameScreen(game));
        }
        if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MenuScreen(game));
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }

    @Override
    public void show() {
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }
}
