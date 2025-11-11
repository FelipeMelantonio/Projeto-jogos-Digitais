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
import com.badlogic.gdx.utils.Array;

import com.felipemelantonio.motorunneriot.MotoRunnerGame;
import com.felipemelantonio.motorunneriot.entities.Background;
import com.felipemelantonio.motorunneriot.entities.Carro;
import com.felipemelantonio.motorunneriot.entities.Moeda;
import com.felipemelantonio.motorunneriot.entities.Moto;
import com.felipemelantonio.motorunneriot.utils.LevelManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameScreen implements Screen {

    private final MotoRunnerGame game;
    private final int fase;

    private SpriteBatch batch;
    private BitmapFont font;
    private Background background;
    private Moto moto;
    private Array<Carro> carros;

    // ==== MOEDAS ====
    private Array<Moeda> moedas;
    private float coinSpawnTimer;
    private float coinIntervalBase;
    private int moedasColetadas = 0;

    private int laneCount;
    private float worldSpeed;
    private float spawnTimer;
    private float distancia;
    private boolean isPaused;

    private float insetFactor;
    private LevelManager level;
    private Random rng;

    // === Dwell / distribuição ===
    private float[] laneDwell;
    private final float DWELL_DECAY = 0.6f;
    private final float DWELL_CAP = 6.0f;

    // === Fase 1 “oposta” control ===
    private int f1OppositeStreak = 0;
    private final int F1_STREAK_CAP = 2;
    private final float F1_DWELL_BIAS_S = 1.2f;

    // === Fase 2/3 distribuição ===
    private int f23NonPlayerStreak = 0;
    private final int F23_STREAK_CAP = 3;
    private final float BIAS_STRENGTH = 1.2f;
    private final float ADJ_FALLOFF_NEAR = 1.0f;
    private final float ADJ_FALLOFF_ADJ = 0.7f;
    private final float ADJ_FALLOFF_FAR = 0.35f;

    // === “grudado na faixa” ===
    private int lastLane = -1;
    private float sameLaneTime = 0f;
    private final float STICK_THRESHOLD = 1.2f;
    private final float STICK_COOLDOWN_SECS = 1.4f;
    private final float STICK_SAFE_FRONT_PX = 200f;
    private float stickCooldown = 0f;

    // === FIM DE FASE ===
    private boolean finishing = false; // durante a animação final
    private float finishTimer = 0f; // cronômetro da animação final
    private boolean fadingOut = false;
    private float fadeAlpha = 1f;

    // ==========================
    // === SELEÇÃO DE NÍVEL ===
    // ==========================
    private final boolean selectingLevel; // true quando faseSelecionada == 0
    private Texture texSelecionar;
    private float selX, selY, selW, selH; // posição/dimensões da arte
    private final Rectangle rLv1 = new Rectangle();
    private final Rectangle rLv2 = new Rectangle();
    private final Rectangle rLv3 = new Rectangle();

    public GameScreen(MotoRunnerGame game, int faseSelecionada) {
        this.game = game;
        // se 0 => modo seleção; senão joga
        this.selectingLevel = (faseSelecionada == 0);
        this.fase = Math.max(1, Math.min(3, selectingLevel ? 1 : faseSelecionada));
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.setColor(Color.WHITE);

        carros = new Array<>();
        moedas = new Array<>();
        spawnTimer = 0f;
        coinSpawnTimer = 0f;
        distancia = 0f;
        isPaused = false;
        rng = new Random();

        Carro.initTextureIfNeeded();
        Moeda.initIfNeeded();

        // Config por fase (visual/faixas)
        switch (fase) {
            case 1:
                background = new Background("fase1.png", 200f);
                laneCount = 2;
                insetFactor = 0.15f;
                break;
            case 2:
                background = new Background("fase2.png", 300f);
                laneCount = 3;
                insetFactor = 0.22f;
                break;
            default:
                background = new Background("estrada.png", 400f);
                laneCount = 4;
                insetFactor = 0.235f;
                break;
        }

        moto = new Moto(laneCount, insetFactor);
        level = new LevelManager(fase);
        laneDwell = new float[laneCount];

        coinIntervalBase = (fase == 1 ? 1.2f : fase == 2 ? 1.0f : 0.9f);

        // --- seleção por imagem ---
        if (selectingLevel) {
            moto.setControlsEnabled(false);
            texSelecionar = loadTex("Selecionar.png"); // coloque em android/assets/ com este nome
            layoutSelection();
        }
    }

    private Texture loadTex(String path) {
        try {
            if (!Gdx.files.internal(path).exists())
                return null;
            Texture t = new Texture(path);
            t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            return t;
        } catch (Exception e) {
            return null;
        }
    }

    /** Centraliza Selecionar.png e cria as hitboxes dos 3 botões */
    private void layoutSelection() {
        if (texSelecionar == null)
            return;
        float W = Gdx.graphics.getWidth();
        float H = Gdx.graphics.getHeight();

        // ocupa ~60% da largura, mantendo proporção
        selW = W * 0.60f;
        selH = selW * ((float) texSelecionar.getHeight() / texSelecionar.getWidth());
        selX = (W - selW) * 0.5f;
        selY = H * 0.62f - selH * 0.5f; // um pouco acima do centro

        // A arte (768x768) – percentuais aproximados das áreas dos botões
        float bw = selW * 0.74f;
        float bh = selH * 0.13f;
        float bx = selX + selW * 0.13f;

        float by1 = selY + selH * 0.48f; // NÍVEL 1
        float by2 = selY + selH * 0.32f; // NÍVEL 2
        float by3 = selY + selH * 0.16f; // NÍVEL 3

        rLv1.set(bx, by1, bw, bh);
        rLv2.set(bx, by2, bw, bh);
        rLv3.set(bx, by3, bw, bh);
    }

    @Override
    public void render(float delta) {

        // === OVERLAY DE SELEÇÃO DE NÍVEL ===
        if (selectingLevel) {
            float dt = Math.min(delta, 1f / 60f);

            // Fundo animado
            worldSpeed = 260f;
            background.setSpeed(worldSpeed);
            background.update(dt);
            moto.update(dt, 0f); // personagem parado

            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            batch.begin();
            background.draw(batch);
            moto.draw(batch);
            if (texSelecionar != null) {
                batch.draw(texSelecionar, selX, selY, selW, selH);
            } else {
                // fallback textual
                font.getData().setScale(2.0f);
                font.setColor(Color.GOLD);
                font.draw(batch, "SELECIONE O NÍVEL", Gdx.graphics.getWidth() * 0.5f - 220f,
                        Gdx.graphics.getHeight() * 0.60f);
                font.setColor(Color.WHITE);
                font.getData().setScale(1.6f);
                font.draw(batch, "Nível 1", Gdx.graphics.getWidth() * 0.5f - 60f, Gdx.graphics.getHeight() * 0.50f);
                font.draw(batch, "Nível 2", Gdx.graphics.getWidth() * 0.5f - 60f, Gdx.graphics.getHeight() * 0.43f);
                font.draw(batch, "Nível 3", Gdx.graphics.getWidth() * 0.5f - 60f, Gdx.graphics.getHeight() * 0.36f);
            }
            batch.end();

            // Mouse/Touch
            if (Gdx.input.justTouched()) {
                float mx = Gdx.input.getX();
                float my = Gdx.graphics.getHeight() - Gdx.input.getY();
                if (rLv1.contains(mx, my)) {
                    game.setScreen(new GameScreen(game, 1));
                    return;
                }
                if (rLv2.contains(mx, my)) {
                    game.setScreen(new GameScreen(game, 2));
                    return;
                }
                if (rLv3.contains(mx, my)) {
                    game.setScreen(new GameScreen(game, 3));
                    return;
                }
            }
            // Teclado
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1)) {
                game.setScreen(new GameScreen(game, 1));
                return;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_2)) {
                game.setScreen(new GameScreen(game, 2));
                return;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_3)) {
                game.setScreen(new GameScreen(game, 3));
                return;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                game.setScreen(new MenuScreen(game));
                return;
            }

            return; // não processa o resto do jogo enquanto está no overlay
        }

        // ====== JOGO NORMAL ======
        if (isPaused) {
            renderPause();
            return;
        }

        float dt = Math.min(delta, 1f / 60f);
        level.update(dt);
        worldSpeed = level.worldSpeedPx();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        background.setSpeed(worldSpeed);
        background.update(dt);

        // Atualizações só se não estiver finalizando
        if (!finishing) {
            moto.update(dt, worldSpeed);

            // Dwell + “grudado”
            int laneNow = moto.getCurrentLaneIndex();
            if (laneNow == lastLane)
                sameLaneTime += dt;
            else {
                sameLaneTime = 0f;
                lastLane = laneNow;
            }

            for (int i = 0; i < laneCount; i++) {
                if (i == laneNow)
                    laneDwell[i] = Math.min(DWELL_CAP, laneDwell[i] + dt);
                else
                    laneDwell[i] = Math.max(0f, laneDwell[i] - dt * DWELL_DECAY);
            }
            if (stickCooldown > 0f)
                stickCooldown -= dt;

            // Spawns padrão
            spawnTimer += dt;
            if (spawnTimer >= level.spawnInterval()) {
                spawnWave();
                spawnTimer = 0f;
            }

            // Força spawn se ficar muito na mesma faixa
            tryForceStickSpawn();

            // Coins
            coinSpawnTimer += dt;
            float coinIntervalNow = Math.max(0.55f, coinIntervalBase - 0.35f * clamp01(level.getTime() / 60f));
            if (coinSpawnTimer >= coinIntervalNow) {
                spawnCoin();
                coinSpawnTimer = 0f;
            }
        }

        // Atualiza objetos
        for (int i = carros.size - 1; i >= 0; i--) {
            Carro c = carros.get(i);

            if (finishing) {
                // Quando o jogador vence, os carros sobem suavemente (efeito de "fuga")
                c.getBounds().y += 220 * dt; // velocidade para cima
                if (c.getBounds().y > Gdx.graphics.getHeight() + 100) {
                    carros.removeIndex(i); // remove quando sai da tela
                }
            } else {
                // Movimento normal dos carros durante o jogo
                c.update(dt, worldSpeed);
                if (c.getBounds().y + c.getBounds().height < 0)
                    carros.removeIndex(i);
            }
        }
        for (int i = moedas.size - 1; i >= 0; i--) {
            Moeda m = moedas.get(i);
            m.update(dt, worldSpeed);
            if (m.getBounds().y + m.getBounds().height < 0) {
                moedas.removeIndex(i);
                continue;
            }
            if (!finishing && moto.getBounds().overlaps(m.getBounds())) {
                moedas.removeIndex(i);
                moedasColetadas++;
            }
        }

        // Distância
        distancia += worldSpeed * dt * 0.035f;

        // Checa meta e inicia final
        float meta = getGoalMetersForFase(fase);
        if (!finishing && distancia >= meta) {
            finishing = true;
            finishTimer = 0f;

            // Para spawns novos
            spawnTimer = -9999f;
            coinSpawnTimer = -9999f;

            // Transição suave (fade + fuga para cima)
            fadingOut = true;
            fadeAlpha = 1f;
        }

        // Colisão somente se não estiver finalizando
        if (!finishing) {
            for (Carro c : carros) {
                if (moto.getBounds().overlaps(c.getBounds())) {
                    game.setScreen(new GameOverScreen(game, distancia, fase));
                    dispose();
                    return;
                }
            }
        }

        // Animação final: moto vai para frente e “sai” da tela
        if (finishing) {
            finishTimer += dt;
            moto.getBounds().y += dt * 260f; // sobe a moto
            background.setSpeed(worldSpeed * 1.3f);
            // após ~2s troca para tela de vitória
            if (finishTimer >= 2.0f) {
                game.setScreen(new LevelCompleteScreen(game, fase, (int) distancia, moedasColetadas));
                dispose();
                return;
            }
        }

        // Desenho
        batch.begin();
        background.draw(batch);
        if (!finishing) {
            for (Moeda m : moedas)
                m.draw(batch);
            for (Carro c : carros)
                c.draw(batch);
        } else {
            // Durante o final, cria um fade suave e movimento natural
            if (fadingOut) {
                fadeAlpha -= delta * 0.8f; // velocidade do fade (1.25s até sumir)
                if (fadeAlpha <= 0f) {
                    fadeAlpha = 0f;
                    fadingOut = false;
                    carros.clear();
                    moedas.clear();
                }
            }

            Color oldColor = batch.getColor();
            batch.setColor(oldColor.r, oldColor.g, oldColor.b, fadeAlpha);

            // Carros descem naturalmente enquanto desaparecem
            for (Carro c : carros) {
                c.getBounds().y -= delta * 120f; // desliza para fora da tela
                c.draw(batch);
            }

            for (Moeda m : moedas) {
                m.getBounds().y -= delta * 120f;
                m.draw(batch);
            }

            batch.setColor(oldColor);
        }
        moto.draw(batch);

        float h = Gdx.graphics.getHeight();
        font.draw(batch, "Fase: " + fase, 10, h - 10);
        font.draw(batch, "Distância: " + (int) distancia + " m (Meta: " + (int) meta + " m)", 10, h - 30);
        font.draw(batch, "Velocidade: " + (int) worldSpeed + " px/s", 10, h - 50);
        font.draw(batch, "Moedas: " + moedasColetadas, 10, h - 70);
        font.draw(batch, "P - Pausar | ESC - Menu", 10, h - 90);

        if (finishing) {
            font.setColor(Color.GOLD);
            font.draw(batch, "FIM DA FASE! Segure-se...", 300, h - 40);
            font.setColor(Color.WHITE);
        }

        batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.P))
            isPaused = true;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            game.setScreen(new MenuScreen(game));
    }

    private float getGoalMetersForFase(int f) {
        switch (f) {
            case 1:
                return 500f; // Fase 1
            case 2:
                return 1200f; // Fase 2
            case 3:
                return 1200f; // Fase 3
            default:
                return 1200f;
        }
    }

    // ================== IA de spawn (igual ao seu código, preservado)
    // ==================
    private void spawnWave() {
        int screenH = Gdx.graphics.getHeight();
        float spawnYBase = screenH + 40f;

        float[] topYByLane = new float[laneCount];
        for (int l = 0; l < laneCount; l++)
            topYByLane[l] = Float.NEGATIVE_INFINITY;
        for (Carro c : carros)
            topYByLane[c.getLaneIndex()] = Math.max(topYByLane[c.getLaneIndex()], c.getBounds().y);

        float minGap = level.laneGapPx();

        List<Integer> livres = new ArrayList<>();
        for (int l = 0; l < laneCount; l++)
            if ((spawnYBase - topYByLane[l]) > minGap)
                livres.add(l);
        if (livres.isEmpty())
            return;

        boolean isPhase1 = (fase == 1);
        boolean twoLanes = (laneCount == 2);
        int playerLane = moto.getCurrentLaneIndex();

        int lane1;
        if (isPhase1 && twoLanes) {
            int opposite = 1 - playerLane;
            boolean playerLaneFree = livres.contains(playerLane);
            boolean dwellHigh = laneDwell[playerLane] >= F1_DWELL_BIAS_S;
            boolean blockOppStreak = (f1OppositeStreak >= F1_STREAK_CAP);

            if (playerLaneFree && (dwellHigh || blockOppStreak))
                lane1 = playerLane;
            else {
                if (playerLaneFree && rng.nextFloat() < 0.65f)
                    lane1 = playerLane;
                else
                    lane1 = livres.get(rng.nextInt(livres.size()));
            }
            if (lane1 == opposite)
                f1OppositeStreak++;
            else
                f1OppositeStreak = 0;

        } else {
            lane1 = pickLaneWeighted(livres, playerLane);
            boolean nearPlayer = (Math.abs(lane1 - playerLane) <= 1);
            if (!nearPlayer)
                f23NonPlayerStreak++;
            else
                f23NonPlayerStreak = 0;

            if (f23NonPlayerStreak >= F23_STREAK_CAP) {
                List<Integer> near = new ArrayList<>();
                for (int l : livres)
                    if (Math.abs(l - playerLane) <= 1)
                        near.add(l);
                if (!near.isEmpty())
                    lane1 = near.get(rng.nextInt(near.size()));
                f23NonPlayerStreak = 0;
            }
        }

        spawnSingleAtLane(lane1, spawnYBase);
        livres.remove((Integer) lane1);

        boolean allowDouble = !(isPhase1 && twoLanes);
        boolean doubleSpawn = allowDouble && rng.nextFloat() < level.pDouble() && !livres.isEmpty();
        if (doubleSpawn) {
            int lane2 = (fase >= 2) ? pickLaneWeighted(livres, playerLane)
                    : livres.get(rng.nextInt(livres.size()));

            float jitter = 70f + rng.nextFloat() * 110f;
            float y2 = spawnYBase + jitter;
            float minDeltaY = 140f;
            if (Math.abs(jitter) < minDeltaY)
                y2 = spawnYBase + minDeltaY;

            if (lane2 == playerLane) {
                float motoTop = moto.getBounds().y + moto.getBounds().height;
                float safeStart = motoTop + STICK_SAFE_FRONT_PX;
                if (y2 < safeStart)
                    y2 = safeStart;
            }

            spawnSingleAtLane(lane2, y2);
        }

        if (isPhase1 && twoLanes) {
            spawnTimer = -0.35f;
        }
    }

    private void tryForceStickSpawn() {
        if (sameLaneTime < STICK_THRESHOLD || stickCooldown > 0f)
            return;

        int targetLane = moto.getCurrentLaneIndex();
        int screenH = Gdx.graphics.getHeight();
        float spawnYBase = screenH + 40f;

        float[] topYByLane = new float[laneCount];
        for (int l = 0; l < laneCount; l++)
            topYByLane[l] = Float.NEGATIVE_INFINITY;
        for (Carro c : carros)
            topYByLane[c.getLaneIndex()] = Math.max(topYByLane[c.getLaneIndex()], c.getBounds().y);

        float minGap = level.laneGapPx();

        int chosenLane = targetLane;
        if ((spawnYBase - topYByLane[targetLane]) <= minGap) {
            int alt = findAdjacentFreeLane(spawnYBase, topYByLane, minGap, targetLane);
            if (alt != -1)
                chosenLane = alt;
            else
                return;
        }

        float motoTop = moto.getBounds().y + moto.getBounds().height;
        float y = Math.max(spawnYBase, motoTop + STICK_SAFE_FRONT_PX);

        if (fase == 1 && laneCount == 2) {
            int other = 1 - chosenLane;
            float otherTop = topYByLane[other];
            if (otherTop > 0 && (y - otherTop) < 160f)
                y = otherTop + 180f;
        }

        spawnSingleAtLane(chosenLane, y);

        sameLaneTime = 0f;
        stickCooldown = STICK_COOLDOWN_SECS;
    }

    private int findAdjacentFreeLane(float spawnYBase, float[] topYByLane, float minGap, int center) {
        for (int d = 1; d < laneCount; d++) {
            int l = center - d;
            if (l >= 0 && (spawnYBase - topYByLane[l]) > minGap)
                return l;
            int r = center + d;
            if (r < laneCount && (spawnYBase - topYByLane[r]) > minGap)
                return r;
        }
        return -1;
    }

    private void spawnCoin() {
        int screenH = Gdx.graphics.getHeight();
        float spawnYBase = screenH + 40f;

        float[] topYByLane = new float[laneCount];
        for (int l = 0; l < laneCount; l++)
            topYByLane[l] = Float.NEGATIVE_INFINITY;
        for (Carro c : carros)
            topYByLane[c.getLaneIndex()] = Math.max(topYByLane[c.getLaneIndex()], c.getBounds().y);

        float minGapCoin = Math.max(120f, level.laneGapPx() * 0.65f);

        List<Integer> livres = new ArrayList<>();
        for (int l = 0; l < laneCount; l++)
            if ((spawnYBase - topYByLane[l]) > minGapCoin)
                livres.add(l);
        if (livres.isEmpty())
            return;

        int playerLane = moto.getCurrentLaneIndex();

        int lane;
        List<Integer> near = new ArrayList<>();
        for (int l : livres)
            if (Math.abs(l - playerLane) <= 1)
                near.add(l);

        float r = rng.nextFloat();
        if (livres.contains(playerLane) && r < 0.60f)
            lane = playerLane;
        else if (!near.isEmpty() && r < 0.90f)
            lane = near.get(rng.nextInt(near.size()));
        else
            lane = livres.get(rng.nextInt(livres.size()));

        float extraSafe = 60f;
        if ((spawnYBase - topYByLane[lane]) < (minGapCoin + extraSafe)) {
            for (int l : near) {
                if ((spawnYBase - topYByLane[l]) > (minGapCoin + extraSafe)) {
                    lane = l;
                    break;
                }
            }
        }

        float[] centers = computeLaneCenters(laneCount, insetFactor);
        moedas.add(new Moeda(centers, lane, spawnYBase));
    }

    private float[] computeLaneCenters(int laneCount, float inset) {
        float screenWidth = Gdx.graphics.getWidth();
        float margem = screenWidth * inset;
        float larguraPista = screenWidth - (margem * 2);

        switch (laneCount) {
            case 4: {
                float[] frac = new float[] { 1f / 8f, 3f / 8f, 5f / 8f, 7f / 8f };
                return new float[] {
                        margem + larguraPista * frac[0],
                        margem + larguraPista * frac[1],
                        margem + larguraPista * frac[2],
                        margem + larguraPista * frac[3]
                };
            }
            case 3: {
                float esp = larguraPista / 4f;
                return new float[] {
                        margem + esp * 1f,
                        margem + esp * 2f,
                        margem + esp * 3f
                };
            }
            default: {
                float esp = larguraPista / 3f;
                return new float[] {
                        margem + esp * 1f,
                        margem + esp * 2f
                };
            }
        }
    }

    private int pickLaneWeighted(List<Integer> candidates, int playerLane) {
        if (candidates.size() == 1)
            return candidates.get(0);

        float total = 0f;
        float[] weights = new float[candidates.size()];

        for (int i = 0; i < candidates.size(); i++) {
            int lane = candidates.get(i);
            float dwellNorm = (DWELL_CAP <= 0f) ? 0f : Math.min(1f, laneDwell[lane] / DWELL_CAP);

            int dist = Math.abs(lane - playerLane);
            float falloff = (dist == 0) ? ADJ_FALLOFF_NEAR
                    : (dist == 1) ? ADJ_FALLOFF_ADJ
                            : ADJ_FALLOFF_FAR;

            float w = 1.0f + BIAS_STRENGTH * dwellNorm * falloff;
            weights[i] = w;
            total += w;
        }

        float r = rng.nextFloat() * total;
        float acc = 0f;
        for (int i = 0; i < weights.length; i++) {
            acc += weights[i];
            if (r <= acc)
                return candidates.get(i);
        }
        return candidates.get(candidates.size() - 1);
    }

    private void spawnSingleAtLane(int lane, float spawnY) {
        float vCar = worldSpeed * level.rivalSpeedFactor();
        carros.add(new Carro(lane, spawnY, vCar, laneCount, insetFactor));
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    // ============================== Pausa ==============================
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

        if (Gdx.input.isKeyJustPressed(Input.Keys.P))
            isPaused = false;
        if (Gdx.input.isKeyJustPressed(Input.Keys.R))
            game.setScreen(new GameScreen(game, fase));
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            game.setScreen(new MenuScreen(game));
    }

    @Override
    public void resize(int width, int height) {
        if (background != null)
            background.onResize();
        if (selectingLevel)
            layoutSelection();
    }

    @Override
    public void pause() {
        isPaused = true;
    }

    @Override
    public void resume() {
        isPaused = false;
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        if (moto != null)
            moto.dispose();
        if (background != null)
            background.dispose();
        Carro.disposeStatic();
        Moeda.disposeStatic();
        if (texSelecionar != null)
            texSelecionar.dispose();
    }
}
