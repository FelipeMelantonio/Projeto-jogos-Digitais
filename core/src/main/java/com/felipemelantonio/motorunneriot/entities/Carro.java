package com.felipemelantonio.motorunneriot.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import java.util.Random;

public class Carro {
    private Texture texture;
    private Rectangle bounds;
    private float velocidade;
    private static final float SCALE = 0.085f;
    private float[] lanesX;

    public Carro(int nivel) {
        texture = new Texture("carro.png");

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();
        float margem = screenWidth * 0.15f;
        float larguraPista = screenWidth - (margem * 2);
        float espacamento = larguraPista / 3.0f;

        lanesX = new float[]{
            margem + espacamento * 0f + espacamento * 0.1f,
            margem + espacamento * 1f,
            margem + espacamento * 2f,
            margem + espacamento * 3f - espacamento * 0.1f
        };

        Random random = new Random();
        float laneX = lanesX[random.nextInt(lanesX.length)];

        float width = texture.getWidth() * SCALE;
        float height = texture.getHeight() * SCALE;

        bounds = new Rectangle(laneX - width / 2f, screenHeight, width, height);
        velocidade = 150 + nivel * 25;
    }

    public void update(float delta) {
        bounds.y -= velocidade * delta;
    }

    public void draw(SpriteBatch batch) {
        batch.draw(texture, bounds.x, bounds.y, bounds.width, bounds.height);
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public void dispose() {
        texture.dispose();
    }

    public static Carro spawnSafe(Array<Carro> carros, int nivel) {
        Carro novo = new Carro(nivel);
        for (Carro c : carros) {
            if (Math.abs(c.getBounds().x - novo.getBounds().x) < 5 &&
                Math.abs(c.getBounds().y - novo.getBounds().y) < 350) {
                return null;
            }
        }
        return novo;
    }
}
