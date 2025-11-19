package com.felipemelantonio.motorunneriot.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

/**
 * Representa uma moeda coletável do jogo.
 * Ela desce pela pista alinhada às faixas (igual aos carros),
 * e o jogador coleta ao encostar na hitbox.
 */
public class Moeda {

    // ============================================================
    // TEXTURA ÚNICA COMPARTILHADA ENTRE TODAS AS INSTÂNCIAS
    // ============================================================

    /**
     * A moeda usa apenas 1 textura no jogo inteiro.
     * Em vez de carregar várias vezes, carregamos uma única vez
     * e reutilizamos nas outras instâncias → performance e economia de memória.
     */
    private static Texture texture;

    /** Flag usada para garantir que a textura só será carregada uma vez. */
    private static boolean loaded = false;

    // ============================================================
    // ATRIBUTOS DA MOEDA
    // ============================================================

    /** Retângulo de colisão e posição da moeda. */
    private final Rectangle bounds;

    /** Faixa em que a moeda nasceu (0, 1, 2...). */
    private final int laneIndex;

    /** Escala para ajustar o tamanho da moeda na tela. */
    private static final float SCALE = 0.06f;

    // ============================================================
    // CARREGAMENTO DA TEXTURA (APENAS UMA VEZ)
    // ============================================================

    /**
     * Método chamado antes de criar qualquer moeda.
     * Carrega a textura apenas se ainda não estiver carregada.
     */
    public static void initIfNeeded() {
        if (!loaded) {
            texture = new Texture("moeda.png"); // arquivo dentro da pasta assets/

            // Ativa filtro linear: evita serrilhado e deixa o sprite mais suave.
            texture.setFilter(Texture.TextureFilter.Linear,
                    Texture.TextureFilter.Linear);

            loaded = true;
        }
    }

    // ============================================================
    // CONSTRUTOR DA MOEDA
    // ============================================================

    /**
     * Cria a moeda já posicionada no centro da faixa correspondente.
     *
     * @param laneCenters array contendo as coordenadas X de cada faixa
     * @param laneIndex   índice da faixa onde a moeda será criada
     * @param startY      posição Y inicial (acima da tela normalmente)
     */
    public Moeda(float[] laneCenters, int laneIndex, float startY) {

        // Garante que a textura já foi carregada.
        initIfNeeded();

        // Calcula largura e altura da moeda após aplicar a escala.
        float w = texture.getWidth() * SCALE;
        float h = texture.getHeight() * SCALE;

        // Garante que laneIndex está dentro do intervalo válido.
        float xCenter = laneCenters[Math.max(0, Math.min(laneIndex, laneCenters.length - 1))];

        // Salva a faixa original
        this.laneIndex = laneIndex;

        /**
         * Cria o retângulo da moeda:
         * - Centraliza na faixa (X)
         * - Começa no Y informado (normalmente acima da tela)
         * - Define largura/altura de acordo com o sprite escalado
         */
        this.bounds = new Rectangle(
                xCenter - w / 2f, // centraliza
                startY, // nasce acima da tela
                w,
                h);
    }

    // ============================================================
    // ATUALIZAÇÃO DA MOEDA
    // ============================================================

    /**
     * Atualiza a moeda descendo junto com o mundo.
     * A moeda não tem velocidade própria — ela apenas acompanha
     * o worldSpeed (velocidade da fase).
     */
    public void update(float dt, float worldSpeedPx) {
        bounds.y -= worldSpeedPx * dt;
    }

    // ============================================================
    // DESENHO
    // ============================================================

    /** Desenha a moeda na posição atual. */
    public void draw(SpriteBatch batch) {
        batch.draw(texture, bounds.x, bounds.y, bounds.width, bounds.height);
    }

    // ============================================================
    // GETTERS
    // ============================================================

    public Rectangle getBounds() {
        return bounds;
    }

    public int getLaneIndex() {
        return laneIndex;
    }

    // ============================================================
    // LIMPEZA DA TEXTURA ESTÁTICA
    // ============================================================

    /**
     * Libera a textura da memória ao fechar o jogo.
     * Chamado pelo GameScreen.dispose().
     */
    public static void disposeStatic() {
        if (loaded && texture != null) {
            texture.dispose();
            loaded = false;
        }
    }
}
