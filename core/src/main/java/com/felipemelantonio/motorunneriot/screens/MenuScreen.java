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

public class MenuScreen implements Screen {

    private final MotoRunnerGame game;
    private SpriteBatch batch;
    private BitmapFont font, small;
    private Background bg;
    private Moto moto;

    private Texture texLogo, texJogar, texSair;

    // Retângulos de clique/desenho (iguais ao que aparece na tela)
    private final Rectangle rJogar = new Rectangle();
    private final Rectangle rSair = new Rectangle();

    private float roadSpeed = 280f;

    // posição/tamanho do logo
    private float _logoX, _logoY, _logoW, _logoH;

    // ===== parâmetros de layout (fáceis de ajustar) =====
    private static final float TOP_MARGIN_RATIO = 0.08f; // margem superior do logo (%H)
    private static final float TITLE_WIDTH_RATIO = 0.68f; // %W para o logo
    private static final float BTN_WIDTH_RATIO = 0.28f; // %W para botões (padronizados)
    private static final float BTN_GAP_RATIO = 0.40f; // gap vertical = % da ALTURA do botão
    private static final float BOTTOM_MARGIN_RATIO = 0.10f; // margem inferior (%H)
    private static final float HINT_Y_RATIO = 0.05f; // posição do texto de dica

    public MenuScreen(MotoRunnerGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        small = new BitmapFont();
        font.setColor(Color.WHITE);
        small.setColor(new Color(1, 1, 1, 0.85f));

        bg = new Background("fase2.png", roadSpeed);
        moto = new Moto(3, 0.22f);
        moto.setControlsEnabled(false);

        texLogo = loadOrNull("Logo.png");
        texJogar = loadOrNull("Jogar.png");
        texSair = loadOrNull("Sair.png");

        // filtros para nitidez
        if (texLogo != null)
            texLogo.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        if (texJogar != null)
            texJogar.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        if (texSair != null)
            texSair.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        layout();
    }

    private Texture loadOrNull(String path) {
        try {
            if (!Gdx.files.internal(path).exists())
                return null;
            return new Texture(path);
        } catch (Exception e) {
            return null;
        }
    }

    private void layout() {
        final float W = Gdx.graphics.getWidth();
        final float H = Gdx.graphics.getHeight();

        // Margens seguras
        final float TOP_MARGIN = H * 0.08f;
        final float BOTTOM_MARGIN = H * 0.10f;

        // Larguras-alvo (antes de checar se cabe)
        float logoW = W * 0.60f; // ligeiramente menor p/ sobrar respiro
        float btnW = W * 0.26f; // botões padronizados

        // Alturas a partir da proporção das imagens (fallbacks caso não exista textura)
        float logoH = (texLogo != null) ? logoW * texLogo.getHeight() / texLogo.getWidth() : H * 0.12f;
        // Use o aspecto do JOGAR como referência para ambos os botões
        float btnAspect = (texJogar != null)
                ? (float) texJogar.getHeight() / (float) texJogar.getWidth()
                : 0.24f; // fallback
        float btnH = btnW * btnAspect;

        // Um único espaçamento para padronizar visualmente
        float SP = H * 0.035f;

        // Altura total do bloco (logo + jogar + sair + espaçamentos)
        float total = logoH + SP + btnH + SP + btnH;

        // Área útil entre as margens
        float available = H - TOP_MARGIN - BOTTOM_MARGIN;

        // Se não couber, escala tudo (logo, botões E espaçamento) com o mesmo fator
        if (total > available) {
            float f = available / total;
            logoW *= f;
            logoH *= f;
            btnW *= f;
            btnH *= f;
            SP *= f;
            total = logoH + SP + btnH + SP + btnH; // recalc
        }

        // Centraliza verticalmente o bloco dentro da área útil (respeitando margens)
        float blockBottom = BOTTOM_MARGIN + (available - total) * 0.5f;

        // Calcula posições de baixo para cima: SAIR -> JOGAR -> LOGO
        float ySair = blockBottom;
        float yJogar = ySair + btnH + SP;
        float yLogo = yJogar + btnH + SP;

        // Centraliza horizontalmente
        float logoX = (W - logoW) * 0.5f;
        float btnX = (W - btnW) * 0.5f;

        // Grava nos campos/retângulos usados no draw e no hitbox
        _logoX = logoX;
        _logoY = yLogo;
        _logoW = logoW;
        _logoH = logoH;

        rJogar.set(btnX, yJogar, btnW, btnH);
        rSair.set(btnX, ySair, btnW, btnH);
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

        batch.begin();
        // Fundo e moto (moto atrás da UI)
        bg.draw(batch);
        moto.draw(batch);

        // LOGO
        if (texLogo != null) {
            batch.draw(texLogo, _logoX, _logoY, _logoW, _logoH);
        } else {
            float W = Gdx.graphics.getWidth();
            font.getData().setScale(2.0f);
            font.draw(batch, "MOTO RUNNER IoT", W * 0.5f - 260f, _logoY + _logoH * 0.75f);
        }

        // BOTÕES (desenha usando exatamente os retângulos de clique)
        if (texJogar != null)
            batch.draw(texJogar, rJogar.x, rJogar.y, rJogar.width, rJogar.height);
        if (texSair != null)
            batch.draw(texSair, rSair.x, rSair.y, rSair.width, rSair.height);

        // Dica
        small.getData().setScale(1.1f);
        small.draw(batch, "ENTER para JOGAR  •  ESC para SAIR",
                25, Gdx.graphics.getHeight() * HINT_Y_RATIO);

        batch.end();
    }

    private void handleInput() {
        float H = Gdx.graphics.getHeight();

        // Clique do mouse/touch
        if (Gdx.input.justTouched()) {
            float mx = Gdx.input.getX();
            float my = H - Gdx.input.getY();
            if (rJogar.contains(mx, my)) {
                game.setScreen(new LevelSelectScreen(game));
                return;
            }
            if (rSair.contains(mx, my)) {
                Gdx.app.exit();
                return;
            }
        }

        // Teclado
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            game.setScreen(new LevelSelectScreen(game));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }
    }

    @Override
    public void resize(int width, int height) {
        layout();
        if (bg != null)
            bg.onResize();
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

    @Override
    public void dispose() {
        if (batch != null)
            batch.dispose();
        if (font != null)
            font.dispose();
        if (small != null)
            small.dispose();
        if (bg != null)
            bg.dispose();
        if (moto != null)
            moto.dispose();
        if (texLogo != null)
            texLogo.dispose();
        if (texJogar != null)
            texJogar.dispose();
        if (texSair != null)
            texSair.dispose();
    }
}
