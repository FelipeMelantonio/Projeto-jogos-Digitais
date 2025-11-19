package com.felipemelantonio.motorunneriot.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

import com.felipemelantonio.motorunneriot.MotoRunnerGame;
import com.felipemelantonio.motorunneriot.entities.Background;
import com.felipemelantonio.motorunneriot.entities.Carro;
import com.felipemelantonio.motorunneriot.entities.Moeda;
import com.felipemelantonio.motorunneriot.entities.Moto;
import com.felipemelantonio.motorunneriot.utils.LevelManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * GameScreen
 * ----------
 * Esta classe representa a TELA DE JOGO em si, onde:
 * - A moto é controlada pelo jogador
 * - Carros inimigos são gerados (spawn) com uma lógica de IA
 * - Moedas aparecem em posições seguras para o jogador coletar
 * - A dificuldade evolui com o tempo (LevelManager)
 * - A velocidade global do "mundo" é controlada (base + boost/pedalada)
 * - É detectado fim de fase, colisão, pausa e transições para outras telas.
 *
 * Ela implementa a interface Screen do LibGDX, então possui métodos do ciclo de
 * vida:
 * show(), render(), resize(), pause(), resume(), hide(), dispose().
 */
public class GameScreen implements Screen {

    // Referência para o "jogo principal" (classe MotoRunnerGame, que estende Game)
    // Usamos essa referência para trocar de telas: setScreen(new OutraTela(...))
    private final MotoRunnerGame game;

    // Número da fase atual (1, 2 ou 3).
    // Ele é recebido no construtor, mas já vem clampado para ficar neste intervalo.
    private final int fase;

    // Objetos básicos de renderização
    private SpriteBatch batch; // responsável por desenhar sprites na tela
    private BitmapFont font; // fonte para desenhar textos (HUD, pausa, etc.)
    private Background background; // fundo animado da estrada

    // Entidades principais do jogo
    private Moto moto; // moto controlada pelo jogador
    private Array<Carro> carros; // lista de carros inimigos na tela

    // ==== ÁUDIO (música e efeitos) ====
    private Music faseMusic; // música de fundo da fase
    private Sound crashSound; // som de colisão (usado ao bater) - pode ser inicializado em outro lugar
    private Sound coinSound; // som tocado ao pegar uma moeda

    // ==== MOEDAS ====
    private Array<Moeda> moedas; // lista de moedas ativas na tela
    private float coinSpawnTimer; // cronômetro para decidir quando gerar a próxima moeda
    private float coinIntervalBase; // intervalo base entre spawns de moeda (por fase)
    private int moedasColetadas = 0; // contador de moedas que o jogador já pegou na fase

    // Número de faixas da pista (varia conforme a fase)
    private int laneCount;

    // ==== Sistema de velocidade global ====
    // A ideia é: LevelManager define uma velocidade base da fase,
    // e o jogador ainda pode multiplicar isso com "boost" (como se
    // pedalasse/acelerasse).
    private float worldSpeedBase; // velocidade base da fase (px/s), vinda do LevelManager
    private float worldSpeedBoost; // multiplicador de boost (1.0 = normal, > 1.0 = mais rápido)
    private float worldSpeed; // velocidade final usada em tudo (fundo, carros, moedas, distância)

    // Controle de tempo para spawn de carros
    private float spawnTimer;

    // Distância percorrida na fase (em "metros" do jogo)
    // É calculada com base na velocidade e no tempo.
    private float distancia;

    // Flag que indica se o jogo está pausado (menu de pausa aberto)
    private boolean isPaused;

    // Fator de margem da pista nas laterais (quanto "sobra" de borda sem faixa)
    private float insetFactor;

    // LevelManager é um controlador de dificuldade:
    // - define velocidade base do mundo
    // - define intervalo de spawn
    // - define probabilidade de double spawn etc.
    private LevelManager level;

    // Gerador de números aleatórios para spawn aleatório de faixas, moedas etc.
    private Random rng;

    // ==== IA de distribuição de carros ====
    // laneDwell guarda, para cada faixa, quanto tempo a moto fica naquela faixa.
    // Isso serve para a IA "perceber" que o jogador está abusando de uma faixa
    // e reagir, às vezes trazendo carros pra essa faixa.
    private float[] laneDwell;

    // Constantes usadas para controlar como o laneDwell cresce e diminui
    private final float DWELL_DECAY = 0.6f; // quão rápido o "peso" da faixa cai quando o jogador sai dela
    private final float DWELL_CAP = 6.0f; // valor máximo de acumulação de dwell

    // Controle específico para fase 1 com 2 faixas:
    // evita ficar spawnando SEMPRE na faixa oposta do jogador, o que seria injusto.
    private int f1OppositeStreak = 0; // quantas vezes seguidas spawnou na faixa oposta
    private final int F1_STREAK_CAP = 2; // limite antes da IA começar a corrigir isso
    private final float F1_DWELL_BIAS_S = 1.2f; // tempo de dwell mínimo para usar o viés pró-faixa do jogador

    // Controle para fases 2 e 3:
    // evita a situação de spawnar carros muito tempo longe do jogador,
    // deixando o jogo "fácil" e sem necessidade de desviar.
    private int f23NonPlayerStreak = 0; // quantas vezes seguidas spawnou longe do jogador
    private final int F23_STREAK_CAP = 3; // limite antes de forçar spawn mais próximo ao jogador

