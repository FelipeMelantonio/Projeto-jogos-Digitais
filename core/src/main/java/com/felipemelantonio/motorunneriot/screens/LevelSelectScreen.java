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

/**
 * LevelSelectScreen
 * -----------------
 * Tela onde o jogador escolhe qual fase quer jogar (1, 2 ou 3).
 *
 * Responsabilidades principais:
 * - Mostrar um fundo animado (mesmo estilo do jogo)
 * - Mostrar a moto andando como elemento visual
 * - Desenhar o título e 3 "botões" de fase (imagens Nivel1, Nivel2, Nivel3)
 * - Calcular as posições e tamanhos dos botões de forma RESPONSIVA
 * (tudo baseado na largura/altura da tela)
 * - Detectar clique do mouse / toque em cada botão
 * - Ler teclas 1/2/3, ENTER e ESC para navegação rápida
 */
public class LevelSelectScreen implements Screen {

    // Referência para o jogo principal, usada para trocar de telas.
    private final MotoRunnerGame game;

    // Ferramentas básicas de renderização.
    private SpriteBatch batch; // "pincel" para desenhar texturas e textos
    private BitmapFont hint; // fonte usada para o texto de dica no rodapé
    private Background bg; // fundo animado (estrada)
    private Moto moto; // moto aparecendo como animação na tela de seleção

    // Texturas dos elementos visuais desta tela.
    private Texture texTitulo; // imagem "Selecionar" (título no topo)
    private Texture texN1; // botão visual do Nível 1
    private Texture texN2; // botão visual do Nível 2
    private Texture texN3; // botão visual do Nível 3

    // Retângulos que representam a área clicável (hitbox) de cada botão.
    // Não são desenhados, só usados para detecção de clique/toque.
    private final Rectangle rN1 = new Rectangle();
    private final Rectangle rN2 = new Rectangle();
    private final Rectangle rN3 = new Rectangle();

    // ===== Constantes de layout (definidas como proporções pra serem responsivas)
    // =====

    // Margem do topo para o título.
    // Exemplo: 0.11f significa que o título fica a ~11% da altura da tela a partir
    // do topo.
    private static final float TOP_MARGIN = 0.11f;

    // Proporção da largura do título em relação à largura da tela.
    // Ex.: 0.40f → título com 40% da largura da tela.
    private static final float TITLE_WIDTH_RATIO = 0.40f;

    // Proporção da largura dos botões em relação à largura da tela.
    // Ex.: 0.20f → cada botão tem 20% da largura da tela.
    private static final float BTN_WIDTH_RATIO = 0.20f;

    // >>> Proporção fixa dos botões, baseada na resolução original da imagem
    // (568x187).
    // Isso garante que, independente da tela, o botão mantenha o mesmo aspecto (sem
    // deformar).
    private static final float BTN_REF_W = 568f;
    private static final float BTN_REF_H = 187f;
    private static final float BTN_ASPECT = BTN_REF_H / BTN_REF_W; // ~0.329

    // Espaço vertical entre os botões:
    // definido como 50% da ALTURA do botão → fica proporcional.
    private static final float BTN_SPACE_RATIO_H = 0.50f;

    // Distância fixa em pixels entre o título e o primeiro botão.
    private static final float GAP_TITLE_BTNS_PX = 24f;

    // Velocidade da estrada/mundo nessa tela de seleção (apenas visual).
    private float roadSpeed = 280f;

    /**
     * Construtor da tela de seleção de fases.
     *
     * @param game referência ao jogo principal
     */
    public LevelSelectScreen(MotoRunnerGame game) {
        this.game = game;
    }

