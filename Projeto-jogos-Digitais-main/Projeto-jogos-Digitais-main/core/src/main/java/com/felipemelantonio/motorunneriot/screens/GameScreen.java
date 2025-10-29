package com.felipemelantonio.motorunneriot.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;

import com.felipemelantonio.motorunneriot.MotoRunnerGame;
import com.felipemelantonio.motorunneriot.entities.Background;
import com.felipemelantonio.motorunneriot.entities.Carro;
import com.felipemelantonio.motorunneriot.entities.Moto;

public class GameScreen implements Screen {

    private final MotoRunnerGame game;
    private final int fase;              // 1, 2 ou 3

    private SpriteBatch batch;
    private BitmapFont font;
    private Background background;
    private Moto moto;
    private Array<Carro> carros;

    private int laneCount;               // 2 / 3 / 4
    private float worldSpeed;            // px/s
    private float spawnInterval;         // s
    private float spawnTimer;            // s
    private float distancia;             // m (escala simples)
    private boolean isPaused;

    // Fração da largura da tela usada como margem lateral da pista
    private float insetFactor;

    public GameScreen(MotoRunnerGame game, int faseSelecionada) {
        this.game = game;
        this.fase = Math.max(1, Math.min(3, faseSelecionada));
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        font  = new BitmapFont();
        font.setColor(Color.WHITE);

        carros = new Array<>();
        spawnTimer = 0f;
        distancia  = 0f;
        isPaused   = false;
        Carro.initTextureIfNeeded();

        // ======= CONFIGURAÇÃO POR FASE =======
        switch (fase) {
            case 1: // 2 faixas
                background    = new Background("fase1.png", 200f);
                worldSpeed    = 200f;
                spawnInterval = 1.5f;
                laneCount     = 2;
                insetFactor   = 0.15f; // pista padrão (larga)
                break;

            case 2: // 3 faixas (pista mais estreita)
                background    = new Background("fase2.png", 300f);
                worldSpeed    = 300f;
                spawnInterval = 0.8f;
                laneCount     = 3;
                insetFactor   = 0.22f; // já alinhado na sua fase 2
                break;

            default: // 4 faixas (fase 3)
                background    = new Background("estrada.png", 400f);
                worldSpeed    = 400f;
                spawnInterval = 0.7f;
                laneCount     = 4;
                // 🔧 Ajuste fino para alinhar as FAIXAS EXTERNAS com a textura da fase 3
                // Teste 0.23–0.24 se quiser refinar.
                insetFactor   = 0.235f;
                break;
        }

        // Moto alinhada ao número de faixas e à largura visual da pista (inset)
        moto = new Moto(laneCount, insetFactor);
    }

    @Override
    public void render(float delta) {
        if (isPaused) {
            renderPause();
            return;
        }

        float dt = Math.min(delta, 1f / 45f); // step lógico estável

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Atualização de mundo
        background.setSpeed(worldSpeed);
        background.update(dt);
        moto.update(dt);

        // Spawn de carros
        spawnTimer += dt;
        if (spawnTimer >= spawnInterval) {
            spawnCar();
            spawnTimer = 0f;
        }

        // Atualiza e remove carros fora da tela
        for (int i = carros.size - 1; i >= 0; i--) {
            Carro c = carros.get(i);
            c.update(dt, worldSpeed);
            if (c.getBounds().y + c.getBounds().height < 0) {
                carros.removeIndex(i);
            }
        }

        // Distância percorrida (escala simples px->m)
        distancia += worldSpeed * dt * 0.035f;

        // Colisão
        for (Carro c : carros) {
            if (moto.getBounds().overlaps(c.getBounds())) {
                game.setScreen(new GameOverScreen(game, distancia, fase));
                dispose();
                return;
            }
        }

        // ======== DRAW ========
        batch.begin();
        background.draw(batch);
        for (Carro c : carros) c.draw(batch);
        moto.draw(batch);

        font.draw(batch, "Fase: " + fase, 10, Gdx.graphics.getHeight() - 10);
        font.draw(batch, "Distância: " + (int) distancia + " m", 10, Gdx.graphics.getHeight() - 30);
        font.draw(batch, "Velocidade: " + (int) worldSpeed + " px/s", 10, Gdx.graphics.getHeight() - 50);
        font.draw(batch, "P - Pausar | ESC - Menu", 10, Gdx.graphics.getHeight() - 70);
        batch.end();

        // Entradas globais
        if (Gdx.input.isKeyJustPressed(Input.Keys.P))       isPaused = true;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))  game.setScreen(new MenuScreen(game));
    }

    private void spawnCar() {
        int lane = (int) (Math.random() * laneCount);
        float startY = Gdx.graphics.getHeight() + 40f;
        float vCar   = worldSpeed * 0.4f;

        // Usa o mesmo inset da fase atual para alinhar com a textura
        carros.add(new Carro(lane, startY, vCar, laneCount, insetFactor));
    }

    private void renderPause() {
        Gdx.gl.glClearColor(0, 0, 0, 0.7f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        font.getData().setScale(1.3f);
        font.draw(batch, "=== PAUSADO ===", 320, 400);
        font.getData().setScale(1f);
        font.draw(batch, "P - Retomar", 340, 360);
        font.draw(batch, "R - Reiniciar Fase", 340, 330);
        font.draw(batch, "ESC - Menu Principal", 340, 300);
        batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.P))       isPaused = false;
        if (Gdx.input.isKeyJustPressed(Input.Keys.R))       game.setScreen(new GameScreen(game, fase));
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))  game.setScreen(new MenuScreen(game));
    }

    @Override public void resize(int width, int height) {
        if (background != null) background.onResize();
    }

    @Override public void pause()  { isPaused = true; }
    @Override public void resume() { isPaused = false; }
    @Override public void hide()   {}

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        if (moto != null)        moto.dispose();
        if (background != null)  background.dispose();
        Carro.disposeStatic();
    }
}