    // Força geral das preferências da IA ao escolher faixa no pickLaneWeighted()
    private final float BIAS_STRENGTH = 1.2f;

    // Fatores de queda (falloff) conforme a distância da faixa do jogador:
    // faixas mais próximas (dist = 0 ou 1) recebem mais peso que faixas muito
    // distantes.
    private final float ADJ_FALLOFF_NEAR = 1.0f; // mesma faixa do jogador
    private final float ADJ_FALLOFF_ADJ = 0.7f; // faixa adjacente ao jogador
    private final float ADJ_FALLOFF_FAR = 0.35f; // faixa mais distante

    // Controle para saber quanto tempo o jogador está na MESMA faixa (para IA
    // "cutucar")
    private int lastLane = -1; // última faixa em que a moto estava
    private float sameLaneTime = 0f; // há quanto tempo está na mesma faixa

    private final float STICK_THRESHOLD = 1.2f; // tempo mínimo para considerar "grudado" na faixa
    private final float STICK_COOLDOWN_SECS = 1.4f; // tempo mínimo entre uma "forçada" e outra
    private final float STICK_SAFE_FRONT_PX = 200f; // distância mínima à frente da moto para spawn seguro

    private float stickCooldown = 0f; // conta quanto falta para poder "forçar" outro spawn de stick

    // Controle de "finalização" da fase:
    // finishing = true quando o jogador alcança a meta de distância.
    private boolean finishing = false;
    private float finishTimer = 0f;

    // Flag para indicar se já limpamos todo o trânsito (carros + moedas) na fase de
    // finalização
    private boolean clearedTraffic = false;

    // Tempos usados na animação de finalização:
    // - FINISH_CLEAR_TIME: tempo escoando carros normalmente antes de limpar
    // - FINISH_ASCEND_TIME: tempo que a moto sobe sozinha para fora da tela
    private static final float FINISH_CLEAR_TIME = 0.7f;
    private static final float FINISH_ASCEND_TIME = 2.0f;

    // GlyphLayout é usado para medir largura/altura de textos — aqui principalmente
    // no menu de pausa
    private GlyphLayout layout = new GlyphLayout();

    // ==== CONSTANTES DO BOOST GLOBAL (ESPAÇO/PEDAL) ====
    // BOOST_STEP: cada apertada no ESPAÇO aumenta um pouco a velocidade
    // BOOST_DECAY: a cada frame, o boost vai diminuindo até voltar a 1.0
    // BOOST_MAX: limite máximo para não ficar absurdo
    private static final float BOOST_STEP = 0.10f; // antes 0.22f — agora sobe só 10% por toque
    private static final float BOOST_DECAY = 0.20f; // desacelera de forma um pouco mais natural
    private static final float BOOST_MAX = 1.35f; // limite reduzido para evitar exagero

    /**
     * Construtor da GameScreen.
     *
     * @param game            referência ao jogo principal (para troca de telas)
     * @param faseSelecionada fase desejada (ex: 1, 2 ou 3)
     *
     *                        Aqui eu já "clampo" a fase: se vier 0 ou 10, por
     *                        exemplo, transformo em [1..3].
     */
    public GameScreen(MotoRunnerGame game, int faseSelecionada) {
        this.game = game;
        this.fase = Math.max(1, Math.min(3, faseSelecionada));
    }

    /**
     * show()
     * ------
     * Chamado quando essa tela passa a ser a tela ATIVA.
     * É aqui que inicializamos todos os recursos da fase:
     * batch, fonte, entidades, level, áudio etc.
     */
    @Override
    public void show() {
        // Cria o SpriteBatch (pincel de desenho) e a fonte básica
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.setColor(Color.WHITE);

        // Inicializa as listas de carros e moedas
        carros = new Array<>();
        moedas = new Array<>();

        // Zera timers e estados de controle
        spawnTimer = 0f;
        coinSpawnTimer = 0f;
        distancia = 0f;
        isPaused = false;
        rng = new Random();
        finishing = false;
        finishTimer = 0f;
        clearedTraffic = false;

        // Inicializa texturas estáticas de Carro e Moeda (otimização para reusar entre
        // telas)
        Carro.initTextureIfNeeded();
        Moeda.initIfNeeded();

        // Configuração visual e de faixas por fase:
        // - background diferente
        // - quantidade de faixas
        // - fator de margem lateral
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

        // Cria a moto já posicionada em uma das faixas,
        // a partir da quantidade de faixas e da margem lateral
        moto = new Moto(laneCount, insetFactor);

        // Cria o gerenciador de level/dificuldade para a fase atual
        level = new LevelManager(fase);

        // Cria o vetor que controla quanto tempo o jogador passa em cada faixa
        laneDwell = new float[laneCount];

        // Define o intervalo base entre moedas dependendo da fase:
        // Fase 1 = moeda mais espaçada; fase 3 = um pouco mais frequente
        coinIntervalBase = (fase == 1 ? 1.2f : fase == 2 ? 1.0f : 0.9f);

        // Inicializa o LevelManager com dt 0 só para ele calcular a velocidade inicial
        level.update(0f);
        worldSpeedBase = level.worldSpeedPx();
        worldSpeedBoost = 1f; // começa sem boost extra (x1.0)
        worldSpeed = worldSpeedBase * worldSpeedBoost;

        // ==== ÁUDIO: música da fase ====
        try {
            faseMusic = Gdx.audio.newMusic(Gdx.files.internal("audio.mp3"));
            faseMusic.setLooping(true); // música em loop
            faseMusic.setVolume(0.6f); // volume moderado
            faseMusic.play(); // começa a tocar
        } catch (Exception e) {
            faseMusic = null; // se der erro ao carregar, apenas ignora
        }

        // ==== ÁUDIO: som de moeda ====
        try {
            coinSound = Gdx.audio.newSound(Gdx.files.internal("coin.mp3"));
        } catch (Exception e) {
            coinSound = null;
        }

        // OBS: crashSound não está sendo carregado aqui.
        // Ele pode ser carregado em outra parte do código (ou pode ser um TODO futuro).
    }

