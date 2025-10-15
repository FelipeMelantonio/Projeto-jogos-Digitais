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

    public Moto() {
        texture = new Texture("moto.png");
        float width = texture.getWidth() * SCALE;
        float height = texture.getHeight() * SCALE;

        atualizarLanes();

        currentLaneIndex = 1;
        float x = lanesX[currentLaneIndex] - width / 2f;
        float y = 80;

        bounds = new Rectangle(x, y, width, height);
        targetX = bounds.x;
    }

    private void atualizarLanes() {
        float screenWidth = Gdx.graphics.getWidth();
        float margem = screenWidth * 0.15f; // borda azul e branca da estrada
        float larguraPista = screenWidth - (margem * 2);
        float espacamento = larguraPista / 3.0f; // 4 faixas = 3 divisões

        lanesX = new float[]{
            margem + espacamento * 0f + espacamento * 0.1f, // faixa 1
            margem + espacamento * 1f,                      // faixa 2
            margem + espacamento * 2f,                      // faixa 3
            margem + espacamento * 3f - espacamento * 0.1f  // faixa 4
        };
    }

    public void update(float delta) {
        atualizarLanes();

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

    public void draw(SpriteBatch batch) {
        batch.draw(texture, bounds.x, bounds.y, bounds.width, bounds.height);
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public float getVelocidade() {
        return velocidade;
    }

    public void dispose() {
        texture.dispose();
    }
}
