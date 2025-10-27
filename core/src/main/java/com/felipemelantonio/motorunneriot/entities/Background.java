package com.felipemelantonio.motorunneriot.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/** Fundo com scroll vertical contínuo (tileado), sem jitter ao escalar. */
public class Background {
    private final Texture texture;
    private float speedPx;     // px/s
    private float tileHeight;  // ALTURA já escalada no tamanho da tela
    private float scale;       // escala largura-tela / largura-textura
    private float scroll;      // 0..tileHeight

    public Background() {
        this("estrada.png", 220f);
    }

    public Background(String fileName, float speedPx) {
        this.texture = new Texture(fileName);
        // filtro linear evita serrilhado e “tremido” durante o scroll
        this.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        this.speedPx = speedPx;
        recalcTileSize();
    }

    /** Recalcula altura do tile já com a escala para a largura da tela. */
    private void recalcTileSize() {
        float screenW = Gdx.graphics.getWidth();
        scale = screenW / (float) texture.getWidth();
        tileHeight = texture.getHeight() * scale;
        // mantém scroll no intervalo válido ao mudar de tamanho
        if (tileHeight > 0f) scroll = (scroll % tileHeight + tileHeight) % tileHeight;
    }

    /** Chame em GameScreen.resize(w,h) */
    public void onResize() { recalcTileSize(); }

    public void setSpeed(float speedPx) { this.speedPx = speedPx; }

    public void update(float dt) {
        scroll += speedPx * dt;
        if (scroll >= tileHeight) scroll -= tileHeight; // wrap perfeito
    }

    public void draw(SpriteBatch batch) {
        float screenW = Gdx.graphics.getWidth();

        // desenha 2 tiles (3º opcional se quiser margem de segurança)
        float y0 = -scroll;
        batch.draw(texture, 0, y0,           screenW, tileHeight);
        batch.draw(texture, 0, y0 + tileHeight, screenW, tileHeight);
    }

    public void dispose() { texture.dispose(); }
}