    /**
     * show()
     * ------
     * Chamado quando essa tela se torna a tela ativa.
     * Aqui inicializamos todas as texturas, fontes e entidades usadas na seleção.
     */
    @Override
    public void show() {
        batch = new SpriteBatch();

        // Fonte para a dica ("1/2/3 navegar ...") no rodapé.
        hint = new BitmapFont();
        hint.setColor(new Color(1, 1, 1, 0.9f)); // branco com leve transparência

        // Cria o fundo usando a arte da fase 2 e uma velocidade constante.
        bg = new Background("fase2.png", roadSpeed);

        // Cria a moto só para efeitos visuais na tela de seleção.
        // Usa 3 faixas e o mesmo inset da fase 2.
        moto = new Moto(3, 0.22f);
        // Desabilita os controles — aqui o jogador não controla a moto, ela só anima.
        moto.setControlsEnabled(false);

        // Carrega as texturas do título e dos botões de nível.
        texTitulo = new Texture("Selecionar.png");
        texN1 = new Texture("Nivel1.png");
        texN2 = new Texture("Nivel2.png");
        texN3 = new Texture("Nivel3.png");

        // Define filtros LINEAR nas texturas para ficarem mais suaves ao redimensionar.
        texTitulo.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        texN1.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        texN2.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        texN3.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        // Calcula posições e tamanhos iniciais dos botões (responsivo).
        layout();
    }

    /**
     * layout()
     * --------
     * Responsável por calcular as posições e medidas do TÍTULO e dos 3 BOTÕES de
     * fase,
     * sempre baseando tudo no tamanho atual da tela.
     *
     * Objetivos:
     * - Título centralizado no topo
     * - Botões empilhados verticalmente logo abaixo do título
     * - TODOS os botões com o MESMO tamanho e MESMA proporção (sem deformar)
     * - Retângulos rN1 / rN2 / rN3 usados como hitbox para clique/toque.
     */
    private void layout() {
        float W = Gdx.graphics.getWidth();
        float H = Gdx.graphics.getHeight();

        // === TÍTULO ===
        // Largura = fração da largura da tela
        float titleW = W * TITLE_WIDTH_RATIO;
        // Altura proporcional à textura (mantendo proporção original da imagem)
        float titleH = titleW * texTitulo.getHeight() / texTitulo.getWidth();
        // Centralizado em X
        float titleX = (W - titleW) * 0.5f;
        // Posição em Y considerando a margem do topo
        float titleY = H * (1f - TOP_MARGIN) - titleH;

        // === BOTÕES DE FASE (com proporção fixa 568x187) ===
        float btnW = W * BTN_WIDTH_RATIO; // largura como % da tela
        float btnH = btnW * BTN_ASPECT; // altura mantida via proporção fixa
        float space = btnH * BTN_SPACE_RATIO_H; // espaço vertical entre os botões

        // Centraliza os botões em X
        float btnX = (W - btnW) * 0.5f;

        // Empilha os botões logo abaixo do título, respeitando gap e espaçamentos
        float n1Y = titleY - GAP_TITLE_BTNS_PX - btnH;
        float n2Y = n1Y - space - btnH;
        float n3Y = n2Y - space - btnH;

        // Define as hitboxes retangulares (usadas para detectar clique nas imagens).
        rN1.set(btnX, n1Y, btnW, btnH);
        rN2.set(btnX, n2Y, btnW, btnH);
        rN3.set(btnX, n3Y, btnW, btnH);
    }

