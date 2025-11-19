package com.felipemelantonio.motorunneriot.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.felipemelantonio.motorunneriot.MotoRunnerGame;

/**
 * LevelCompleteScreen
 * -------------------
 * Esta classe representa a TELA DE FASE CONCLUÍDA.
 *
 * Ela é exibida quando o jogador atinge a meta de distância de uma fase.
 * Responsabilidades:
 * - Mostrar o fundo de "level up" / fase concluída.
 * - Exibir informações da fase que acabou:
 * - distância final
 * - quantidade de moedas coletadas
 * - Mostrar um "menuzinho" no rodapé com atalhos:
 * - ENTER / ESPAÇO → voltar para o menu principal
 * - R → repetir a mesma fase
 * - 1 / 2 / 3 → ir diretamente para outra fase
 * - ESC → também volta pro menu
 * - Desenhar um retângulo semitransparente atrás do texto
 * para melhorar a legibilidade (overlay por cima da imagem).
 */
public class LevelCompleteScreen implements Screen {

    // Referência para o jogo principal (classe MotoRunnerGame).
    // Usamos esta referência para trocar de tela com game.setScreen(...).
    private final MotoRunnerGame game;

    // Fase que o jogador acabou de completar.
    private final int fase;

    // Distância final percorrida na fase (em "metros do jogo").
    private final int distanciaFinal;

    // Quantidade de moedas coletadas na fase.
    private final int moedas;

    // Objetos de renderização:
    private SpriteBatch batch; // usado para desenhar fundos e textos
    private BitmapFont fontBig; // fonte maior (mensagem central)
    private BitmapFont fontSmall; // fonte menor (informações do rodapé)
    private Texture background; // imagem de fundo de "level up"
    private ShapeRenderer shapeRenderer; // usado para desenhar o retângulo semitransparente

    /**
     * Construtor da tela de "fase concluída".
     *
     * @param game           referência ao jogo principal
     * @param fase           fase que foi concluída
     * @param distanciaFinal distância percorrida na fase
     * @param moedas         moedas coletadas na fase
     *
     *                       Aqui apenas armazenamos esses dados. A criação de
     *                       objetos gráficos
     *                       (batch, fontes, textura, shapeRenderer) é feita no
     *                       show().
     */
    public LevelCompleteScreen(MotoRunnerGame game, int fase, int distanciaFinal, int moedas) {
        this.game = game;
        this.fase = fase;
        this.distanciaFinal = distanciaFinal;
        this.moedas = moedas;
    }

    /**
     * show()
     * ------
     * Chamado automaticamente quando esta tela passa a ser a tela ativa
     * via game.setScreen(new LevelCompleteScreen(...)).
     *
     * É aqui que inicializamos:
     * - SpriteBatch
     * - fontes
     * - textura de fundo
     * - ShapeRenderer para o retângulo de contraste
     */
    @Override
    public void show() {
        // Cria um SpriteBatch só para esta tela.
        batch = new SpriteBatch();

        // Cria duas fontes:
        // - fontBig: usada para o texto central (poderia exibir "Fase X Concluída", por
        // exemplo)
        // - fontSmall: usada para o texto do rodapé (informações e atalhos)
        fontBig = new BitmapFont();
        fontSmall = new BitmapFont();

        // Carrega a imagem de fundo da tela de nível concluído.
        // Essa textura normalmente é uma arte de "Level Complete" ou "Parabéns".
        background = new Texture("levelup.png"); // imagem de fundo em fullscreen

        // ShapeRenderer será usado para desenhar o retângulo semitransparente
        // atrás dos textos do rodapé, melhorando a leitura.
        shapeRenderer = new ShapeRenderer();

        // Define as cores das fontes:
        // - Título em dourado
        // - Informações em branco
        fontBig.setColor(Color.GOLD);
        fontSmall.setColor(Color.WHITE);
    }

