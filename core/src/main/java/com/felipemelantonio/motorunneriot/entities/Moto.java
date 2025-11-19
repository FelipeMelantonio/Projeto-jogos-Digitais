package com.felipemelantonio.motorunneriot.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Rectangle;
// REMOVIDO: import com.felipemelantonio.motorunneriot.utils.IoTInput;

/**
 * Classe que representa a moto controlada pelo jogador.
 * Ela:
 * - fica sempre alinhada às faixas da pista;
 * - se move lateralmente entre faixas com uma animação suave;
 * - ajusta a velocidade com base na velocidade do mundo + um “esforço”
 * simulado.
 */
public class Moto {

    private Texture texture;
    private Rectangle bounds;
    private float velocidade; // px/s

    private int currentLaneIndex;
    private float targetX;
    private float moveSpeed = 18f;

    private static final float SCALE = 0.09f;
    private float[] lanesX;
    private int laneCount;
    private float insetFactor;

    private boolean controlsEnabled = true;

    // >>> Simulação interna do "esforço" (substitui IoTInput)
    private float effortTime = 0f; // faz o papel do "time" da IoTInput

    public void setControlsEnabled(boolean v) {
        controlsEnabled = v;
    }

    public Moto(int laneCount) {
        this(laneCount, 0.15f);
    }

    public Moto(int laneCount, float insetFactor) {
        texture = new Texture("moto.png");
        float width = texture.getWidth() * SCALE;
        float height = texture.getHeight() * SCALE;

        this.laneCount = Math.max(2, laneCount);
        this.insetFactor = insetFactor;

        lanesX = computeLaneCenters(this.laneCount, this.insetFactor);

        currentLaneIndex = this.laneCount / 2;
        float x = lanesX[currentLaneIndex] - width / 2f;
        float y = 80;

        bounds = new Rectangle(x, y, width, height);

        targetX = bounds.x;
        velocidade = 0f;
    }

    private float[] computeLaneCenters(int laneCount, float inset) {
        float screenWidth = Gdx.graphics.getWidth();
        float margem = screenWidth * inset;
        float larguraPista = screenWidth - (margem * 2);

        switch (laneCount) {
            case 4: {
                float[] f = new float[] { 1f / 8f, 3f / 8f, 5f / 8f, 7f / 8f };
                return new float[] {
                        margem + larguraPista * f[0],
                        margem + larguraPista * f[1],
                        margem + larguraPista * f[2],
                        margem + larguraPista * f[3]
                };
            }
            case 3: {
                float esp = larguraPista / 4f;
                return new float[] {
                        margem + esp * 1f,
                        margem + esp * 2f,
                        margem + esp * 3f
                };
            }
            default: {
                float esp = larguraPista / 3f;
                return new float[] {
                        margem + esp * 1f,
                        margem + esp * 2f
                };
            }
        }
    }

    public void update(float delta, float worldSpeed) {
        // Recalcula faixas se a resolução mudar
        lanesX = computeLaneCenters(this.laneCount, this.insetFactor);

        // Movimentação lateral entre faixas (setas)
        if (controlsEnabled) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) && currentLaneIndex > 0) {
                currentLaneIndex--;
                targetX = lanesX[currentLaneIndex] - bounds.width / 2f;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) && currentLaneIndex < lanesX.length - 1) {
                currentLaneIndex++;
                targetX = lanesX[currentLaneIndex] - bounds.width / 2f;
            }
        }

        // Animação suave até a faixa alvo
        bounds.x = Interpolation.linear.apply(bounds.x, targetX, delta * moveSpeed);

        // === Simulação de "esforço" que antes vinha da IoTInput ===
        // Mesma lógica da classe IoTInput: time += 0.1f; 50 + seno * 20
        effortTime += 0.1f;
        float effort = 50f + (float) Math.sin(effortTime) * 20f;

        // Converte esforço em bônus de até +15% da velocidade
        float effortBoost = Math.min(0.15f, effort / 400f);

        // Velocidade final da moto
        this.velocidade = worldSpeed * (0.95f + effortBoost);
    }

    public void update(float delta) {
        update(delta, 0f);
    }

    public void draw(SpriteBatch batch) {
        batch.draw(texture, bounds.x, bounds.y, bounds.width, bounds.height);
    }

    public int getCurrentLaneIndex() {
        return currentLaneIndex;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public float getVelocidade() {
        return this.velocidade;
    }

    public void dispose() {
        if (texture != null)
            texture.dispose();
    }
}
