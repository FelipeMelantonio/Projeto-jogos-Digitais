package com.felipemelantonio.motorunneriot.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.felipemelantonio.motorunneriot.MotoRunnerGame;

/**
 * GameOverScreen
 * --------------
 * Esta classe representa a TELA DE GAME OVER do jogo.
 *
 * Responsabilidades principais:
 * - Exibir um fundo específico de "Game Over"
 * - Mostrar ao jogador:
 * - a distância final percorrida na fase
 * - as instruções de teclas para:
 * - reiniciar a fase atual (ENTER)
 * - voltar ao menu principal (ESC)
 * - Detectar o input dessas teclas e trocar de tela usando o MotoRunnerGame.
 *
 * Ela implementa a interface Screen do LibGDX, então participa do ciclo de vida
 * de telas do framework (show, render, resize, pause, resume, hide, dispose).
 */
public class GameOverScreen implements Screen {

    // Referência para o "jogo principal" (classe que estende
    // com.badlogic.gdx.Game).
    // Essa referência permite fazer game.setScreen(...) para trocar de tela.
    private final MotoRunnerGame game;

    // SpriteBatch específico desta tela.
    // É o "pincel" que usamos para desenhar texturas e textos na tela.
    private SpriteBatch batch;

    // Fonte usada para desenhar todos os textos da GameOverScreen.
    private BitmapFont font;

    // Textura de fundo exibida quando o jogador perde.
    // Normalmente é uma imagem fullscreen escrita "Game Over" ou similar.
    private Texture background;

    // Distância final percorrida na partida que acabou.
    // É recebida da GameScreen e exibida ao jogador como feedback de desempenho.
    private float distanciaFinal;

    // Número da fase em que o jogador estava quando perdeu.
    // Guardar esse valor permite reiniciar exatamente essa fase.
    private int fase;

    /**
     * Construtor da tela de Game Over.
     *
     * @param game      referência para o jogo principal (usada para troca de telas)
     * @param distancia distância percorrida na fase (em "metros do jogo")
     * @param fase      número da fase em que o jogador estava
     *
     *                  Aqui fazemos:
     *                  - guardar os parâmetros em atributos
     *                  - criar os objetos de desenho (SpriteBatch, BitmapFont)
     *                  - carregar a textura de fundo do Game Over
     */
    public GameOverScreen(MotoRunnerGame game, float distancia, int fase) {
        // Guarda a referência para o jogo principal.
        this.game = game;

        // Armazena a distância percorrida para exibição.
        this.distanciaFinal = distancia;

        // Armazena o número da fase em que o jogador perdeu.
        this.fase = fase;

        // Cria um SpriteBatch só para esta tela.
        // Poderíamos compartilhar um único batch global entre telas,
        // mas usar um batch aqui torna a tela independente.
        batch = new SpriteBatch();

        // Cria uma fonte básica padrão do LibGDX.
        // Ela usa um bitmap interno simples, suficiente para textos de HUD.
        font = new BitmapFont();

        // Carrega a imagem de fundo da tela de Game Over.
        // É importante que "gameover-image.jpg" esteja na pasta assets do projeto.
        // Essa imagem normalmente cobre a tela inteira com uma arte de "Game Over".
        background = new Texture("gameover-image.jpg");
    }