    /**
     * render(delta)
     * --------------
     * Chamado em TODO frame do jogo.
     * Aqui acontece:
     * - Atualização de lógica (se não pausado)
     * - Atualização de velocidade e boost
     * - Spawns de carros e moedas
     * - Detecção de colisão e fim de fase
     * - Desenho do fundo, entidades, HUD e menu de pausa
     * - Leitura de input (teclas) de pausa, menu, etc.
     *
     * @param delta tempo (em segundos) desde o último frame
     */
    @Override
    public void render(float delta) {
        // dt limitado a no máximo 1/60s para evitar saltos muito bruscos se o FPS cair
        float dt = Math.min(delta, 1f / 60f);

        // Atualizamos o gerenciador de level apenas se não estiver pausado
        // Ele controla progressão de dificuldade com o tempo da fase
        if (!isPaused) {
            level.update(dt);
        }

        // 1) Obtemos a velocidade BASE a partir do LevelManager
        worldSpeedBase = level.worldSpeedPx();

        // 2) Atualiza o BOOST global (apenas se não estiver pausado nem finalizando)
        if (!isPaused && !finishing) {
            // Quando o jogador aperta ESPAÇO, é como "pedalar" ou "acelerar":
            // aumentamos o multiplicador de velocidade
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                worldSpeedBoost += BOOST_STEP;
                if (worldSpeedBoost > BOOST_MAX) {
                    worldSpeedBoost = BOOST_MAX; // não passa do máximo
                }
            }

            // Mesmo se o jogador não apertar nada, o boost vai voltando aos poucos para 1.0
            worldSpeedBoost -= BOOST_DECAY * dt;
            if (worldSpeedBoost < 1f) {
                worldSpeedBoost = 1f;
            }
        }

        // 3) Calcula a velocidade FINAL: base * boost (essa é a que realmente se usa no
        // jogo)
        worldSpeed = worldSpeedBase * worldSpeedBoost;

        // Controle da música:
        // - pausa se o jogo está pausado
        // - retoma se não estiver pausado, não estiver finalizando e não estiver
        // tocando
        if (faseMusic != null) {
            if (isPaused && faseMusic.isPlaying()) {
                faseMusic.pause();
            } else if (!isPaused && !finishing && !faseMusic.isPlaying()) {
                faseMusic.play();
            }
        }

        // Limpa a tela com um fundo preto antes de desenhar qualquer coisa
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Atualiza o fundo (scroll da estrada) se o jogo não estiver pausado
        if (!isPaused) {
            background.setSpeed(worldSpeed);
            background.update(dt);
        }

        // ================= LÓGICA PRINCIPAL DO JOGO (quando não está finalizando nem
        // pausado) =================
        if (!finishing && !isPaused) {

            // Atualiza a moto, passando dt e velocidade do mundo
            // (a moto pode usar a velocidade para sincronizar animações se quiser)
            moto.update(dt, worldSpeed);

            // ---- Controle de dwell (tempo na mesma faixa) ----
            int laneNow = moto.getCurrentLaneIndex();
            if (laneNow == lastLane)
                sameLaneTime += dt; // continua na mesma faixa, aumenta contador
            else {
                sameLaneTime = 0f; // mudou de faixa, zera
                lastLane = laneNow;
            }

            // Para cada faixa:
            // - na faixa atual, aumentamos dwell
            // - nas outras faixas, fazemos o dwell decair com o tempo
            for (int i = 0; i < laneCount; i++) {
                if (i == laneNow)
                    laneDwell[i] = Math.min(DWELL_CAP, laneDwell[i] + dt);
                else
                    laneDwell[i] = Math.max(0f, laneDwell[i] - dt * DWELL_DECAY);
            }

            // Reduz o cooldown do sistema de "forçar spawn"
            if (stickCooldown > 0f)
                stickCooldown -= dt;

            // ---- Spawn de carros por tempo (ondas) ----
            spawnTimer += dt;
            if (spawnTimer >= level.spawnInterval()) {
                // Quando o timer atinge o intervalo, chamamos spawnWave(),
                // que tem toda a lógica de IA de trânsito.
                spawnWave();
                spawnTimer = 0f;
            }

            // Caso o jogador esteja muito tempo na mesma faixa, tentamos
            // criar um spawn específico para "desgrudar" ele (sem ser injusto).
            tryForceStickSpawn();

            // ---- Spawn de moedas ----
            coinSpawnTimer += dt;

            // Intervalo atual de moedas: começa em coinIntervalBase e diminui levemente
            // conforme a fase avança (tempo do LevelManager).
            float coinIntervalNow = Math.max(0.55f,
                    coinIntervalBase - 0.35f * clamp01(level.getTime() / 60f));
            if (coinSpawnTimer >= coinIntervalNow) {
                spawnCoin();
                coinSpawnTimer = 0f;
            }
        }

