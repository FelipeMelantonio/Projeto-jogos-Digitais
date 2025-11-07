package com.felipemelantonio.motorunneriot.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.felipemelantonio.motorunneriot.MotoRunnerGame;

public class LevelCompleteScreen implements Screen {

    private final MotoRunnerGame game;
    private final int fase;
    private final int distanciaFinal;
    private final int moedas;

    private SpriteBatch batch;
    private BitmapFont fontBig, fontSmall;

    public LevelCompleteScreen(MotoRunnerGame game, int fase, int distanciaFinal, int moedas) {
        this.game = game;
        this.fase = fase;
        this.distanciaFinal = distanciaFinal;
        this.moedas = moedas;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        fontBig = new BitmapFont();
        fontSmall = new BitmapFont();
        fontBig.setColor(Color.GOLD);
        fontSmall.setColor(Color.WHITE);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        fontBig.getData().setScale(2.0f);
        fontBig.draw(batch, "Fase " + fase + " concluída!", 260, 420);

        fontSmall.getData().setScale(1.2f);
        fontSmall.draw(batch, "Distância: " + distanciaFinal + " m", 320, 370);
        fontSmall.draw(batch, "Moedas: " + moedas, 320, 340);

        fontSmall.getData().setScale(1.1f);
        fontSmall.setColor(Color.LIGHT_GRAY);
        fontSmall.draw(batch, "ENTER / ESPAÇO  —  Voltar ao Menu", 220, 270);
        fontSmall.draw(batch, "1 / 2 / 3       —  Ir direto para outra fase", 220, 240);
        fontSmall.draw(batch, "R               —  Repetir a fase", 220, 210);
        fontSmall.setColor(Color.WHITE);

        batch.end();

        // Navegação
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
            Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            game.setScreen(new MenuScreen(game));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            game.setScreen(new GameScreen(game, fase));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            game.setScreen(new GameScreen(game, 1));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            game.setScreen(new GameScreen(game, 2));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            game.setScreen(new GameScreen(game, 3));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MenuScreen(game));
        }
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (fontBig != null) fontBig.dispose();
        if (fontSmall != null) fontSmall.dispose();
    }
}