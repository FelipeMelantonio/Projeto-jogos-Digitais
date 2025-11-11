package com.felipemelantonio.motorunneriot.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.felipemelantonio.motorunneriot.MotoRunnerGame;
import com.felipemelantonio.motorunneriot.entities.Background;
import com.felipemelantonio.motorunneriot.entities.Moto;

public class LevelSelectScreen implements Screen {

    private final MotoRunnerGame game;

    private SpriteBatch batch;
    private BitmapFont hint;
    private Background bg;
    private Moto moto;

    private Texture texTitulo, texN1, texN2, texN3;

    private final Rectangle rN1 = new Rectangle();
    private final Rectangle rN2 = new Rectangle();
    private final Rectangle rN3 = new Rectangle();

    // ===== Layout (padronizado) =====
    private static final float TOP_MARGIN          = 0.11f; // margem do topo p/ título
    private static final float TITLE_WIDTH_RATIO   = 0.40f; // largura do título (%W)
    private static final float BTN_WIDTH_RATIO     = 0.20f; // largura dos botões (%W)

    // >>> Proporção fixa dos botões (baseada no Nível 2: 568x187)
    private static final float BTN_REF_W           = 568f;
    private static final float BTN_REF_H           = 187f;
    private static final float BTN_ASPECT          = BTN_REF_H / BTN_REF_W; // 0.329...

    private static final float BTN_SPACE_RATIO_H   = 0.50f; // espaçamento = 50% da ALTURA do botão
    private static final float GAP_TITLE_BTNS_PX   = 24f;   // respiro entre título e 1º botão

    private float roadSpeed = 280f;

    public LevelSelectScreen(MotoRunnerGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        hint = new BitmapFont();
        hint.setColor(new Color(1, 1, 1, 0.9f));

        bg = new Background("fase2.png", roadSpeed);
        moto = new Moto(3, 0.22f);
        moto.setControlsEnabled(false);

        texTitulo = new Texture("Selecionar.png");
        texN1     = new Texture("Nivel1.png");
        texN2     = new Texture("Nivel2.png");
        texN3     = new Texture("Nivel3.png");

        // filtros (pode deixar Linear)
        texTitulo.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        texN1.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        texN2.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        texN3.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        layout();
    }

    /** Calcula posições/medidas garantindo MESMO tamanho e MESMA proporção para os 3 botões. */
    private void layout() {
        float W = Gdx.graphics.getWidth();
        float H = Gdx.graphics.getHeight();

        // === Título (medidas normais) ===
        float titleW = W * TITLE_WIDTH_RATIO;
        float titleH = titleW * texTitulo.getHeight() / texTitulo.getWidth();
        float titleX = (W - titleW) * 0.5f;
        float titleY = H * (1f - TOP_MARGIN) - titleH;

        // === Botões com proporção fixa (568x187) ===
        float btnW = W * BTN_WIDTH_RATIO;     // largura fixa proporcional à tela
        float btnH = btnW * BTN_ASPECT;       // altura derivada da proporção fixa
        float space = btnH * BTN_SPACE_RATIO_H;

        // Centraliza em X
        float btnX = (W - btnW) * 0.5f;

        // Empilha logo abaixo do título
        float n1Y = titleY - GAP_TITLE_BTNS_PX - btnH;
        float n2Y = n1Y - space - btnH;
        float n3Y = n2Y - space - btnH;

        // Define hitboxes
        rN1.set(btnX, n1Y, btnW, btnH);
        rN2.set(btnX, n2Y, btnW, btnH);
        rN3.set(btnX, n3Y, btnW, btnH);
    }

    @Override
    public void render(float delta) {
        float dt = Math.min(delta, 1f / 60f);
        bg.setSpeed(roadSpeed);
        bg.update(dt);
        moto.update(dt, roadSpeed);

        handleInput();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float W = Gdx.graphics.getWidth();
        float H = Gdx.graphics.getHeight();

        // Recalcula medidas do título para desenhar (iguais às de layout)
        float titleW = W * TITLE_WIDTH_RATIO;
        float titleH = titleW * texTitulo.getHeight() / texTitulo.getWidth();
        float titleX = (W - titleW) * 0.5f;
        float titleY = H * (1f - TOP_MARGIN) - titleH;

        batch.begin();
        bg.draw(batch);
        moto.draw(batch);

        // Título
        batch.draw(texTitulo, titleX, titleY, titleW, titleH);

        // Botões – todos com MESMA largura/altura (rN*.width/height)
        batch.draw(texN1, rN1.x, rN1.y, rN1.width, rN1.height);
        batch.draw(texN2, rN2.x, rN2.y, rN2.width, rN2.height);
        batch.draw(texN3, rN3.x, rN3.y, rN3.width, rN3.height);

        // Dica
        hint.getData().setScale(1.0f);
        hint.draw(batch, "↑/↓ navegar   ENTER iniciar   ESC voltar", (W - 460) / 2f, H * 0.075f);
        batch.end();
    }

    private void handleInput() {
        if (Gdx.input.justTouched()) {
            float mx = Gdx.input.getX();
            float my = Gdx.graphics.getHeight() - Gdx.input.getY();

            if (rN1.contains(mx, my)) { game.setScreen(new GameScreen(game, 1)); return; }
            if (rN2.contains(mx, my)) { game.setScreen(new GameScreen(game, 2)); return; }
            if (rN3.contains(mx, my)) { game.setScreen(new GameScreen(game, 3)); return; }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) game.setScreen(new GameScreen(game, 1));
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) game.setScreen(new GameScreen(game, 2));
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) game.setScreen(new GameScreen(game, 3));
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) game.setScreen(new MenuScreen(game));
    }

    @Override public void resize(int width, int height) { layout(); if (bg != null) bg.onResize(); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (hint != null)  hint.dispose();
        if (bg != null)    bg.dispose();
        if (moto != null)  moto.dispose();
        if (texTitulo != null) texTitulo.dispose();
        if (texN1 != null) texN1.dispose();
        if (texN2 != null) texN2.dispose();
        if (texN3 != null) texN3.dispose();
    }
}
