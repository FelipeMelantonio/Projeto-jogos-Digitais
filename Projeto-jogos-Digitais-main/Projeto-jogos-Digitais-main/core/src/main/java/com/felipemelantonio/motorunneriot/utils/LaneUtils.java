package com.felipemelantonio.motorunneriot.utils;

import com.badlogic.gdx.Gdx;

public class LaneUtils {
    // 4 faixas com margens laterais ~15% e pequenas “folgas” nas pontas
    public static float[] getLanesX() {
        float screenWidth = Gdx.graphics.getWidth();
        float margem = screenWidth * 0.15f;
        float larguraPista = screenWidth - (margem * 2f);
        float espacamento = larguraPista / 3f; // 4 faixas = 3 divisões

        return new float[] {
            margem + espacamento * 0f + espacamento * 0.10f,
            margem + espacamento * 1f,
            margem + espacamento * 2f,
            margem + espacamento * 3f - espacamento * 0.10f
        };
    }
}
