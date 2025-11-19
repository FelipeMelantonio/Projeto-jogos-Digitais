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
 * MenuScreen
 * ==========
 * Tela inicial do jogo.
 *
 * Aqui eu:
 * - desenho o fundo animado (estrada)
 * - desenho uma moto andando no fundo só pra dar vida
 * - desenho o logo e os botões "Jogar" e "Sair"
 * - trato o input do mouse/teclado para trocar de tela ou sair do jogo
 *
 * Essa classe IMPLEMENTA a interface Screen do LibGDX, então o ciclo de vida é:
 * - show() -> chamado quando essa tela vira a tela ativa
 * - render() -> chamado TODO FRAME (é o “batimento” da tela)
 * - resize() -> chamado se a janela/tela mudar de tamanho
 * - pause() -> se o aplicativo for pausado (alt+tab, minimizar, etc.)
 * - resume()
 * - hide() -> quando outra tela entra no lugar
 * - dispose() -> liberar memória (texturas, fonte, etc.)
 */
public class MenuScreen implements Screen {

    // Referência pro jogo principal (MotoRunnerGame).
    // Eu uso isso pra trocar de tela: game.setScreen(new LevelSelectScreen(game));
    private final MotoRunnerGame game;

    // Objetos usados para desenhar na tela:
    // - SpriteBatch: "pincel" que desenha sprites/texturas
    // - BitmapFont: fonte para desenhar textos
    private SpriteBatch batch;
    private BitmapFont font; // fonte maior (texto do logo fallback)
    private BitmapFont small; // fonte menor (texto de dica embaixo)

    // Fundo animado da pista + moto que fica andando no menu (só visual)
    private Background bg;
    private Moto moto;

    // Texturas da interface do menu
    private Texture texLogo; // imagem do logo (MotoRunnerIoT)
    private Texture texJogar; // botão "Jogar"
    private Texture texSair; // botão "Sair"

    // Retângulos que representam a ÁREA CLICÁVEL dos botões.
    // Eles são usados:
    // - para desenhar os botões (posição e tamanho)
    // - para detectar se o clique do mouse caiu dentro do botão
    private final Rectangle rJogar = new Rectangle();
    private final Rectangle rSair = new Rectangle();

    // Velocidade usada para animar o fundo e a moto no menu
    private float roadSpeed = 280f;

    // Posição X/Y e tamanho (largura/altura) do logo na tela.
    // São calculados no método layout().
    private float _logoX, _logoY, _logoW, _logoH;

    // Parâmetros de layout (responsivo, baseado em proporção da tela).
    // Eu uso esses valores para que o menu fique “bonito” em qualquer resolução.
    private static final float TOP_MARGIN_RATIO = 0.08f; // margem do topo até o logo (8% da altura da tela)
    private static final float TITLE_WIDTH_RATIO = 0.68f; // logo usa ~68% da largura da tela
    private static final float BTN_WIDTH_RATIO = 0.28f; // botões usam ~28% da largura da tela
    private static final float BTN_GAP_RATIO = 0.40f; // espaço vertical entre botões = 40% da ALTURA do botão
    private static final float BOTTOM_MARGIN_RATIO = 0.10f; // margem inferior de 10%
    private static final float HINT_Y_RATIO = 0.05f; // altura do texto de dica (5% da altura da tela)

    // Construtor recebe o "game" para poder trocar de telas depois
    public MenuScreen(MotoRunnerGame game) {
        this.game = game;
    }

    /**
     * show()
     * ------
     * Chamado UMA VEZ quando essa tela vira a tela atual.
     * Aqui eu instancio todos os objetos que preciso para desenhar o menu.
     */
    @Override
    public void show() {
        // Cria o SpriteBatch e as fontes
        batch = new SpriteBatch();
        font = new BitmapFont();
        small = new BitmapFont();

        // Ajusta a cor da fonte
        font.setColor(Color.WHITE);
        small.setColor(new Color(1, 1, 1, 0.85f)); // branco um pouco transparente

        // Cria o fundo da fase 2 só pra ficar rodando no menu
        bg = new Background("fase2.png", roadSpeed);

        // Cria a moto em uma pista de 3 faixas, com margem lateral 0.22
        moto = new Moto(3, 0.22f);
        // No menu não quero o jogador controlando, então desabilito os controles
        moto.setControlsEnabled(false);

        // Carrega as imagens da interface.
        // Se o arquivo não existir, o método loadOrNull() devolve null.
        texLogo = loadOrNull("Logo.png");
        texJogar = loadOrNull("Jogar.png");
        texSair = loadOrNull("Sair.png");

        // Aplica filtro Linear para as texturas ficarem suaves quando escaladas
        if (texLogo != null)
            texLogo.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        if (texJogar != null)
            texJogar.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        if (texSair != null)
            texSair.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        // Calcula posição e tamanho do logo e dos botões com base no tamanho da tela
        layout();
    }