    /**
     * render(delta)
     * --------------
     * Chamado em todo frame enquanto essa tela está ativa.
     *
     * Fluxo:
     * 1) Limpa a tela.
     * 2) Desenha a imagem de fundo.
     * 3) Calcula os textos e suas larguras (para centralizar tudo no rodapé).
     * 4) Desenha um retângulo semitransparente atrás dos textos do rodapé.
     * 5) Desenha os textos por cima do retângulo.
     * 6) Desenha uma mensagem central de destaque (fontBig).
     * 7) Lê o input do jogador (teclas) e faz a navegação entre telas.
     */
    @Override
    public void render(float delta) {
        // 1) Limpa a tela com a cor preta.
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Pega dimensões atuais da tela para desenhar em qualquer resolução.
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        // 2) Desenha a imagem de fundo em fullscreen.
        batch.begin();
        batch.draw(background, 0, 0, screenWidth, screenHeight);
        batch.end();

        // 3) Prepara os textos que aparecerão no rodapé (informações e atalhos).
        String texto2 = "Distância: " + distanciaFinal + " m";
        String texto3 = "Moedas: " + moedas;
        String texto4 = "ENTER / ESPAÇO — Menu";
        String texto5 = "R — Repetir";
        String texto6 = "1 / 2 / 3 — Outra Fase";

        // Usamos a fonte pequena para esses textos
        BitmapFont font = fontSmall;
        font.getData().setScale(1.1f); // aumenta um pouco o tamanho

        // GlyphLayout calcula a largura de cada texto na fonte atual
        GlyphLayout layout2 = new GlyphLayout(font, texto2);
        GlyphLayout layout3 = new GlyphLayout(font, texto3);
        GlyphLayout layout4 = new GlyphLayout(font, texto4);
        GlyphLayout layout5 = new GlyphLayout(font, texto5);
        GlyphLayout layout6 = new GlyphLayout(font, texto6);

        // Espaço horizontal entre cada texto
        float espaco = 50f;

        // Largura total do bloco com TODOS os textos do rodapé + espaços
        float totalLargura = layout2.width +
                layout3.width +
                layout4.width +
                layout5.width +
                layout6.width +
                espaco * 4;

        // startX é a posição X inicial para centralizar o bloco de textos na tela
        float startX = (screenWidth - totalLargura) / 2f;

        // Altura (Y) onde os textos do rodapé serão desenhados
        float y = 50f; // próximo ao rodapé

        // 4) Desenha um retângulo preto semitransparente atrás dos textos
        // para aumentar contraste e legibilidade.

        // Altura do retângulo
        float rectAltura = 60f;
        // Posição Y do retângulo (um pouco abaixo dos textos, por isso o +15f)
        float rectY = y - rectAltura + 15f;

        // Habilita blend para permitir transparência (alpha)
        Gdx.gl.glEnable(GL20.GL_BLEND);

        // Inicia o ShapeRenderer em modo "Filled" (retângulo preenchido)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        // Cor RGBA (0,0,0,0.5) → preto com 50% de transparência
        shapeRenderer.setColor(0, 0, 0, 0.5f);
        // Desenha o retângulo logo atrás do bloco de textos
        shapeRenderer.rect(startX - 20, // X inicial um pouco antes dos textos
                rectY, // Y do retângulo
                totalLargura + 40, // largura um pouco maior que o bloco de texto
                rectAltura); // altura do retângulo
        shapeRenderer.end();

        // Desabilita blend depois de desenhar formas semitransparentes
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // 5) Desenha os textos do rodapé por cima do retângulo
        batch.begin();

        float currentX = startX;

        // Distância final
        font.draw(batch, layout2, currentX, y);
        currentX += layout2.width + espaco;

        // Moedas
        font.draw(batch, layout3, currentX, y);
        currentX += layout3.width + espaco;

        // ENTER / ESPAÇO → Menu
        font.draw(batch, layout4, currentX, y);
        currentX += layout4.width + espaco;

        // R → Repetir
        font.draw(batch, layout5, currentX, y);
        currentX += layout5.width + espaco;

        // 1 / 2 / 3 → Outra fase
        font.draw(batch, layout6, currentX, y);

        batch.end();

        // 6) Mensagem central de destaque (texto grande no meio da tela)
        // Aqui você deixou a string vazia, mas poderia ser algo como:
        // "FASE " + fase + " CONCLUÍDA!"
        batch.begin();
        fontBig.getData().setScale(2.0f);
        // OBS: string atual está vazia: " " + " "
        // Você pode trocar por um texto real se quiser mostrar algo na apresentação.
        fontBig.draw(batch, " " + " ", screenWidth / 2f - 180, screenHeight / 2f + 80);
        batch.end();

        // 7) Navegação (input de teclado)
        // Aqui definimos o que acontece quando o jogador aperta cada tecla.

        // ENTER ou ESPAÇO → volta para o Menu principal
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
                Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
            game.setScreen(new MenuScreen(game));

        // R → repetir a mesma fase que acabou de ser concluída
        if (Gdx.input.isKeyJustPressed(Input.Keys.R))
            game.setScreen(new GameScreen(game, fase));

        // 1, 2 ou 3 → ir direto para uma fase específica
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1))
            game.setScreen(new GameScreen(game, 1));
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2))
            game.setScreen(new GameScreen(game, 2));
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3))
            game.setScreen(new GameScreen(game, 3));

        // ESC → também volta para o Menu
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            game.setScreen(new MenuScreen(game));
    }

    // Métodos padrão da Screen que não precisam de lógica específica aqui.
    // Eles são obrigatórios pela interface, então declaramos vazios.

    @Override
    public void resize(int width, int height) {
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
     * Libera todos os recursos gráficos utilizados por esta tela:
     * - SpriteBatch
     * - as duas fontes
     * - textura de fundo
     * - ShapeRenderer
     *
     * É chamado quando a tela não será mais usada, evitando vazamento de memória.
     */
    @Override
    public void dispose() {
        if (batch != null)
            batch.dispose();
        if (fontBig != null)
            fontBig.dispose();
        if (fontSmall != null)
            fontSmall.dispose();
        if (background != null)
            background.dispose();
        if (shapeRenderer != null)
            shapeRenderer.dispose();
    }
}
