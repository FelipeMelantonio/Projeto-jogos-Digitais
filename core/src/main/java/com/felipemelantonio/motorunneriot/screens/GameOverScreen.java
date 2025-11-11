package com.felipemelantonio.motorunneriot.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.felipemelantonio.motorunneriot.MotoRunnerGame;

public class GameOverScreen implements Screen {
    private final MotoRunnerGame game;
    private SpriteBatch batch;
    private BitmapFont font;
    private Texture background;
    private float distanciaFinal;
    private int fase;

    public GameOverScreen(MotoRunnerGame game, float distancia, int fase) {
        this.game = game;
        this.distanciaFinal = distancia;
        this.fase = fase;
        batch = new SpriteBatch();
        font = new BitmapFont();
        background = new Texture("gameover-image.jpg"); // imagem de fundo fullscreen
    }

    @Override
    public void render(float delta) {
        // Limpa a tela
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        batch.begin();

        // Fundo fullscreen
        batch.draw(background, 0, 0, screenWidth, screenHeight);

        // Configura fonte
        font.getData().setScale(1.1f);

        // Textos a exibir
        String texto2 = "Distância: " + (int) distanciaFinal + " m";
        String texto3 = "ENTER - Reiniciar";
        String texto4 = "ESC - Menu Principal";

        // Calcula largura de cada texto
        GlyphLayout layout2 = new GlyphLayout(font, texto2);
        GlyphLayout layout3 = new GlyphLayout(font, texto3);
        GlyphLayout layout4 = new GlyphLayout(font, texto4);

        float espaco = 60f; // espaço entre textos
        float totalLargura = layout2.width + layout3.width + layout4.width + (espaco * 2);

        // Ponto inicial para centralizar horizontalmente
        float startX = (screenWidth - totalLargura) / 2f;
        float y = 50f; // distância do rodapé

        // Desenha lado a lado
        float currentX = startX;
        font.draw(batch, layout2, currentX, y);
        currentX += layout2.width + espaco;
        font.draw(batch, layout3, currentX, y);
        currentX += layout3.width + espaco;
        font.draw(batch, layout4, currentX, y);

        batch.end();

        // Controles
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER))
            game.setScreen(new GameScreen(game, fase));
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            game.setScreen(new MenuScreen(game));
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        background.dispose();
    }

    @Override public void show() {}
    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
