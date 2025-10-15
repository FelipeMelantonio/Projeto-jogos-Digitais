package com.felipemelantonio.motorunneriot.entities;


import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Background {
    private Texture texture;
    private float y1, y2;
    private float speed;
    private float height;

    public Background() {
        texture = new Texture("estrada.png");
        height = texture.getHeight();
        y1 = 0;
        y2 = height;
        speed = 200f;
    }

    public void update(float delta) {
        y1 -= speed * delta;
        y2 -= speed * delta;

        if (y1 + height < 0) y1 = y2 + height;
        if (y2 + height < 0) y2 = y1 + height;
    }

    public void draw(SpriteBatch batch, float screenWidth, float screenHeight) {
        float scale = screenWidth / texture.getWidth();
        batch.draw(texture, 0, y1, screenWidth, texture.getHeight() * scale);
        batch.draw(texture, 0, y2, screenWidth, texture.getHeight() * scale);
    }

    public void dispose() {
        texture.dispose();
    }
}
