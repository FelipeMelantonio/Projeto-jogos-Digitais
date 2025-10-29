package com.felipemelantonio.motorunneriot.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import java.util.Random;

public class Carro {

    // === TEXTURAS E SORTEIO ===
    private static Array<Texture> texturasCarros;
    private static boolean texturasCarregadas = false;
    private static final Random random = new Random();

    // === ATRIBUTOS DE INSTÂNCIA ===
    private final Texture texturaCarro;
    private final Rectangle bounds;
    private final float velocidadePx;
    private static final float SCALE = 0.085f;

    private final float[] lanesX;
    private final int laneIndex;

    // === COMPATIBILIDADE COM GameScreen ===
    public static void initTextureIfNeeded() {
        // GameScreen chama esse método, então apenas redirecionamos
        initTexturesIfNeeded();
    }

    // === MÉTODO ESTÁTICO PARA CARREGAR AS TEXTURAS ===
    private static void initTexturesIfNeeded() {
        if (!texturasCarregadas) {
            texturasCarros = new Array<>();
            texturasCarros.add(new Texture("carro.png"));
            texturasCarros.add(new Texture("carro2.png"));
            texturasCarros.add(new Texture("carro3.png"));
            texturasCarros.add(new Texture("carro4.png"));
            texturasCarros.add(new Texture("carro5.png"));
            texturasCarros.add(new Texture("carro6.png"));
            texturasCarros.add(new Texture("carro7.png"));
            texturasCarregadas = true;
        }
    }

    /**
     * Construtor principal — mantém toda a lógica de faixas do código base,
     * adicionando o sorteio de textura entre vários modelos de carros.
     */
    public Carro(int laneIndex, float startY, float velocidadeCarroPx, int laneCount, float insetFactor) {
        initTexturesIfNeeded();

        // Sorteia uma textura aleatória para este carro
        this.texturaCarro = texturasCarros.random();

        float screenWidth  = Gdx.graphics.getWidth();
        float margem       = screenWidth * insetFactor;
        float larguraPista = screenWidth - (margem * 2);

        // === CÁLCULO DE CENTROS ALINHADO À MOTO ===
        float[] centers;
        switch (Math.max(2, laneCount)) {
            case 4: {
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
                float esp = larguraPista / 4f;
                centers = new float[]{
                    margem + esp * 1f,
                    margem + esp * 2f,
                    margem + esp * 3f
                };
                break;
            }
            default: {
                float esp = larguraPista / 3f;
                centers = new float[]{
                    margem + esp * 1f,
                    margem + esp * 2f
                };
                break;
            }
        }

        float width  = texturaCarro.getWidth()  * SCALE;
        float height = texturaCarro.getHeight() * SCALE;

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

    // === MÉTODOS PRINCIPAIS ===
    public void update(float dt, float worldSpeedPx) {
        bounds.y -= (worldSpeedPx + velocidadePx) * dt;
    }

    public void draw(SpriteBatch batch) {
        batch.draw(texturaCarro, bounds.x, bounds.y, bounds.width, bounds.height);
    }

    public Rectangle getBounds() { return bounds; }
    public int getLaneIndex() { return laneIndex; }

    // === LIMPEZA DE RECURSOS ===
    public static void disposeStatic() {
        if (texturasCarregadas && texturasCarros != null) {
            for (Texture t : texturasCarros) {
                t.dispose();
            }
            texturasCarros.clear();
            texturasCarregadas = false;
        }
    }
}
