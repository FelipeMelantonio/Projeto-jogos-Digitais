// Conteúdo para: GameScreen.java
package com.felipemelantonio.motorunneriot.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout; // Import necessário!
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align; // Import necessário!
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
    private float worldSpeed; // Velocidade do JOGO (carros, moedas, distância)
    private float spawnTimer;
    private float distancia;
    private boolean isPaused;

    private float insetFactor;
    private LevelManager level;
    private Random rng;

    // ... (Atributos de controle de IA - DWELL, STREAK, etc. - permanecem intocados) ...
    // ...
    private float[] laneDwell;
    private final float DWELL_DECAY = 0.6f;
    private final float DWELL_CAP = 6.0f;
    private int f1OppositeStreak = 0;
    private final int F1_STREAK_CAP = 2;
    private final float F1_DWELL_BIAS_S = 1.2f;
    private int f23NonPlayerStreak = 0;
    private final int F23_STREAK_CAP = 3;
    private final float BIAS_STRENGTH = 1.2f;
    private final float ADJ_FALLOFF_NEAR = 1.0f;
    private final float ADJ_FALLOFF_ADJ = 0.7f;
    private final float ADJ_FALLOFF_FAR = 0.35f;
    private int lastLane = -1;
    private float sameLaneTime = 0f;
    private final float STICK_THRESHOLD = 1.2f;
    private final float STICK_COOLDOWN_SECS = 1.4f;
    private final float STICK_SAFE_FRONT_PX = 200f;
    private float stickCooldown = 0f;
    private boolean finishing = false; 
    private float finishTimer = 0f; 

    // ==== MODO BACKGROUND (para o MenuScreen) ====
    private boolean isBackgroundMode = false;
    
    // Layout para centralização de texto
    private GlyphLayout layout = new GlyphLayout();

    // ==== NOVA FÍSICA DO BACKGROUND "GRAVITACIONAL" ====
    /** Velocidade ATUAL do background (pixels/segundo) */
    private float backgroundSpeed; 
    /** Quanto acelera por segundo ao segurar Espaço (pixels/segundo²) */
    private final float BACKGROUND_ACCELERATION = 900f; 
    /** Quanto desacelera por segundo ao soltar Espaço (atrito/arrasto) */
    private final float BACKGROUND_DRAG = 500f;
    /** Velocidade máxima que o fundo pode atingir */
    private final float MAX_BACKGROUND_SPEED = 2500f;
    // =======================================================


    /**
     * Construtor principal para o jogo normal.
     */
    public GameScreen(MotoRunnerGame game, int faseSelecionada) {
        this(game, faseSelecionada, false); 
    }

    /**
     * Construtor para modo background.
     */
    public GameScreen(MotoRunnerGame game, int faseSelecionada, boolean isBackgroundMode) {
        this.game = game;
        this.fase = Math.max(1, Math.min(3, faseSelecionada));
        this.isBackgroundMode = isBackgroundMode;
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
            case 1: background = new Background("fase1.png", 200f); laneCount = 2; insetFactor = 0.15f; break;
            case 2: background = new Background("fase2.png", 300f); laneCount = 3; insetFactor = 0.22f; break;
            default: background = new Background("estrada.png", 400f); laneCount = 4; insetFactor = 0.235f; break;
        }

        moto = new Moto(laneCount, insetFactor);
        level = new LevelManager(fase);
        laneDwell = new float[laneCount];

        coinIntervalBase = (fase == 1 ? 1.2f : fase == 2 ? 1.0f : 0.9f);
        
        if (isBackgroundMode) {
            worldSpeed = 250f; // Velocidade fixa para o menu
            backgroundSpeed = worldSpeed; // Fundo do menu usa velocidade fixa
        } else {
            level.update(0); 
            worldSpeed = level.worldSpeedPx();
            backgroundSpeed = 0f; // Fundo do jogo começa PARADO
        }
    }

    @Override
    public void render(float delta) {
        float dt = Math.min(delta, 1f / 60f);

        // A velocidade do "mundo" (carros, level) congela se pausado
        if (!isBackgroundMode) {
            if (!isPaused) {
                level.update(dt);
            }
             worldSpeed = level.worldSpeedPx(); // worldSpeed afeta apenas carros, moedas, etc.
        }


        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        // ATUALIZAÇÃO VISUAL (Move se NÃO estiver pausado OU se for modo background)
        
        boolean shouldMove = !isPaused || isBackgroundMode;

        if (shouldMove) {
            // No modo de menu (isBackgroundMode), o backgroundSpeed já foi setado para 250f no show()
            // No modo de jogo (!isBackgroundMode), o backgroundSpeed é controlado pelo Espaço (ver abaixo)
            background.setSpeed(backgroundSpeed); 
            background.update(dt);
        }

        // Atualizações só se não estiver finalizando
        if (!finishing) {

            // === LÓGICA DE JOGO (Spawns, Moto) SÓ RODA SE NÃO FOR BACKGROUND E NÃO ESTIVER PAUSADO ===
            if (!isBackgroundMode && !isPaused) {
                // ATENÇÃO: A moto e os carros ainda são controlados pelo worldSpeed (do LevelManager)
                // Se quiser que a moto acelere com o fundo, teria que mudar aqui.
                // Por enquanto, ela se move com o "level"
                moto.update(dt, worldSpeed);

                // Dwell + “grudado”
                int laneNow = moto.getCurrentLaneIndex();
                if (laneNow == lastLane) sameLaneTime += dt;
                else { sameLaneTime = 0f; lastLane = laneNow; }
                for (int i = 0; i < laneCount; i++) {
                    if (i == laneNow) laneDwell[i] = Math.min(DWELL_CAP, laneDwell[i] + dt);
                    else laneDwell[i] = Math.max(0f, laneDwell[i] - dt * DWELL_DECAY);
                }
                if (stickCooldown > 0f) stickCooldown -= dt;

                // Spawns (controlados pelo 'level')
                spawnTimer += dt;
                if (spawnTimer >= level.spawnInterval()) { spawnWave(); spawnTimer = 0f; }
                tryForceStickSpawn();

                // Coins (controlados pelo 'level')
                coinSpawnTimer += dt;
                float coinIntervalNow = Math.max(0.55f, coinIntervalBase - 0.35f * clamp01(level.getTime() / 60f));
                if (coinSpawnTimer >= coinIntervalNow) { spawnCoin(); coinSpawnTimer = 0f; }


                // ==== NOVA LÓGICA GRAVITACIONAL PARA FUNDO ====
                if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
                    // Acelera
                    backgroundSpeed += BACKGROUND_ACCELERATION * dt;
                } else {
                    // Desacelera (atrito)
                    backgroundSpeed = Math.max(0, backgroundSpeed - BACKGROUND_DRAG * dt);
                }
                // Limita a velocidade máxima
                backgroundSpeed = Math.min(backgroundSpeed, MAX_BACKGROUND_SPEED);
                // ===============================================

            }
        }

        // Atualiza objetos (carros/moedas existentes)
        // ELES CONTINUAM USANDO O worldSpeed (do level), NÃO o backgroundSpeed
        if (shouldMove) { 
            // Os carros se movem na velocidade do level, não do fundo
            float entitySpeed = (isBackgroundMode) ? backgroundSpeed : worldSpeed;

            for (int i = carros.size - 1; i >= 0; i--) {
                Carro c = carros.get(i);
                // No modo de jogo (else), eles usam o worldSpeed do LevelManager
                c.update(dt, entitySpeed); 
                if (c.getBounds().y + c.getBounds().height < 0)
                    carros.removeIndex(i);
            }
            for (int i = moedas.size - 1; i >= 0; i--) {
                Moeda m = moedas.get(i);
                m.update(dt, entitySpeed);
                if (m.getBounds().y + m.getBounds().height < 0) {
                    moedas.removeIndex(i);
                    continue;
                }
            }
        }
        
        // Coleta de Moedas (Lógica que depende do player)
        if (!finishing && !isBackgroundMode && !isPaused) {
             for (int i = moedas.size - 1; i >= 0; i--) {
                Moeda m = moedas.get(i);
                if (moto.getBounds().overlaps(m.getBounds())) {
                    moedas.removeIndex(i);
                    moedasColetadas++;
                }
            }
        }

        // === LÓGICA DE JOGO (DISTANCIA, COLISÃO) SÓ RODA SE NÃO FOR BACKGROUND E NÃO ESTIVER PAUSADO ===
        if (!isBackgroundMode && !isPaused) {
            // Distância é baseada no 'worldSpeed' (LevelManager), não na velocidade do fundo
            distancia += worldSpeed * dt * 0.035f;

            // Checa meta e inicia final
            float meta = getGoalMetersForFase(fase);
            if (!finishing && distancia >= meta) {
                finishing = true;
                finishTimer = 0f;
            }

            // Colisão (baseada no worldSpeed dos carros)
            if (!finishing) {
                for (Carro c : carros) {
                    if (moto.getBounds().overlaps(c.getBounds())) {
                        game.setScreen(new GameOverScreen(game, distancia, fase));
                        dispose();
                        return;
                    }
                }
            }

            // Animação final
            if (finishing) {
                finishTimer += dt;
                moto.getBounds().y += dt * 260f; 
                if (finishTimer >= 2.0f) {
                    game.setScreen(new LevelCompleteScreen(game, fase, (int) distancia, moedasColetadas));
                    dispose();
                    return;
                }
            }
        } // === FIM DO BLOCO (!isBackgroundMode && !isPaused) ===


        // Desenho (Sempre desenha)
        batch.begin();
        background.draw(batch);
        for (Moeda m : moedas) m.draw(batch);
        for (Carro c : carros) c.draw(batch);
        moto.draw(batch); 

        // === HUD SÓ APARECE SE NÃO FOR MODO BACKGROUND E NÃO ESTIVER PAUSADO ===
        if (!isBackgroundMode && !isPaused) {
            float h = Gdx.graphics.getHeight();
            float meta = getGoalMetersForFase(fase);
            
            font.draw(batch, "Fase: " + fase, 10, h - 10);
            font.draw(batch, "Distância: " + (int) distancia + " m (Meta: " + (int) meta + " m)", 10, h - 30);
            // Mostra as duas velocidades (debug)
            font.draw(batch, "Vel. Jogo (Carros): " + (int) worldSpeed + " px/s", 10, h - 50);
            font.draw(batch, "Vel. Fundo (Espaço): " + (int) backgroundSpeed + " px/s", 10, h - 70); 
            font.draw(batch, "Moedas: " + moedasColetadas, 10, h - 90);
            font.draw(batch, "P - Pausar | ESC - Menu", 10, h - 110);


            if (finishing) {
                font.setColor(Color.GOLD);
                font.draw(batch, "FIM DA FASE! Segure-se...", 300, h - 40);
                font.setColor(Color.WHITE);
            }
        }
        
        // === DESENHA O MENU DE PAUSA CENTRALIZADO ===
        if (isPaused) {
            float screenW = Gdx.graphics.getWidth();
            float screenH = Gdx.graphics.getHeight();
            float centerX = screenW / 2f;
            
            // Título
            font.getData().setScale(1.8f);
            String title = "=== PAUSADO ===";
            layout.setText(font, title);
            font.draw(batch, title, centerX - layout.width / 2, screenH * 0.65f); 

            // Opções
            font.getData().setScale(1.2f);
            float lineHeight = font.getLineHeight() * 1.5f;

            String opt1 = "P - Retomar";
            layout.setText(font, opt1);
            font.draw(batch, opt1, centerX - layout.width / 2, screenH * 0.5f + lineHeight);

            String opt2 = "R - Reiniciar Fase";
            layout.setText(font, opt2);
            font.draw(batch, opt2, centerX - layout.width / 2, screenH * 0.5f);

            String opt3 = "ESC - Menu Principal";
            layout.setText(font, opt3);
            font.draw(batch, opt3, centerX - layout.width / 2, screenH * 0.5f - lineHeight);
            
            font.setColor(Color.WHITE); 
        }

        batch.end();

        // === LÓGICA DE INPUT (Separada para Pausa) ===
        if (isBackgroundMode) {
            return; // Sem input no modo de fundo do menu
        }

        if (isPaused) {
            // Input do Menu de Pausa
            if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
                isPaused = false;
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                game.setScreen(new GameScreen(game, fase));
                dispose();
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                game.setScreen(new MenuScreen(game));
                dispose();
            }
        } else {
            // Input do Jogo Rodando
            if (!finishing) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
                    isPaused = true;
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                    game.setScreen(new MenuScreen(game));
                    dispose();
                }
            }
        }
    }

    private float getGoalMetersForFase(int f) {
        switch (f) {
            case 1: return 500f; 
            case 2: return 1200f; 
            case 3: return 1200f; 
            default: return 1200f;
        }
    }

    // ... (todos os seus métodos de spawnWave, tryForceStickSpawn, etc. permanecem aqui) ...

    private void spawnWave() {
        int screenH = Gdx.graphics.getHeight();
        float spawnYBase = screenH + 40f;

        float[] topYByLane = new float[laneCount];
        for (int l = 0; l < laneCount; l++) topYByLane[l] = Float.NEGATIVE_INFINITY;
        for (Carro c : carros) topYByLane[c.getLaneIndex()] = Math.max(topYByLane[c.getLaneIndex()], c.getBounds().y);

        float minGap = level.laneGapPx();

        List<Integer> livres = new ArrayList<>();
        for (int l = 0; l < laneCount; l++)
            if ((spawnYBase - topYByLane[l]) > minGap)
                livres.add(l);
        if (livres.isEmpty()) return;

        boolean isPhase1 = (fase == 1);
        boolean twoLanes = (laneCount == 2);
        int playerLane = moto.getCurrentLaneIndex();

        int lane1;
        if (isPhase1 && twoLanes) {
            int opposite = 1 - playerLane;
            boolean playerLaneFree = livres.contains(playerLane);
            boolean dwellHigh = laneDwell[playerLane] >= F1_DWELL_BIAS_S;
            boolean blockOppStreak = (f1OppositeStreak >= F1_STREAK_CAP);

            if (playerLaneFree && (dwellHigh || blockOppStreak)) lane1 = playerLane;
            else {
                if (playerLaneFree && rng.nextFloat() < 0.65f) lane1 = playerLane;
                else lane1 = livres.get(rng.nextInt(livres.size()));
            }
            if (lane1 == opposite) f1OppositeStreak++;
            else f1OppositeStreak = 0;

        } else {
            lane1 = pickLaneWeighted(livres, playerLane);
            boolean nearPlayer = (Math.abs(lane1 - playerLane) <= 1);
            if (!nearPlayer) f23NonPlayerStreak++;
            else f23NonPlayerStreak = 0;

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
            int lane2 = (fase >= 2) ? pickLaneWeighted(livres, playerLane) : livres.get(rng.nextInt(livres.size()));
            float jitter = 70f + rng.nextFloat() * 110f;
            float y2 = spawnYBase + jitter;
            float minDeltaY = 140f;
            if (Math.abs(jitter) < minDeltaY) y2 = spawnYBase + minDeltaY;

            if (lane2 == playerLane) {
                float motoTop = moto.getBounds().y + moto.getBounds().height;
                float safeStart = motoTop + STICK_SAFE_FRONT_PX;
                if (y2 < safeStart) y2 = safeStart;
            }
            spawnSingleAtLane(lane2, y2);
        }

        if (isPhase1 && twoLanes) {
            spawnTimer = -0.35f;
        }
    }

    private void tryForceStickSpawn() {
        if (sameLaneTime < STICK_THRESHOLD || stickCooldown > 0f) return;

        int targetLane = moto.getCurrentLaneIndex();
        int screenH = Gdx.graphics.getHeight();
        float spawnYBase = screenH + 40f;

        float[] topYByLane = new float[laneCount];
        for (int l = 0; l < laneCount; l++) topYByLane[l] = Float.NEGATIVE_INFINITY;
        for (Carro c : carros) topYByLane[c.getLaneIndex()] = Math.max(topYByLane[c.getLaneIndex()], c.getBounds().y);

        float minGap = level.laneGapPx();

        int chosenLane = targetLane;
        if ((spawnYBase - topYByLane[targetLane]) <= minGap) {
            int alt = findAdjacentFreeLane(spawnYBase, topYByLane, minGap, targetLane);
            if (alt != -1) chosenLane = alt;
            else return;
        }

        float motoTop = moto.getBounds().y + moto.getBounds().height;
        float y = Math.max(spawnYBase, motoTop + STICK_SAFE_FRONT_PX);

        if (fase == 1 && laneCount == 2) {
            int other = 1 - chosenLane;
            float otherTop = topYByLane[other];
            if (otherTop > 0 && (y - otherTop) < 160f) y = otherTop + 180f;
        }

        spawnSingleAtLane(chosenLane, y);
        sameLaneTime = 0f;
        stickCooldown = STICK_COOLDOWN_SECS;
    }

    private int findAdjacentFreeLane(float spawnYBase, float[] topYByLane, float minGap, int center) {
        for (int d = 1; d < laneCount; d++) {
            int l = center - d;
            if (l >= 0 && (spawnYBase - topYByLane[l]) > minGap) return l;
            int r = center + d;
            if (r < laneCount && (spawnYBase - topYByLane[r]) > minGap) return r;
        }
        return -1;
    }

    private void spawnCoin() {
        int screenH = Gdx.graphics.getHeight();
        float spawnYBase = screenH + 40f;

        float[] topYByLane = new float[laneCount];
        for (int l = 0; l < laneCount; l++) topYByLane[l] = Float.NEGATIVE_INFINITY;
        for (Carro c : carros) topYByLane[c.getLaneIndex()] = Math.max(topYByLane[c.getLaneIndex()], c.getBounds().y);

        float minGapCoin = Math.max(120f, level.laneGapPx() * 0.65f);

        List<Integer> livres = new ArrayList<>();
        for (int l = 0; l < laneCount; l++)
            if ((spawnYBase - topYByLane[l]) > minGapCoin)
                livres.add(l);
        if (livres.isEmpty()) return;

        int playerLane = moto.getCurrentLaneIndex();

        int lane;
        List<Integer> near = new ArrayList<>();
        for (int l : livres)
            if (Math.abs(l - playerLane) <= 1)
                near.add(l);

        float r = rng.nextFloat();
        if (livres.contains(playerLane) && r < 0.60f) lane = playerLane;
        else if (!near.isEmpty() && r < 0.90f) lane = near.get(rng.nextInt(near.size()));
        else lane = livres.get(rng.nextInt(livres.size()));

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
        if (candidates.size() == 1) return candidates.get(0);

        float total = 0f;
        float[] weights = new float[candidates.size()];

        for (int i = 0; i < candidates.size(); i++) {
            int lane = candidates.get(i);
            float dwellNorm = (DWELL_CAP <= 0f) ? 0f : Math.min(1f, laneDwell[lane] / DWELL_CAP);

            int dist = Math.abs(lane - playerLane);
            float falloff = (dist == 0) ? ADJ_FALLOFF_NEAR : (dist == 1) ? ADJ_FALLOFF_ADJ : ADJ_FALLOFF_FAR;

            float w = 1.0f + BIAS_STRENGTH * dwellNorm * falloff;
            weights[i] = w;
            total += w;
        }

        float r = rng.nextFloat() * total;
        float acc = 0f;
        for (int i = 0; i < weights.length; i++) {
            acc += weights[i];
            if (r <= acc) return candidates.get(i);
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

    @Override
    public void resize(int width, int height) {
        if (background != null)
            background.onResize();
    }

    @Override
    public void pause() {
        if (!isBackgroundMode) {
            isPaused = true;
        }
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
}

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (font != null) font.dispose();
        if (moto != null)
            moto.dispose();
        if (background != null)
            background.dispose();
        Carro.disposeStatic();
        Moeda.disposeStatic();
    }
}