    /**
     * render(delta)
     * --------------
     * Chamado em todo frame.
     *
     * Aqui fazemos:
     * - Atualização da animação de fundo e da moto
     * - Leitura de input (mouse/teclado)
     * - Desenho do fundo, da moto, do título e dos botões
     * - Desenho da dica de controle no rodapé
     */
    @Override
    public void render(float delta) {
        // Limita dt para evitar steps muito grandes se o FPS cair.
        float dt = Math.min(delta, 1f / 60f);

        // Atualiza velocidade e movimento do fundo e da moto
        bg.setSpeed(roadSpeed);
        bg.update(dt);
        moto.update(dt, roadSpeed);

        // Lê input de mouse/teclado (troca de fase, ESC, etc.)
        handleInput();

        // Limpa a tela antes de desenhar
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float W = Gdx.graphics.getWidth();
        float H = Gdx.graphics.getHeight();

        // Recalcula medidas do título para desenhar (mesma lógica usada em layout())
        float titleW = W * TITLE_WIDTH_RATIO;
        float titleH = titleW * texTitulo.getHeight() / texTitulo.getWidth();
        float titleX = (W - titleW) * 0.5f;
        float titleY = H * (1f - TOP_MARGIN) - titleH;

        batch.begin();

        // Desenha o fundo animado
        bg.draw(batch);

        // Desenha a moto (apenas como elemento visual decorativo)
        moto.draw(batch);

        // Desenha o título "Selecionar"
        batch.draw(texTitulo, titleX, titleY, titleW, titleH);

        // Desenha os 3 botões usando exatamente as dimensões das hitboxes
        // (isso garante que a área clicável bate com a área visual).
        batch.draw(texN1, rN1.x, rN1.y, rN1.width, rN1.height);
        batch.draw(texN2, rN2.x, rN2.y, rN2.width, rN2.height);
        batch.draw(texN3, rN3.x, rN3.y, rN3.width, rN3.height);

        // Desenha a dica de controles no rodapé
        hint.getData().setScale(1.0f);
        hint.draw(batch,
                "1/2/3 navegar   ENTER iniciar   ESC voltar",
                (W - 460) / 2f,
                H * 0.075f);

        batch.end();
    }

    /**
     * handleInput()
     * -------------
     * Trata toda a entrada do jogador nessa tela:
     * - clique / toque do mouse em cima dos botões
     * - teclas 1, 2, 3 para escolher fase
     * - ESC para voltar ao menu principal
     */
    private void handleInput() {
        // Clique ou toque na tela
        if (Gdx.input.justTouched()) {
            // Posição do mouse em coordenadas de janela
            float mx = Gdx.input.getX();
            // LibGDX usa origem no canto inferior esquerdo,
            // então precisamos inverter o Y (H - getY()).
            float my = Gdx.graphics.getHeight() - Gdx.input.getY();

            // Verifica se o clique/touch caiu dentro de alguma hitbox
            if (rN1.contains(mx, my)) {
                game.setScreen(new GameScreen(game, 1));
                return;
            }
            if (rN2.contains(mx, my)) {
                game.setScreen(new GameScreen(game, 2));
                return;
            }
            if (rN3.contains(mx, my)) {
                game.setScreen(new GameScreen(game, 3));
                return;
            }
        }

        // Navegação por teclado: números 1, 2, 3
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1))
            game.setScreen(new GameScreen(game, 1));
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2))
            game.setScreen(new GameScreen(game, 2));
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3))
            game.setScreen(new GameScreen(game, 3));

        // ESC volta para a tela de menu principal
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            game.setScreen(new MenuScreen(game));
    }

    /**
     * resize(width, height)
     * ----------------------
     * Chamado quando a janela/tela é redimensionada.
     *
     * Aqui reaplicamos o layout() para recalcular posições e tamanhos
     * dos botões e do título com base no novo tamanho de tela.
     * Também informamos o Background para ele se adaptar.
     */
    @Override
    public void resize(int width, int height) {
        layout(); // recalcule posições dos botões
        if (bg != null)
            bg.onResize(); // ajusta o fundo, se necessário
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

    /**
     * dispose()
     * ----------
     * Libera todos os recursos gráficos alocados por esta tela.
     * É chamado quando a tela não será mais usada.
     */
    @Override
    public void dispose() {
        if (batch != null)
            batch.dispose();
        if (hint != null)
            hint.dispose();
        if (bg != null)
            bg.dispose();
        if (moto != null)
            moto.dispose();
        if (texTitulo != null)
            texTitulo.dispose();
        if (texN1 != null)
            texN1.dispose();
        if (texN2 != null)
            texN2.dispose();
        if (texN3 != null)
            texN3.dispose();
    }
}
