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
    private int fase;

    public GameOverScreen(MotoRunnerGame game, float distancia, int fase) {
        this.game = game;
        this.distanciaFinal = distancia;
        this.fase = fase;
        batch = new SpriteBatch();
        font = new BitmapFont();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();
        font.getData().setScale(1.4f);
        font.draw(batch, "GAME OVER", 340, 400);
        font.getData().setScale(1.1f);
        font.draw(batch, "Distância percorrida: " + (int) distanciaFinal + " m", 300, 360);
        font.draw(batch, "ENTER - Reiniciar Fase", 310, 320);
        font.draw(batch, "ESC - Menu Principal", 330, 290);
        batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER))
            game.setScreen(new GameScreen(game, fase));
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            game.setScreen(new MenuScreen(game));
    }

    @Override public void dispose() { batch.dispose(); font.dispose(); }
    @Override public void show() {}
    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