    /**
     * render(delta)
     * --------------
     * Método chamado em todo frame enquanto esta tela está ativa.
     *
     * Funções principais neste método:
     * 1) Limpar a tela com uma cor de fundo (preto)
     * 2) Desenhar:
     * - a imagem de fundo de Game Over
     * - o texto da distância final
     * - as instruções ENTER / ESC
     * 3) Ler input do teclado para:
     * - ENTER → reiniciar a fase atual
     * - ESC → voltar ao menu principal
     *
     * @param delta tempo em segundos desde o último frame (LibGDX passa isso
     *              automaticamente)
     */
    @Override
    public void render(float delta) {
        // 1) Limpa a tela, definindo a cor de fundo como preto (RGBA = 0,0,0,1).
        // Isso garante que não restem resíduos de telas anteriores.
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Captura a largura e a altura atuais da tela.
        // Isso permite desenhar elementos que se adaptam a qualquer resolução.
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        // Inicia o processo de desenho.
        // Tudo que for desenhado entre begin() e end() será enviado à GPU.
        batch.begin();

        // 2) Desenha a imagem de fundo ocupando a tela inteira.
        // Posição (0,0) é o canto inferior esquerdo no sistema de coordenadas padrão do
        // LibGDX.
        // Usamos screenWidth e screenHeight para esticar a textura no tamanho da tela.
        batch.draw(background, 0, 0, screenWidth, screenHeight);

        // 3) Configura o tamanho da fonte.
        // Aumentamos um pouco a escala da fonte para a leitura ficar confortável.
        font.getData().setScale(1.1f);

        // 4) Preparamos os textos que serão exibidos na parte inferior da tela.
        // texto2 → mostra a distância final percorrida
        // texto3 → instrução para reiniciar (ENTER)
        // texto4 → instrução para voltar ao menu (ESC)
        String texto2 = "Distância: " + (int) distanciaFinal + " m";
        String texto3 = "ENTER - Reiniciar";
        String texto4 = "ESC - Menu Principal";

        // 5) GlyphLayout é usado para calcular largura/altura de um texto com a fonte
        // atual.
        // Aqui usamos para saber a largura de cada texto e assim centralizar o
        // conjunto.
        GlyphLayout layout2 = new GlyphLayout(font, texto2);
        GlyphLayout layout3 = new GlyphLayout(font, texto3);
        GlyphLayout layout4 = new GlyphLayout(font, texto4);

        // Espaço horizontal entre cada texto (em pixels).
        float espaco = 60f;

        // Largura total do "bloco" de textos:
        // soma das larguras dos três textos + 2 intervalos de espaço.
        float totalLargura = layout2.width + layout3.width + layout4.width + (espaco * 2);

        // Posição X inicial para desenhar o bloco de textos centralizado.
        // Fórmula: (larguraDaTela - larguraTotalDoBloco) / 2.
        // Isso coloca o conjunto todo no meio da tela.
        float startX = (screenWidth - totalLargura) / 2f;

        // Coordenada Y para desenhar os textos (próximo do rodapé da tela).
        float y = 50f;

        // 6) Desenha os textos lado a lado, começando em startX.
        float currentX = startX;

        // 6.1) Desenha o texto da distância.
        font.draw(batch, layout2, currentX, y);
        // Avança o X: largura do texto + espaço.
        currentX += layout2.width + espaco;

        // 6.2) Desenha o texto com a instrução do ENTER.
        font.draw(batch, layout3, currentX, y);
        currentX += layout3.width + espaco;

        // 6.3) Desenha o texto com a instrução do ESC.
        font.draw(batch, layout4, currentX, y);

        // Finaliza o desenho deste frame.
        batch.end();

        // 7) Leitura das teclas (INPUT)
        // Importante fazer isso FORA do batch.begin()/end().

        // Se o jogador apertou ENTER nesse frame:
        // - criamos uma nova GameScreen com a mesma fase que ele estava
        // - usamos game.setScreen(...) para trocar para ela
        // Dessa forma, o jogo é reiniciado na mesma fase em que ele perdeu.
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER))
            game.setScreen(new GameScreen(game, fase));

        // Se o jogador apertou ESC nesse frame:
        // - trocamos a tela atual pela tela de menu principal (MenuScreen)
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            game.setScreen(new MenuScreen(game));
    }

    /**
     * dispose()
     * ----------
     * Método chamado quando essa tela não for mais usada.
     * Aqui liberamos todos os recursos gráficos que foram alocados:
     * - SpriteBatch
     * - BitmapFont
     * - Texture de fundo
     *
     * Isso é importante para evitar vazamento de memória, especialmente em GPU.
     */
    @Override
    public void dispose() {
        batch.dispose(); // Libera o SpriteBatch
        font.dispose(); // Libera a fonte
        background.dispose(); // Libera a textura de fundo
    }

    // ================== Métodos restantes do ciclo de vida da Screen
    // ==================
    // No momento, essa tela não precisa reagir a esses eventos, então eles ficam
    // vazios.
    // Mesmo vazios, eles são obrigatórios porque fazem parte da interface Screen.

    /**
     * show()
     * -------
     * Chamado automaticamente quando essa tela é configurada como a tela ativa
     * via game.setScreen(new GameOverScreen(...)).
     * Aqui poderíamos, por exemplo, reiniciar timers ou tocar uma música
     * específica,
     * mas como essa tela já é totalmente inicializada no construtor, não há lógica
     * extra.
     */
    @Override
    public void show() {
    }

    /**
     * resize(width, height)
     * ----------------------
     * Chamado quando a janela é redimensionada.
     * Não precisamos ajustar nada aqui porque já usamos a largura/altura dinâmica
     * no render(), então o layout se adapta automaticamente.
     */
    @Override
    public void resize(int width, int height) {
    }

    /**
     * pause()
     * -------
     * Chamado quando o jogo é pausado pelo sistema (por exemplo, ao minimizar o
     * app).
     * Para essa tela de Game Over, não há estado especial para gerenciar.
     */
    @Override
    public void pause() {
    }

    /**
     * resume()
     * --------
     * Chamado quando o jogo volta de um estado de pause.
     * Não há lógica específica para retomar na tela de Game Over.
     */
    @Override
    public void resume() {
    }

    /**
     * hide()
     * ------
     * Chamado quando essa tela deixa de ser a tela ativa,
     * porque outra tela foi chamada com setScreen().
     * Geralmente a limpeza pesada fica em dispose(), então aqui não é necessário
     * nada.
     */
    @Override
    public void hide() {
    }
}
