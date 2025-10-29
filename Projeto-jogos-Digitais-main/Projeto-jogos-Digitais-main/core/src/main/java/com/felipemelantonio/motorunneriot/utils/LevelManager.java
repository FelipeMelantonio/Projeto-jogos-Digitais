package com.felipemelantonio.motorunneriot.utils;

import java.util.Random;

/**
 * Controla a dificuldade do jogo com base no tempo de jogo,
 * incluindo ondas (bursts), velocidade crescente e spawn adaptativo.
 */
public class LevelManager {

    private float time;              // tempo total de jogo
    private float lastBurstTime;     // tempo desde o último burst
    private boolean inBurst;         // se está dentro de uma onda intensa
    private float burstDuration;     // quanto dura o burst atual
    private Random random;

    public LevelManager() {
        time = 0f;
        lastBurstTime = 0f;
        inBurst = false;
        burstDuration = 0f;
        random = new Random();
    }

    public void update(float delta) {
        time += delta;
        lastBurstTime += delta;

        // A cada 10–15s, chance de burst (fase difícil)
        if (!inBurst && lastBurstTime > 10f && random.nextFloat() < 0.25f) {
            inBurst = true;
            burstDuration = 4f + random.nextFloat() * 3f; // burst dura 4–7s
            lastBurstTime = 0f;
        }

        // Sai do burst depois que o tempo passa
        if (inBurst && lastBurstTime > burstDuration) {
            inBurst = false;
            lastBurstTime = 0f;
        }
    }

    public float getTime() { return time; }

    public boolean isBurst() { return inBurst; }

    // ======= CURVAS DE DIFICULDADE =======

    /** Velocidade do mundo em px/s (cresce suave e estabiliza) */
    public float worldSpeedPx() {
        float base = 280f;
        float bonus = (float)(220f * (1 - Math.exp(-time / 60f)));
        float burstBonus = inBurst ? 80f : 0f;
        return base + bonus + burstBonus;
    }

    /** Intervalo entre spawns, decai de 1.6s para ~0.5s */
    public float spawnInterval() {
        float base = 1.6f - (float)(1.05f * (1 - Math.exp(-time / 50f)));
        if (inBurst) base *= 0.5f; // dobra a frequência
        return Math.max(0.4f, base);
    }

    /** Probabilidade de spawn duplo (0.1 → 0.45) */
    public float pDouble() {
        float base = 0.10f + 0.35f * clamp(time / 120f);
        if (inBurst) base += 0.25f;
        return Math.min(0.7f, base);
    }

    /** Gap mínimo entre carros na mesma faixa (260→140) */
    public float laneGapPx() {
        float base = 260f - 120f * clamp(time / 180f);
        if (inBurst) base *= 0.7f;
        return base;
    }

    /** Fator aleatório de velocidade de cada carro */
    public float rivalSpeedFactor() {
        return 0.85f + random.nextFloat() * 0.25f;
    }

    private float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
}
