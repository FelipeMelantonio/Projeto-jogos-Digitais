package com.felipemelantonio.motorunneriot.utils;

import java.util.Random;

/**
 * Dificuldade progressiva por fase (sem bursts).
 * Agora com velocidades INICIAIS mais altas em todas as fases.
 */
public class LevelManager {

    private float time;
    private final int fase;
    private final Random rng = new Random();

    public LevelManager(int fase) {
        this.fase = Math.max(1, Math.min(3, fase));
        this.time = 0f;
    }

    public void update(float delta) { time += delta; }

    /** 0→1 suavemente ao longo do tempo. */
    private float ramp(float secondsToMax) {
        return (float)(1.0 - Math.exp(-time / Math.max(1f, secondsToMax)));
    }

    /** Velocidade do mundo (px/s) — bases aumentadas para todas as fases. */
    public float worldSpeedPx() {
        float base, add, secs;
        switch (fase) {
            // antes ~300 → ~700; agora ~420 → ~840 (sobe mais cedo)
            case 1: base = 420f; add = 420f; secs = 35f; break;

            // antes ~340 → ~680; agora ~460 → ~880
            case 2: base = 460f; add = 420f; secs = 50f; break;

            // antes ~400 → ~800; agora ~520 → ~920
            default: base = 520f; add = 400f; secs = 55f; break;
        }
        return base + add * ramp(secs);
    }

    /** Intervalo entre spawns (s) — mantido; opcional: reduza start/min se quiser ainda mais intensidade. */
    public float spawnInterval() {
        float start, min, secs;
        switch (fase) {
            case 1: start = 1.10f; min = 0.45f; secs = 45f; break;
            case 2: start = 0.95f; min = 0.40f; secs = 45f; break;
            default: start = 0.80f; min = 0.32f; secs = 40f; break;
        }
        float t = ramp(secs);
        return Math.max(min, start - (start - min) * t);
    }

    /** Probabilidade de spawn duplo (fase 1 desativado via GameScreen). */
    public float pDouble() {
        switch (fase) {
            case 1: return 0.0f;
            case 2: return 0.22f + 0.26f * ramp(60f);
            default: return 0.30f + 0.32f * ramp(50f);
        }
    }

    /** Gap mínimo na mesma faixa (px). */
    public float laneGapPx() {
        switch (fase) {
            case 1: return 300f - 120f * ramp(80f);
            case 2: return 260f - 120f * ramp(75f);
            default: return 240f - 130f * ramp(70f);
        }
    }

    /** Fator relativo de velocidade dos rivais. */
    public float rivalSpeedFactor() {
        float lo, hi;
        switch (fase) {
            case 1: lo = 1.04f; hi = 1.20f; break;
            case 2: lo = 1.10f; hi = 1.30f; break;
            default: lo = 1.18f; hi = 1.40f; break;
        }
        return lo + rng.nextFloat() * (hi - lo);
    }

    public int getFase() { return fase; }
    public float getTime() { return time; }
}
