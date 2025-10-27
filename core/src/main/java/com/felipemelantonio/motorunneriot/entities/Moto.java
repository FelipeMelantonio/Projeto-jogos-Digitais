package com.felipemelantonio.motorunneriot.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Rectangle;
import com.felipemelantonio.motorunneriot.utils.IoTInput;

public class Moto {
    private Texture texture;
    private Rectangle bounds;
    private float velocidade;
    private int currentLaneIndex;
    private float targetX;
    private float moveSpeed = 10f;

    private static final float SCALE = 0.09f;
    private float[] lanesX;
    private int laneCount;
    private float insetFactor; // margem lateral (fração da largura da tela)

    public Moto(int laneCount) {
        this(laneCount, 0.15f);
    }

    public Moto(int laneCount, float insetFactor) {
        texture = new Texture("moto.png");
        float width  = texture.getWidth()  * SCALE;
        float height = texture.getHeight() * SCALE;

        this.laneCount   = Math.max(2, laneCount);
        this.insetFactor = insetFactor;
        lanesX = computeLaneCenters(this.laneCount, this.insetFactor);

        // começa na faixa central
        currentLaneIndex = this.laneCount / 2;
        float x = lanesX[currentLaneIndex] - width / 2f;
        float y = 80;

        bounds = new Rectangle(x, y, width, height);
        targetX = bounds.x;
    }

    /** Calcula os centros das faixas considerando a margem lateral (insetFactor). */
    private float[] computeLaneCenters(int laneCount, float inset) {
        float screenWidth   = Gdx.graphics.getWidth();
        float margem        = screenWidth * inset;      // margem lateral da pista
        float larguraPista  = screenWidth - (margem * 2);

        switch (laneCount) {
            case 4: {
                // Fase 3: 4 faixas EQUISPAÇADAS no asfalto -> 1/8, 3/8, 5/8, 7/8
                float[] frac = new float[]{ 1f/8f, 3f/8f, 5f/8f, 7f/8f };
                return new float[] {
                    margem + larguraPista * frac[0],
                    margem + larguraPista * frac[1],
                    margem + larguraPista * frac[2],
                    margem + larguraPista * frac[3]
                };
            }
            case 3: {
                // 3 faixas igualmente espaçadas: 1/4, 2/4, 3/4
                float esp = larguraPista / 4f;
                return new float[] {
                    margem + esp * 1f,
                    margem + esp * 2f,
                    margem + esp * 3f
                };
            }
            default: { // 2 faixas = duas internas
                float esp = larguraPista / 3f;
                return new float[] {
                    margem + esp * 1f,
                    margem + esp * 2f
                };
            }
        }
    }

    public void update(float delta) {
        // mantém alinhado se a resolução/margem mudar
        lanesX = computeLaneCenters(this.laneCount, this.insetFactor);

        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) && currentLaneIndex > 0) {
            currentLaneIndex--;
            targetX = lanesX[currentLaneIndex] - bounds.width / 2f;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) && currentLaneIndex < lanesX.length - 1) {
            currentLaneIndex++;
            targetX = lanesX[currentLaneIndex] - bounds.width / 2f;
        }

        bounds.x = Interpolation.linear.apply(bounds.x, targetX, delta * moveSpeed);
        velocidade = IoTInput.getCurrentSpeed();
    }

    public void draw(SpriteBatch batch) { batch.draw(texture, bounds.x, bounds.y, bounds.width, bounds.height); }
    public Rectangle getBounds() { return bounds; }
    public float getVelocidade() { return velocidade; }

    public void dispose() {
        if (texture != null) texture.dispose();
    }
}
