package com.felipemelantonio.motorunneriot.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Classe responsável pelo fundo da pista.
 * Ela faz o efeito de scroll vertical contínuo (a pista descendo sem parar).
 * Isso dá a sensação de movimento no jogo.
 */
public class Background {

    private final Texture texture; // imagem do fundo
    private float speedPx; // velocidade em pixels/seg
    private float tileHeight; // altura total da textura já escalada para a tela
    private float scale; // proporção entre largura da tela e largura da textura
    private float scroll; // quanto já “andou” o fundo

    /** Construtor padrão. */
    public Background() {
        this("estrada.png", 220f);
    }

    /**
     * Construtor completo.
     * 
     * @param fileName -- arquivo PNG do fundo
     * @param speedPx  -- velocidade do scroll
     */
    public Background(String fileName, float speedPx) {
        this.texture = new Texture(fileName);

        // Filtro linear evita serrilhado e tremidas quando o fundo rola rápido
        this.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        this.speedPx = speedPx;
        recalcTileSize(); // calcula altura escalada correta
    }

    /**
     * Calcula o tamanho real do fundo com base na largura da tela.
     * Isso garante que o fundo nunca estique errado quando entra em fullscreen.
     */
    private void recalcTileSize() {
        float screenW = Gdx.graphics.getWidth();

        // escala horizontal = largura da tela / largura da textura
        scale = screenW / (float) texture.getWidth();

        // altura do fundo já escalada
        tileHeight = texture.getHeight() * scale;

        // Normaliza o valor do scroll para garantir que ele sempre fique no intervalo
        // 0..tileHeight.
        // Explicação:
        // 1) (scroll % tileHeight) mantém o scroll dentro do tamanho de um tile,
        // mas pode gerar valor negativo caso o scroll seja negativo.
        // 2) (+ tileHeight) garante que mesmo que seja negativo, ele fique positivo.
        // 3) (% tileHeight) aplica o módulo novamente para voltar ao intervalo final
        // correto.
        // Isso evita flicker e garante uma rolagem infinita suave, sem "pulos" mesmo
        // após redimensionamento.
        if (tileHeight > 0f)
            scroll = (scroll % tileHeight + tileHeight) % tileHeight;
    }

    /** Chamado sempre que a janela muda de tamanho (ex: fullscreen). */
    public void onResize() {
        recalcTileSize();
    }

    /** Define a velocidade do fundo (usada para sincronizar com worldSpeed). */
    public void setSpeed(float speedPx) {
        this.speedPx = speedPx;
    }

    /** Atualiza o scroll vertical de acordo com dt. */
    public void update(float dt) {
        scroll += speedPx * dt;

        // Quando passa da altura total, volta para zero → rolagem infinita perfeita
        if (scroll >= tileHeight)
            scroll -= tileHeight;
    }

    /** Desenha o fundo repetido 2 vezes (para cobrir a tela toda). */
    public void draw(SpriteBatch batch) {
        float screenW = Gdx.graphics.getWidth();

        float y0 = -scroll;

        batch.draw(texture, 0, y0, screenW, tileHeight);
        batch.draw(texture, 0, y0 + tileHeight, screenW, tileHeight);
    }

    /** Libera memória. */
    public void dispose() {
        texture.dispose();
    }
}
