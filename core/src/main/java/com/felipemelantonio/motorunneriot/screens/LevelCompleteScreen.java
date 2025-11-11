package com.felipemelantonio.motorunneriot.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.felipemelantonio.motorunneriot.MotoRunnerGame;

public class LevelCompleteScreen implements Screen {

    private final MotoRunnerGame game;
    private final int fase;
    private final int distanciaFinal;
    private final int moedas;

    private SpriteBatch batch;
    private BitmapFont fontBig, fontSmall;
    private Texture background;
    private ShapeRenderer shapeRenderer;

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
        background = new Texture("levelup.jpg"); // imagem de fundo fullscreen
        shapeRenderer = new ShapeRenderer();

        fontBig.setColor(Color.GOLD);
        fontSmall.setColor(Color.WHITE);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        // --- Desenha a imagem de fundo ---
        batch.begin();
        batch.draw(background, 0, 0, screenWidth, screenHeight);
        batch.end();

        // --- Calcula textos ---
        String texto2 = "Distância: " + distanciaFinal + " m";
        String texto3 = "Moedas: " + moedas;
        String texto4 = "ENTER / ESPAÇO — Menu";
        String texto5 = "R — Repetir";
        String texto6 = "1 / 2 / 3 — Outra Fase";

        BitmapFont font = fontSmall;
        font.getData().setScale(1.1f);

        GlyphLayout layout2 = new GlyphLayout(font, texto2);
        GlyphLayout layout3 = new GlyphLayout(font, texto3);
        GlyphLayout layout4 = new GlyphLayout(font, texto4);
        GlyphLayout layout5 = new GlyphLayout(font, texto5);
        GlyphLayout layout6 = new GlyphLayout(font, texto6);

        float espaco = 50f;
        float totalLargura = layout2.width + layout3.width + layout4.width + layout5.width + layout6.width + espaco * 4;
        float startX = (screenWidth - totalLargura) / 2f;
        float y = 50f; // altura do rodapé

        // --- Desenha o retângulo de contraste ---
        float rectAltura = 60f;
        float rectY = y - rectAltura + 15f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.5f); // retângulo preto semitransparente
        shapeRenderer.rect(startX - 20, rectY, totalLargura + 40, rectAltura);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // --- Desenha os textos sobre o retângulo ---
        batch.begin();

        float currentX = startX;
        font.draw(batch, layout2, currentX, y);
        currentX += layout2.width + espaco;
        font.draw(batch, layout3, currentX, y);
        currentX += layout3.width + espaco;
        font.draw(batch, layout4, currentX, y);
        currentX += layout4.width + espaco;
        font.draw(batch, layout5, currentX, y);
        currentX += layout5.width + espaco;
        font.draw(batch, layout6, currentX, y);

        batch.end();

        // --- Mensagem central de destaque ---
        batch.begin();
        fontBig.getData().setScale(2.0f);
        fontBig.draw(batch, "Fase " + fase + " concluída!", screenWidth / 2f - 180, screenHeight / 2f + 80);
        batch.end();

        // --- Navegação ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
            game.setScreen(new MenuScreen(game));

        if (Gdx.input.isKeyJustPressed(Input.Keys.R))
            game.setScreen(new GameScreen(game, fase));

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1))
            game.setScreen(new GameScreen(game, 1));
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2))
            game.setScreen(new GameScreen(game, 2));
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3))
            game.setScreen(new GameScreen(game, 3));

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            game.setScreen(new MenuScreen(game));
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
        if (background != null) background.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
    }
}