        // ================= Atualização de carros e moedas =================
        if (!isPaused) {
            // Atualiza cada carro com base na worldSpeed
            for (int i = carros.size - 1; i >= 0; i--) {
                Carro c = carros.get(i);
                c.update(dt, worldSpeed);

                // Remove carros que saíram totalmente da tela (otimização)
                if (c.getBounds().y + c.getBounds().height < 0)
                    carros.removeIndex(i);
            }

            // Atualiza cada moeda
            for (int i = moedas.size - 1; i >= 0; i--) {
                Moeda m = moedas.get(i);
                m.update(dt, worldSpeed);

                // Remove moedas que saíram da tela
                if (m.getBounds().y + m.getBounds().height < 0) {
                    moedas.removeIndex(i);
                }
            }
        }

        // ================= Coleta de moedas (apenas antes do fim de fase)
        // =================
        if (!finishing && !isPaused) {
            for (int i = moedas.size - 1; i >= 0; i--) {
                Moeda m = moedas.get(i);

                // Se a bounding box da moto encosta na da moeda, considera como coleta
                if (moto.getBounds().overlaps(m.getBounds())) {
                    moedas.removeIndex(i);
                    moedasColetadas++;

                    // Toca som de moeda, se carregado
                    if (coinSound != null) {
                        coinSound.play(0.7f);
                    }
                }
            }
        }

        // ================= Distância, fim de fase e colisões =================
        if (!isPaused) {
            // Distância cresce com a velocidade final e o tempo
            // (0.035f é um fator de conversão px → "metros do jogo")
            distancia += worldSpeed * dt * 0.035f;

            float meta = getGoalMetersForFase(fase); // meta de metros para essa fase

            // Quando atinge a meta e ainda não estava finalizando, entra no modo de
            // finalização
            if (!finishing && distancia >= meta) {
                finishing = true;
                finishTimer = 0f;
                clearedTraffic = false;
            }

            if (!finishing) {
                // ===== MODO NORMAL (antes de bater a meta): colisão leva ao GameOver =====
                for (Carro c : carros) {
                    if (moto.getBounds().overlaps(c.getBounds())) {
                        // Se tiver som de colisão, toca
                        if (crashSound != null) {
                            crashSound.play(0.9f);
                        }
                        // Para a música
                        if (faseMusic != null) {
                            faseMusic.stop();
                        }
                        // Troca para tela de GameOver passando distância e fase
                        game.setScreen(new GameOverScreen(game, distancia, fase));
                        dispose();
                        return;
                    }
                }
            } else {
                // ===== MODO FINALIZAÇÃO (depois de bater a meta) =====
                finishTimer += dt;

                // 1) Primeiro, deixa o trânsito descer por um pequeno tempo
                if (!clearedTraffic && finishTimer >= FINISH_CLEAR_TIME) {
                    // Depois disso, limpa tudo para a moto subir sozinha
                    carros.clear();
                    moedas.clear();
                    clearedTraffic = true;
                }

                // 2) A partir de certo momento, moto começa a subir sozinha (animação de
                // vitória)
                if (finishTimer >= FINISH_CLEAR_TIME) {
                    moto.getBounds().y += dt * 260f;
                }

                // 3) Quando termina o tempo de animação, troca para LevelCompleteScreen
                if (finishTimer >= FINISH_CLEAR_TIME + FINISH_ASCEND_TIME) {
                    if (faseMusic != null) {
                        faseMusic.stop();
                    }
                    game.setScreen(new LevelCompleteScreen(
                            game,
                            fase,
                            (int) distancia,
                            moedasColetadas));
                    dispose();
                    return;
                }
            }
        }

        // ================= DESENHO (renderização) =================
        batch.begin();

        // Desenha o fundo, as moedas, os carros e a moto
        background.draw(batch);
        for (Moeda m : moedas)
            m.draw(batch);
        for (Carro c : carros)
            c.draw(batch);
        moto.draw(batch);

        // HUD (informações da fase) aparece somente quando não está pausado
        if (!isPaused) {
            float h = Gdx.graphics.getHeight();
            float meta = getGoalMetersForFase(fase);

            font.draw(batch, "Fase: " + fase, 10, h - 10);
            font.draw(batch, "Distância: " + (int) distancia + " m (Meta: " + (int) meta + " m)",
                    10, h - 30);
            font.draw(batch, "Velocidade base: " + (int) worldSpeedBase + " px/s",
                    10, h - 50);
            font.draw(batch, "Boost: x" + String.format("%.2f", worldSpeedBoost),
                    10, h - 70);
            font.draw(batch, "Velocidade final: " + (int) worldSpeed + " px/s",
                    10, h - 90);
            font.draw(batch, "Moedas: " + moedasColetadas,
                    10, h - 110);
            font.draw(batch,
                    "ESPAÇO/PEDAL = Aceleração  |  P = Pausar  |  ESC = Menu",
                    10, h - 130);
        }

