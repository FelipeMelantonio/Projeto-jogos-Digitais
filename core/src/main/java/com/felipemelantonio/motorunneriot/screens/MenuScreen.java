package com.felipemelantonio.motorunneriot.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.felipemelantonio.motorunneriot.MotoRunnerGame;

public class MenuScreen implements Screen {

    private final MotoRunnerGame game;
    private SpriteBatch batch;
    private BitmapFont fontTitle, fontOptions;

    private Texture background; // menu_bg.png (opcional)
    private Texture logo;       // logo.png (opcional)
    private Sound hoverSound;   // hover.wav (opcional)
    private Sound clickSound;   // click.wav (opcional)
    private Music music;        // menu_music.mp3 (opcional)

    private int selectedOption = 0;
    private String[] options = {
            "Fase 1 — Coordenação Inicial (2 faixas)",
            "Fase 2 — Resistência e Ritmo (3 faixas)",
            "Fase 3 — Potência e Reflexo (4 faixas)",
            "Sair"
    };

    public MenuScreen(MotoRunnerGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        Gdx.app.log("Menu", "show()");
        batch = new SpriteBatch();
        fontTitle = new BitmapFont();
        fontOptions = new BitmapFont();
        fontTitle.setColor(Color.GOLD);
        fontOptions.setColor(Color.WHITE);

        background = tryLoadTexture("menu_bg.png");
        logo       = tryLoadTexture("logo.png");
        hoverSound = tryLoadSound("hover.wav");
        clickSound = tryLoadSound("click.wav");
        music      = tryLoadMusic("menu_music.mp3");

        if (music != null) {
            music.setLooping(true);
            music.setVolume(0.4f);
            music.play();
        }
    }

    @Override
    public void render(float delta) {
        handleInput();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        // Fundo
        if (background != null) {
            batch.setColor(1, 1, 1, 0.95f);
            batch.draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            batch.setColor(1, 1, 1, 1);
        }

        // Título
        fontTitle.getData().setScale(2.2f);
        String title = "🏍️  MOTO RUNNER IoT";
        float titleX = (Gdx.graphics.getWidth() - 520) / 2f;
        fontTitle.draw(batch, title, titleX, 500);

        // Logo (opcional)
        if (logo != null) {
            float lx = (Gdx.graphics.getWidth() - logo.getWidth()) / 2f;
            batch.draw(logo, lx, 360);
        }

        // Opções
        fontOptions.getData().setScale(1.3f);
        float startY = 300;
        float x = 200;
        float line = 46;

        for (int i = 0; i < options.length; i++) {
            boolean sel = (i == selectedOption);
            fontOptions.setColor(sel ? Color.YELLOW : Color.WHITE);
            float y = startY - i * line;
            fontOptions.draw(batch, options[i], x, y);
        }

        fontOptions.setColor(Color.LIGHT_GRAY);
        fontOptions.getData().setScale(1f);
        fontOptions.draw(batch, "↑/↓ ou 1/2/3 | ENTER/ESPAÇO para selecionar | ESC para sair", 140, 90);

        batch.end();
    }

    private void handleInput() {
        // Navegação
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) changeSelection(-1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) changeSelection(1);

        // Atalhos por número
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) { selectedOption = 0; confirmSelection(); }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) { selectedOption = 1; confirmSelection(); }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) { selectedOption = 2; confirmSelection(); }

        // Confirmar
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            confirmSelection();
        }

        // Sair
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            stopAudio();
            Gdx.app.exit();
        }
    }

    private void changeSelection(int dir) {
        int prev = selectedOption;
        selectedOption = (selectedOption + dir + options.length) % options.length;
        if (hoverSound != null && selectedOption != prev) hoverSound.play(0.4f);
        Gdx.app.log("Menu", "selectedOption=" + selectedOption);
    }

    private void confirmSelection() {
        if (clickSound != null) clickSound.play();
        Gdx.app.log("Menu", "confirm -> option " + selectedOption);
        stopAudio(); // para a música antes de trocar de tela

        switch (selectedOption) {
            case 0:
                game.setScreen(new GameScreen(game, 1));
                break;
            case 1:
                game.setScreen(new GameScreen(game, 2));
                break;
            case 2:
                game.setScreen(new GameScreen(game, 3));
                break;
            case 3:
                Gdx.app.exit();
                break;
            default:
                break;
        }

        // ⚠️ NÃO chamar dispose() aqui — evita crash nativo
    }

    private void stopAudio() {
        if (music != null) music.stop();
    }

    private Texture tryLoadTexture(String path) {
        try {
            if (!Gdx.files.internal(path).exists()) return null;
            Texture t = new Texture(path);
            t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            return t;
        } catch (Exception e) {
            Gdx.app.error("Menu", "Erro ao carregar textura: " + path, e);
            return null;
        }
    }

    private Sound tryLoadSound(String path) {
        try {
            if (!Gdx.files.internal(path).exists()) return null;
            return Gdx.audio.newSound(Gdx.files.internal(path));
        } catch (Exception e) {
            Gdx.app.error("Menu", "Erro ao carregar som: " + path, e);
            return null;
        }
    }

    private Music tryLoadMusic(String path) {
        try {
            if (!Gdx.files.internal(path).exists()) return null;
            return Gdx.audio.newMusic(Gdx.files.internal(path));
        } catch (Exception e) {
            Gdx.app.error("Menu", "Erro ao carregar música: " + path, e);
            return null;
        }
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (fontTitle != null) fontTitle.dispose();
        if (fontOptions != null) fontOptions.dispose();
        if (background != null) background.dispose();
        if (logo != null) logo.dispose();
        if (hoverSound != null) hoverSound.dispose();
        if (clickSound != null) clickSound.dispose();
        if (music != null) music.dispose();
    }
}
