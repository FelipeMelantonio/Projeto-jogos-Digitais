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
    private float velocidade; // px/s
    private int currentLaneIndex;
    private float targetX;
    private float moveSpeed = 18f;

    private static final float SCALE = 0.09f;
    private float[] lanesX;
    private int laneCount;
    private float insetFactor;

    // >>> trava de controle para menus
    private boolean controlsEnabled = true;
    public void setControlsEnabled(boolean v) { controlsEnabled = v; }

    public Moto(int laneCount) { this(laneCount, 0.15f); }

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
                float[] f = new float[]{1f/8f, 3f/8f, 5f/8f, 7f/8f};
                return new float[]{
                    margem + larguraPista * f[0],
                    margem + larguraPista * f[1],
                    margem + larguraPista * f[2],
                    margem + larguraPista * f[3]
                };
            }
            case 3: {
                float esp = larguraPista / 4f;
                return new float[]{ margem + esp*1f, margem + esp*2f, margem + esp*3f };
            }
            default: {
                float esp = larguraPista / 3f;
                return new float[]{ margem + esp*1f, margem + esp*2f };
            }
        }
    }

    public void update(float delta, float worldSpeed) {
        lanesX = computeLaneCenters(this.laneCount, this.insetFactor);

        // só permite mover no jogo, não no menu
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

        bounds.x = Interpolation.linear.apply(bounds.x, targetX, delta * moveSpeed);

        float effort = IoTInput.getCurrentSpeed();
        float effortBoost = Math.min(0.15f, effort / 400f);
        this.velocidade = worldSpeed * (0.95f + effortBoost);
    }

    public void update(float delta) { update(delta, 0f); }
    public void draw(SpriteBatch batch) { batch.draw(texture, bounds.x, bounds.y, bounds.width, bounds.height); }
    public int getCurrentLaneIndex() { return currentLaneIndex; }
    public Rectangle getBounds() { return bounds; }
    public float getVelocidade() { return this.velocidade; }
    public void dispose() { if (texture != null) texture.dispose(); }
}
