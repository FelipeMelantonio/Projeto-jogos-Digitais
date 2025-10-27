package com.felipemelantonio.motorunneriot.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

public class Carro {
    private static Texture TEX;

    private final Rectangle bounds;
    private final float velocidadePx;
    private static final float SCALE = 0.085f;

    private final float[] lanesX;
    private final int laneIndex;

    public static void initTextureIfNeeded() {
        if (TEX == null) TEX = new Texture("carro.png");
    }

    /**
     * Use o MESMO laneCount e insetFactor da fase (os mesmos usados na Moto).
     */
    public Carro(int laneIndex, float startY, float velocidadeCarroPx, int laneCount, float insetFactor) {
        initTextureIfNeeded();

        float screenWidth  = Gdx.graphics.getWidth();
        float margem       = screenWidth * insetFactor; // mesma margem lateral da pista
        float larguraPista = screenWidth - (margem * 2);

        // === CÁLCULO DE CENTROS ALINHADO À MOTO ===
        float[] centers;
        switch (Math.max(2, laneCount)) {
            case 4: {
                // Fase 3: 4 faixas — mesmas frações da Moto: 1/8, 3/8, 5/8, 7/8
                float[] frac = new float[]{ 1f/8f, 3f/8f, 5f/8f, 7f/8f };
                centers = new float[]{
                    margem + larguraPista * frac[0],
                    margem + larguraPista * frac[1],
                    margem + larguraPista * frac[2],
                    margem + larguraPista * frac[3]
                };
                break;
            }
            case 3: {
                // Fase 2: 3 faixas — 1/4, 2/4, 3/4
                float esp = larguraPista / 4f;
                centers = new float[]{
                    margem + esp * 1f,
                    margem + esp * 2f,
                    margem + esp * 3f
                };
                break;
            }
            default: {
                // Fase 1: 2 faixas — 1/3 e 2/3
                float esp = larguraPista / 3f;
                centers = new float[]{
                    margem + esp * 1f,
                    margem + esp * 2f
                };
                break;
            }
        }

        float width  = TEX.getWidth()  * SCALE;
        float height = TEX.getHeight() * SCALE;

        this.laneIndex = Math.max(0, Math.min(laneIndex, centers.length - 1));
        float laneX = centers[this.laneIndex];

        this.lanesX = centers;
        this.bounds = new Rectangle(laneX - width / 2f, startY, width, height);

        // velocidade mínima para não parecer parado
        this.velocidadePx = Math.max(60f, velocidadeCarroPx);
    }

    // Compat (assume fase 3: 4 faixas, inset 0.15f)
    public Carro(int laneIndex, float startY, float velocidadeCarroPx) {
        this(laneIndex, startY, velocidadeCarroPx, 4, 0.15f);
    }

    public void update(float dt, float worldSpeedPx) {
        bounds.y -= (worldSpeedPx + velocidadePx) * dt;
    }

    public void draw(SpriteBatch batch) {
        batch.draw(TEX, bounds.x, bounds.y, bounds.width, bounds.height);
    }

    public Rectangle getBounds() { return bounds; }
    public int getLaneIndex() { return laneIndex; }

    public static void disposeStatic() {
        if (TEX != null) {
            TEX.dispose();
            TEX = null;
        }
    }
}
