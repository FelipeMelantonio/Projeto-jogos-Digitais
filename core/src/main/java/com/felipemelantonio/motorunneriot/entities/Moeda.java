package com.felipemelantonio.motorunneriot.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

/** Moeda coletável alinhada às faixas. */
public class Moeda {

    // textura única compartilhada (carregada uma vez)
    private static Texture texture;
    private static boolean loaded = false;

    private final Rectangle bounds;
    private final int laneIndex; // faixa em que nasceu
    private static final float SCALE = 0.06f; // ajuste fino do tamanho na tela

    public static void initIfNeeded() {
        if (!loaded) {
            texture = new Texture("moeda.png"); // coloque seu arquivo aqui
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            loaded = true;
        }
    }

    /**
     * Cria a moeda centralizada na X da faixa informada.
     * @param laneCenters array com os centros (em X) das faixas (mesmo usado em Carro/Moto)
     * @param laneIndex índice da faixa
     * @param startY posição Y inicial (normalmente acima da tela)
     */
    public Moeda(float[] laneCenters, int laneIndex, float startY) {
        initIfNeeded();
        float w = texture.getWidth()  * SCALE;
        float h = texture.getHeight() * SCALE;
        float xCenter = laneCenters[Math.max(0, Math.min(laneIndex, laneCenters.length - 1))];

        this.laneIndex = laneIndex;
        this.bounds = new Rectangle(xCenter - w/2f, startY, w, h);
    }

    public void update(float dt, float worldSpeedPx) {
        // moeda acompanha o fluxo do mundo (não tem velocidade própria)
        bounds.y -= worldSpeedPx * dt;
    }

    public void draw(SpriteBatch batch) {
        batch.draw(texture, bounds.x, bounds.y, bounds.width, bounds.height);
    }

    public Rectangle getBounds() { return bounds; }
    public int getLaneIndex()    { return laneIndex; }

    public static void disposeStatic() {
        if (loaded && texture != null) {
            texture.dispose();
            loaded = false;
        }
    }
}