    /**
     * loadOrNull(path)
     * ----------------
     * Tenta carregar uma textura do arquivo indicado.
     * - Se o arquivo existe → retorna new Texture(path)
     * - Se NÃO existe ou der erro → retorna null
     *
     * Isso evita o jogo CRASHAR se esquecer de copiar uma imagem para o assets.
     */
    private Texture loadOrNull(String path) {
        try {
            if (!Gdx.files.internal(path).exists())
                return null;
            return new Texture(path);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * layout()
     * --------
     * Responsável por montar o "layout" do menu:
     * - define a largura/altura do logo e dos botões
     * - centraliza tudo horizontalmente
     * - distribui verticalmente dentro da área útil da tela (entre topo e rodapé)
     *
     * A ideia é ficar responsivo: trocou a resolução, chama layout() de novo.
     */
    private void layout() {
        final float W = Gdx.graphics.getWidth();
        final float H = Gdx.graphics.getHeight();

        // Calcula margens reais em pixels
        final float TOP_MARGIN = H * TOP_MARGIN_RATIO;
        final float BOTTOM_MARGIN = H * BOTTOM_MARGIN_RATIO;

        // Largura alvo do logo e dos botões usando porcentagem da largura da tela
        float logoW = W * 0.60f; // uso 60% aqui, um pouco menor que o TITLE_WIDTH_RATIO
        float btnW = W * 0.26f; // 26% da largura para cada botão

        // Altura do logo respeitando a proporção original da imagem
        float logoH = (texLogo != null)
                ? logoW * texLogo.getHeight() / texLogo.getWidth()
                : H * 0.12f; // se não tiver imagem, usa um valor padrão

        // Calcula a proporção do botão usando a textura do botão Jogar
        float btnAspect = (texJogar != null)
                ? (float) texJogar.getHeight() / (float) texJogar.getWidth()
                : 0.24f; // fallback se não tiver textura
        float btnH = btnW * btnAspect; // altura real do botão

        // Espaçamento vertical entre logo <-> Jogar e Jogar <-> Sair
        float SP = H * 0.035f;

        // Altura total ocupada por logo + espaçamentos + 2 botões
        float total = logoH + SP + btnH + SP + btnH;

        // Altura disponível sem contar margens superior/inferior
        float available = H - TOP_MARGIN - BOTTOM_MARGIN;

        // Se NÃO couber, reduzo tudo proporcionalmente (logo, botões e espaçamento)
        if (total > available) {
            float f = available / total; // fator de escala
            logoW *= f;
            logoH *= f;
            btnW *= f;
            btnH *= f;
            SP *= f;

            // recalcula a altura total com os valores reduzidos
            total = logoH + SP + btnH + SP + btnH;
        }

        // Posição Y da base do bloco (de baixo pra cima, onde fica o botão Sair)
        // Centralizo o bloco todo dentro da área útil (entre TOP e BOTTOM).
        float blockBottom = BOTTOM_MARGIN + (available - total) * 0.5f;

        // Y de cada elemento:
        float ySair = blockBottom; // botão Sair na base
        float yJogar = ySair + btnH + SP; // botão Jogar em cima do Sair
        float yLogo = yJogar + btnH + SP; // logo acima do botão Jogar

        // Centraliza tudo no eixo X
        float logoX = (W - logoW) * 0.5f;
        float btnX = (W - btnW) * 0.5f;

        // Salva as infos do logo para usar no render()
        _logoX = logoX;
        _logoY = yLogo;
        _logoW = logoW;
        _logoH = logoH;

        // Define as áreas clicáveis dos botões (usadas para clique e para desenhar)
        rJogar.set(btnX, yJogar, btnW, btnH);
        rSair.set(btnX, ySair, btnW, btnH);
    }

    /**
     * render(delta)
     * -------------
     * É o "batimento" da tela. O LibGDX chama esse método TODO FRAME.
     * Aqui é onde eu:
     * - atualizo animações (fundo e moto)
     * - leio input
     * - limpo a tela
     * - desenho fundo, moto, logo, botões e textos
     */
    @Override
    public void render(float delta) {
        // Limito o dt para no máximo 1/60s pra evitar pulos grandes se FPS cair
        float dt = Math.min(delta, 1f / 60f);

        // Atualiza animação do fundo e da moto
        bg.setSpeed(roadSpeed);
        bg.update(dt);
        moto.update(dt, roadSpeed);

        // Processa teclas e cliques do usuário
        handleInput();

        // Limpa a tela com preto
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Começa o desenho
        batch.begin();

        // Fundo + moto (camada de trás)
        bg.draw(batch);
        moto.draw(batch);

        // Logo: se existe textura, desenha; se não, escreve um texto grande
        if (texLogo != null) {
            batch.draw(texLogo, _logoX, _logoY, _logoW, _logoH);
        } else {
            float W = Gdx.graphics.getWidth();
            font.getData().setScale(2.0f);
            font.draw(batch,
                    "MOTO RUNNER IoT",
                    W * 0.5f - 260f, // centralizado aproximadamente
                    _logoY + _logoH * 0.75f);
        }

        // Desenha os botões exatamente na área dos retângulos rJogar/rSair
        if (texJogar != null)
            batch.draw(texJogar, rJogar.x, rJogar.y, rJogar.width, rJogar.height);
        if (texSair != null)
            batch.draw(texSair, rSair.x, rSair.y, rSair.width, rSair.height);

        // Texto de dica no rodapé (ENTER para jogar, ESC para sair)
        small.getData().setScale(1.1f);
        small.draw(batch,
                "ENTER para JOGAR  •  ESC para SAIR",
                25,
                Gdx.graphics.getHeight() * HINT_Y_RATIO);

        // Termina o desenho
        batch.end();
    }

    /**
     * handleInput()
     * -------------
     * Lê o mouse/toque e o teclado enquanto o menu está na tela.
     * - Clique no botão Jogar => abre LevelSelectScreen
     * - Clique no botão Sair => fecha o jogo
     * - ENTER ou SPACE => abre LevelSelectScreen
     * - ESC => fecha o jogo
     */
    private void handleInput() {
        float H = Gdx.graphics.getHeight();

        // Clique/touch do mouse
        if (Gdx.input.justTouched()) {
            float mx = Gdx.input.getX(); // X do clique em coord. de tela
            float my = H - Gdx.input.getY(); // converte Y (tela → mundo, origem em baixo)

            // Verifica se o clique caiu dentro do retângulo do botão Jogar
            if (rJogar.contains(mx, my)) {
                // Vai para tela de seleção de fase
                game.setScreen(new LevelSelectScreen(game));
                return;
            }
            // Verifica se clicou no botão Sair
            if (rSair.contains(mx, my)) {
                // Encerra a aplicação
                Gdx.app.exit();
                return;
            }
        }

        // Teclas ENTER ou SPACE também iniciam o jogo (vão para seleção de fase)
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
                Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            game.setScreen(new LevelSelectScreen(game));
        }

        // ESC encerra o jogo
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }
    }

    /**
     * resize()
     * --------
     * Chamado quando a janela muda de tamanho.
     * Eu recalculo o layout do menu e aviso o background para se ajustar.
     */
    @Override
    public void resize(int width, int height) {
        layout(); // recalcula posições de logo e botões
        if (bg != null)
            bg.onResize(); // ajusta o fundo
    }

    @Override
    public void pause() {
        // Menu não tem lógica especial de pause.
    }

    @Override
    public void resume() {
        // Retorno do pause do sistema (também sem lógica extra aqui).
    }

    @Override
    public void hide() {
        // Chamado quando outra tela entra no lugar.
        // Quem chama setScreen() geralmente em seguida chama dispose().
    }

    /**
     * dispose()
     * ---------
     * Libera TUDO que foi criado em show():
     * - SpriteBatch
     * - fontes
     * - fundo
     * - moto
     * - texturas do logo e dos botões
     *
     * Evita vazamento de memória de GPU.
     */
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