        // ===== Menu de pausa (overlay por cima do jogo) =====
        if (isPaused) {
            float screenW = Gdx.graphics.getWidth();
            float screenH = Gdx.graphics.getHeight();
            float centerX = screenW / 2f;

            // Título "PAUSADO" maior
            font.getData().setScale(1.8f);
            String title = "=== PAUSADO ===";
            layout.setText(font, title);
            font.draw(batch, title, centerX - layout.width / 2, screenH * 0.65f);

            // Opções do menu com uma escala um pouco menor
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

            // Garante que a cor volte a branco (caso seja alterada em outro lugar)
            font.setColor(Color.WHITE);
        }

        batch.end();

        // ================= INPUT (teclado) – fora do batch =================

        // Dentro do LibGDX é uma boa prática NÃO trocar de tela dentro do begin/end do
        // batch.

        if (isPaused) {
            // Quando o jogo está pausado, tratamos as teclas do menu de pausa
            if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
                // Retoma o jogo
                isPaused = false;
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                // Reinicia a mesma fase
                game.setScreen(new GameScreen(game, fase));
                dispose();
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                // Volta para o menu principal
                game.setScreen(new MenuScreen(game));
                dispose();
            }
        } else {
            // Quando NÃO está pausado:
            // Aqui não deixamos pausar se estiver na animação final (finishing)
            if (!finishing) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
                    // Entra em pausa
                    isPaused = true;
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                    // Volta direto pro menu principal
                    game.setScreen(new MenuScreen(game));
                    dispose();
                }
            }
        }
    }

    /**
     * getGoalMetersForFase
     * ---------------------
     * Função auxiliar que define quantos "metros" o jogador precisa percorrer
     * para completar cada fase.
     */
    private float getGoalMetersForFase(int f) {
        switch (f) {
            case 1:
                return 500f;
            case 2:
                return 1000f;
            case 3:
                return 1000f;
            default:
                return 1200f;
        }
    }

    // ===================== SPAWN / IA DE TRÂNSITO =====================

    /**
     * spawnWave()
     * -----------
     * Este é o método principal de IA de trânsito.
     *
     * Responsabilidades:
     * - Decide em quais faixas os novos carros vão aparecer
     * - Evita formar "paredões" impossíveis (sempre deixa rota de fuga)
     * - Considera uma "zona de perigo" à frente da moto para não empilhar
     * muitos carros ali de uma vez
     * - Usa dwell (tempo em cada faixa) para variar o comportamento por fase
     * - Tem regras diferentes para fase 1 (2 faixas) e fases 2/3 (3 ou 4 faixas)
     */
    private void spawnWave() {
        int screenH = Gdx.graphics.getHeight();
        float spawnYBase = screenH + 40f; // spawn logo acima da parte visível da tela

        // ===== 1) Identificar carros já na "zona de perigo" à frente da moto =====
        float motoTop = moto.getBounds().y + moto.getBounds().height;
        float dangerStart = motoTop + 80f; // início da zone de perigo
        float dangerEnd = motoTop + 420f; // fim da zona de perigo

        // laneBlocked[l] = true se a faixa l tem algum carro dentro da zona de perigo
        boolean[] laneBlocked = new boolean[laneCount];
        int blockedCount = 0;

        for (Carro c : carros) {
            float cyMid = c.getBounds().y + c.getBounds().height * 0.5f;
            // Se o meio do carro está dentro da zona de perigo:
            if (cyMid >= dangerStart && cyMid <= dangerEnd) {
                int ln = c.getLaneIndex();
                if (!laneBlocked[ln]) {
                    laneBlocked[ln] = true;
                    blockedCount++;
                }
            }
        }

        // Se TODAS as faixas já têm carro na zona de perigo, não é seguro spawnar nada.
        if (blockedCount >= laneCount) {
            return;
        }

        // Para cada faixa, calculamos a posição Y do carro mais alto (mais perto do
        // topo)
        float[] topYByLane = new float[laneCount];
        for (int l = 0; l < laneCount; l++)
            topYByLane[l] = Float.NEGATIVE_INFINITY;
        for (Carro c : carros)
            topYByLane[c.getLaneIndex()] = Math.max(topYByLane[c.getLaneIndex()], c.getBounds().y);

        // Espaçamento vertical mínimo entre carros em uma mesma faixa
        float minGap = level.laneGapPx();

        // Lista de faixas livres para spawn, respeitando o minGap
        List<Integer> livres = new ArrayList<>();
        for (int l = 0; l < laneCount; l++)
            if ((spawnYBase - topYByLane[l]) > minGap)
                livres.add(l);
        if (livres.isEmpty())
            return;

        // ===== 2) Evita formar "paredão" perto da moto =====
        // Situação: se quase todas as faixas já estão ocupadas na zona de perigo,
        // não queremos FECHAR a única faixa livre com spawn novo.
        // Então limitamos o spawn a faixas que já estão bloqueadas.
        if (blockedCount >= laneCount - 1) {
            List<Integer> filtered = new ArrayList<>();
            for (int l : livres) {
                if (laneBlocked[l]) {
                    filtered.add(l);
                }
            }
            if (!filtered.isEmpty()) {
                // agora só spawnamos em faixas já "ocupadas" na zona
                livres = filtered;
            }
            // Além disso, mais abaixo desativamos o doubleSpawn.
        }

        boolean isPhase1 = (fase == 1);
        boolean twoLanes = (laneCount == 2);
        int playerLane = moto.getCurrentLaneIndex();

        // ===== 2a) Escolha da primeira faixa (lane1) com regras especiais por fase
        // =====
        int lane1;
        if (isPhase1 && twoLanes) {
            // FASE 1, duas faixas:
            // Regras mais cuidadosas para não ficar injusto
            int opposite = 1 - playerLane; // faixa oposta à do jogador
            boolean playerLaneFree = livres.contains(playerLane);
            boolean dwellHigh = laneDwell[playerLane] >= F1_DWELL_BIAS_S;
            boolean blockOppStreak = (f1OppositeStreak >= F1_STREAK_CAP);

            // Se o jogador está muito tempo na mesma faixa OU já spawnamos demais do outro
            // lado,
            // favorecemos spawnar na faixa do jogador.
            if (playerLaneFree && (dwellHigh || blockOppStreak))
                lane1 = playerLane;
            else {
                // Caso contrário, há uma chance de 65% de spawnar na faixa do jogador
                if (playerLaneFree && rng.nextFloat() < 0.65f)
                    lane1 = playerLane;
                else
                    lane1 = livres.get(rng.nextInt(livres.size()));
            }

            // Atualiza contagem de quantas vezes seguidas spawnamos na faixa oposta
            if (lane1 == opposite)
                f1OppositeStreak++;
            else
                f1OppositeStreak = 0;

        } else {
            // FASES 2 e 3:
            // Usamos pickLaneWeighted, que considera dwell e proximidade ao jogador
            lane1 = pickLaneWeighted(livres, playerLane);

            // Se spawnar muito tempo longe do jogador, o jogo fica fácil demais,
            // então contamos esse "nonPlayerStreak"
            boolean nearPlayer = (Math.abs(lane1 - playerLane) <= 1);
            if (!nearPlayer)
                f23NonPlayerStreak++;
            else
                f23NonPlayerStreak = 0;

            // Se spawnou longe do jogador muitas vezes seguidas,
            // forçamos um spawn mais próximo dele (pra manter desafio constante)
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

        // Finalmente, cria o primeiro carro da onda
        spawnSingleAtLane(lane1, spawnYBase);
        // Remove essa faixa da lista de faixas livres (já usamos)
        livres.remove((Integer) lane1);

        // ===== 3) Regras do doubleSpawn (segundo carro na mesma onda) =====
        // Em geral, permitimos doubleSpawn, MENOS na fase 1 com 2 faixas,
        // pois isso aumenta muito o risco de injustiça.
        boolean allowDouble = !(isPhase1 && twoLanes);

        // Se já estamos quase em "paredão" na zona de perigo, desativamos o doubleSpawn
        if (blockedCount >= laneCount - 1) {
            allowDouble = false;
        }

        // doubleSpawn acontece com uma certa probabilidade vinda do LevelManager
        // (pDouble)
        boolean doubleSpawn = allowDouble &&
                rng.nextFloat() < level.pDouble() && !livres.isEmpty();
        if (doubleSpawn) {
            int lane2 = (fase >= 2)
                    ? pickLaneWeighted(livres, playerLane)
                    : livres.get(rng.nextInt(livres.size()));

            // jitter vertical: deslocamento para não ficar colado em y com o primeiro carro
            float jitter = 70f + rng.nextFloat() * 110f;
            float y2 = spawnYBase + jitter;
            float minDeltaY = 140f;
            if (Math.abs(jitter) < minDeltaY)
                y2 = spawnYBase + minDeltaY;

            // Se o segundo carro cair na faixa do jogador, garantimos uma distância mínima
            // na frente da moto para não spawnar em cima dela
            if (lane2 == playerLane) {
                float motoTop2 = moto.getBounds().y + moto.getBounds().height;
                float safeStart = motoTop2 + STICK_SAFE_FRONT_PX;
                if (y2 < safeStart)
                    y2 = safeStart;
            }
            spawnSingleAtLane(lane2, y2);
        }

        // Pequeno ajuste na fase 1 com 2 faixas:
        // antecipamos um pouco o próximo spawn para deixar o jogo mais dinâmico,
        // mas sem ser injusto por causa de todas as proteções acima.
        if (isPhase1 && twoLanes) {
            spawnTimer = -0.35f;
        }
    }

    /**
     * tryForceStickSpawn()
     * --------------------
     * Objetivo: se o jogador ficar "parado" demais na mesma faixa (sem trocar),
     * essa IA tenta criar um carro numa posição segura à frente dele para forçar
     * que ele desvie.
     *
     * Regras de segurança:
     * - só entra se sameLaneTime passar de STICK_THRESHOLD
     * - respeita um cooldown (stickCooldown) para não repetir o tempo todo
     * - nunca spawnar colado em cima da moto
     * - tenta considerar faixas adjacentes se a faixa atual estiver sem espaço
     */
    private void tryForceStickSpawn() {
        // Condições para ativar:
        // - jogador ficou tempo suficiente na mesma faixa
        // - já passou o cooldown desde a última forçada
        if (sameLaneTime < STICK_THRESHOLD || stickCooldown > 0f)
            return;

        int targetLane = moto.getCurrentLaneIndex();
        int screenH = Gdx.graphics.getHeight();
        float spawnYBase = screenH + 40f;

        // Calcula o carro mais alto em cada faixa
        float[] topYByLane = new float[laneCount];
        for (int l = 0; l < laneCount; l++)
            topYByLane[l] = Float.NEGATIVE_INFINITY;
        for (Carro c : carros)
            topYByLane[c.getLaneIndex()] = Math.max(topYByLane[c.getLaneIndex()], c.getBounds().y);

        float minGap = level.laneGapPx();

        // Primeiro tentamos spawnar na faixa atual do jogador
        int chosenLane = targetLane;

        // Se não há espaço vertical suficiente nessa faixa, procuramos uma faixa
        // adjacente
        if ((spawnYBase - topYByLane[targetLane]) <= minGap) {
            int alt = findAdjacentFreeLane(spawnYBase, topYByLane, minGap, targetLane);
            if (alt != -1)
                chosenLane = alt;
            else
                return; // nenhuma faixa adjacente segura, então desistimos
        }

        // Calculamos uma posição Y segura: pelo menos STICK_SAFE_FRONT_PX à frente da
        // moto
        float motoTop = moto.getBounds().y + moto.getBounds().height;
        float y = Math.max(spawnYBase, motoTop + STICK_SAFE_FRONT_PX);

        // Ajuste especial para fase 1 com 2 faixas:
        // se a outra faixa tem um carro muito perto, aumentamos a distância.
        if (fase == 1 && laneCount == 2) {
            int other = 1 - chosenLane;
            float otherTop = topYByLane[other];
            if (otherTop > 0 && (y - otherTop) < 160f)
                y = otherTop + 180f;
        }

        // Faz o spawn do carro "despertador"
        spawnSingleAtLane(chosenLane, y);
        sameLaneTime = 0f; // zera o tempo grudado
        stickCooldown = STICK_COOLDOWN_SECS; // inicia o cooldown
    }

    /**
     * findAdjacentFreeLane()
     * ----------------------
     * Procura uma faixa próxima (para a esquerda ou direita) da "center"
     * que tenha espaço suficiente para spawnar (respeitando minGap).
     *
     * Retorna o índice da faixa ou -1 se não encontrar nenhuma.
     */
    private int findAdjacentFreeLane(float spawnYBase, float[] topYByLane,
            float minGap, int center) {
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

    /**
     * spawnCoin()
     * -----------
     * Responsável por spawnar uma moeda em uma faixa:
     * - evita spawnar colada em carros
     * - prefere faixas perto do jogador
     * - mantém um espaçamento vertical mínimo
     */
    private void spawnCoin() {
        int screenH = Gdx.graphics.getHeight();
        float spawnYBase = screenH + 40f;

        // Calcula o carro mais alto em cada faixa
        float[] topYByLane = new float[laneCount];
        for (int l = 0; l < laneCount; l++)
            topYByLane[l] = Float.NEGATIVE_INFINITY;
        for (Carro c : carros)
            topYByLane[c.getLaneIndex()] = Math.max(topYByLane[c.getLaneIndex()], c.getBounds().y);

        // Gap mínimo para moedas (um pouco mais flexível que carro)
        float minGapCoin = Math.max(120f, level.laneGapPx() * 0.65f);

        // Lista de faixas com espaço para moeda
        List<Integer> livres = new ArrayList<>();
        for (int l = 0; l < laneCount; l++)
            if ((spawnYBase - topYByLane[l]) > minGapCoin)
                livres.add(l);
        if (livres.isEmpty())
            return;

        int playerLane = moto.getCurrentLaneIndex();

        // Escolha da faixa da moeda:
        // 60% de chance de cair na faixa do jogador (se estiver livre),
        // 30% de chance de cair em faixas próximas,
        // 10% de chance de cair em qualquer outra livre.
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

        // Ajuste extra de segurança em relação ao carro da mesma faixa
        float extraSafe = 60f;
        if ((spawnYBase - topYByLane[lane]) < (minGapCoin + extraSafe)) {
            // Tenta mudar para uma faixa perto que tenha espaço extra
            for (int l : near) {
                if ((spawnYBase - topYByLane[l]) > (minGapCoin + extraSafe)) {
                    lane = l;
                    break;
                }
            }
        }

        // Calcula os centros de cada faixa (posição X) e spawna uma moeda na faixa
        // selecionada
        float[] centers = computeLaneCenters(laneCount, insetFactor);
        moedas.add(new Moeda(centers, lane, spawnYBase));
    }

    /**
     * computeLaneCenters()
     * --------------------
     * Retorna um array com as posições X centrais de cada faixa da pista,
     * levando em conta:
     * - a largura total da tela
     * - a margem lateral (insetFactor)
     * - número de faixas
     */
    private float[] computeLaneCenters(int laneCount, float inset) {
        float screenWidth = Gdx.graphics.getWidth();
        float margem = screenWidth * inset;
        float larguraPista = screenWidth - (margem * 2);

        switch (laneCount) {
            case 4: {
                // Para 4 faixas, usamos frações fixas da largura da pista
                float[] frac = new float[] { 1f / 8f, 3f / 8f, 5f / 8f, 7f / 8f };
                return new float[] {
                        margem + larguraPista * frac[0],
                        margem + larguraPista * frac[1],
                        margem + larguraPista * frac[2],
                        margem + larguraPista * frac[3]
                };
            }
            case 3: {
                // Para 3 faixas, dividimos a pista em 4 partes e usamos os 3 pontos internos
                float esp = larguraPista / 4f;
                return new float[] {
                        margem + esp * 1f,
                        margem + esp * 2f,
                        margem + esp * 3f
                };
            }
            default: {
                // Para 2 faixas, dividimos em 3 partes e usamos os 2 pontos internos
                float esp = larguraPista / 3f;
                return new float[] {
                        margem + esp * 1f,
                        margem + esp * 2f
                };
            }
        }
    }

    /**
     * pickLaneWeighted()
     * -------------------
     * Dado um conjunto de faixas candidatas e a faixa do jogador,
     * escolhe UMA faixa aleatoriamente, mas aplicando PESOS.
     *
     * O peso leva em conta:
     * - dwell (quanto tempo o jogador ficou naquela faixa) → evita monotonia
     * - quão perto a faixa está da faixa do jogador → dá mais relevância
     */
    private int pickLaneWeighted(List<Integer> candidates, int playerLane) {
        // Se só tem uma candidata, não precisa sortear
        if (candidates.size() == 1)
            return candidates.get(0);

        float total = 0f;
        float[] weights = new float[candidates.size()];

        // Calcula o peso de cada faixa candidata
        for (int i = 0; i < candidates.size(); i++) {
            int lane = candidates.get(i);

            // dwell normalizado entre 0 e 1
            float dwellNorm = (DWELL_CAP <= 0f)
                    ? 0f
                    : Math.min(1f, laneDwell[lane] / DWELL_CAP);

            // Distância em número de faixas até o jogador
            int dist = Math.abs(lane - playerLane);

            // Quanto mais perto do jogador, maior é o falloff
            float falloff = (dist == 0) ? ADJ_FALLOFF_NEAR
                    : (dist == 1) ? ADJ_FALLOFF_ADJ
                            : ADJ_FALLOFF_FAR;

            // Peso base 1.0 + componente dependente do dwell e da proximidade
            float w = 1.0f + BIAS_STRENGTH * dwellNorm * falloff;
            weights[i] = w;
            total += w;
        }

        // Sorteio proporcional ao peso:
        // gera um número de 0 até total e anda acumulando até encontrar a faixa
        // correspondente
        float r = rng.nextFloat() * total;
        float acc = 0f;
        for (int i = 0; i < weights.length; i++) {
            acc += weights[i];
            if (r <= acc)
                return candidates.get(i);
        }

        // Fallback (caso a soma de floats dê algum problema de arredondamento)
        return candidates.get(candidates.size() - 1);
    }

    /**
     * spawnSingleAtLane()
     * --------------------
     * Spawna um único carro em uma faixa específica, em uma altura específica.
     *
     * A velocidade do carro é baseada na velocidade do mundo multiplicada por um
     * fator
     * de LevelManager (rivalSpeedFactor).
     */
    // ================== CONTROLE DE VELOCIDADE DOS CARROS POR FASE ==================
private void spawnSingleAtLane(int lane, float spawnY) {
    // velocidade base do carro (relacionada à velocidade do mundo)
    float vCarBase = worldSpeed * level.rivalSpeedFactor();

    // fator de redução conforme a fase
    float slowFactor;
    if (fase == 1) {
        slowFactor = 1.0f;   // Fase 1 mantém a mesma velocidade (já está equilibrada)
    } else if (fase == 2) {
        slowFactor = 0.7f;   // Fase 2 → carros 30% mais lentos
    } else {
        slowFactor = 0.55f;  // Fase 3 → carros 45% mais lentos
    }

    // aplica a redução final
    float vCar = vCarBase * slowFactor;

    // cria o carro com a velocidade ajustada
    carros.add(new Carro(lane, spawnY, vCar, laneCount, insetFactor));
}

    /**
     * clamp01()
     * ---------
     * Função utilitária: limita um valor entre 0 e 1.
     */
    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    // =================== Métodos padrão da interface Screen ===================

    @Override
    public void resize(int width, int height) {
        // Quando o tamanho da tela muda (ex.: janela redimensionada),
        // ajustamos o background para se reposicionar/calcule novamente se necessário.
        if (background != null)
            background.onResize();
    }

    @Override
    public void pause() {
        // Chamado quando o app é pausado pelo sistema (ex.: minimizar, foco perdido)
        // Aqui marcamos o estado de pausa do jogo.
        isPaused = true;
    }

    @Override
    public void resume() {
        // Chamado quando o app volta do pause do sistema.
        // Poderíamos retomar algo aqui se fosse necessário.
    }

    @Override
    public void hide() {
        // Chamado quando outra tela passa a ser exibida no lugar desta.
        // A limpeza pesada de recursos é feita no dispose().
    }

    /**
     * dispose()
     * ----------
     * Responsável por liberar todos os recursos (memória de GPU/CPU) usados por
     * esta tela.
     * É chamado quando essa tela não será mais usada.
     */
    @Override
    public void dispose() {
        if (batch != null)
            batch.dispose();
        if (font != null)
            font.dispose();
        if (moto != null)
            moto.dispose();
        if (background != null)
            background.dispose();

        // Libera texturas estáticas compartilhadas de Carro/Moeda
        Carro.disposeStatic();
        Moeda.disposeStatic();

        // Libera recursos de áudio
        if (faseMusic != null) {
            faseMusic.dispose();
        }
        if (crashSound != null) {
            crashSound.dispose();
        }
        if (coinSound != null) {
            coinSound.dispose();
        }
    }
}
