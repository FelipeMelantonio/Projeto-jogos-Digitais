package com.felipemelantonio.motorunneriot.utils;

import java.util.Random;

/**
 * LevelManager
 * ============
 * Controla a dificuldade da FASE ao longo do tempo.
 *
 * Ele decide:
 * - velocidade da pista (worldSpeed)
 * - intervalo entre spawns de carro
 * - chance de spawn duplo
 * - distância mínima entre carros na mesma faixa
 * - fator de velocidade dos rivais
 *
 * Tudo depende:
 * - da fase (1, 2 ou 3)
 * - do tempo de jogo (time)
 */
public class LevelManager {

    // Quantos segundos o jogador já está vivo nessa fase
    private float time;

    // Fase atual (1, 2 ou 3)
    private final int fase;

    // Gerador de aleatório (usado na velocidade dos rivais)
    private final Random rng = new Random();

    // Construtor: recebe fase e garante que esteja entre 1 e 3
    public LevelManager(int fase) {
        this.fase = Math.max(1, Math.min(3, fase));
        this.time = 0f; // começa com 0 segundos
    }

    // Soma o tempo da fase a cada frame
    public void update(float delta) {
        time += delta;
    }

    /**
     * ramp(secondsToMax)
     * ------------------
     * Transforma o tempo da fase (time) em um valor SUAVE entre 0 e 1.
     *
     * - Quando time é pequeno (início da fase) → ramp ≈ 0
     * - Quando time é grande (após muitos segundos) → ramp ≈ 1
     *
     * O parâmetro secondsToMax DIZ:
     * "aproximadamente em quantos segundos eu quero que isso chegue PERTO de 1".
     *
     * Exemplo mental:
     * - se secondsToMax = 35 → em ~35s a curva já está quase no topo
     * - se secondsToMax = 50 → ela sobe mais devagar, demora mais pra chegar no
     * topo
     */
    private float ramp(float secondsToMax) {
        // Proteção: se alguém passar 0, uso 1 pra não dividir por zero
        float denom = Math.max(1f, secondsToMax);

        // Fórmula:
        // time pequeno → time/denom é pequeno → exp(-algo pequeno) ≈ 1 → 1 - 1 ≈ 0
        // time grande → time/denom grande → exp(-número grande) ≈ 0 → 1 - 0 ≈ 1
        return (float) (1.0 - Math.exp(-time / denom));
    }

    /**
     * worldSpeedPx()
     * --------------
     * Calcula a VELOCIDADE da pista (mundo) em pixels por segundo.
     *
     * Ideia:
     * - Cada fase tem:
     * base = velocidade inicial
     * add = quanto ela PODE crescer ao longo da fase
     * secs = quão rápido essa velocidade cresce (passo da dificuldade)
     *
     * Fórmula final:
     *
     * worldSpeed = base + add * ramp(secs)
     *
     * Lendo isso:
     * - Quando time ≈ 0 → ramp(secs) ≈ 0 → worldSpeed ≈ base
     * - Quando time bem alto → ramp(secs) ≈ 1 → worldSpeed ≈ base + add
     *
     * Sobre o "secs" por fase:
     * - Fase 1: secs = 35f → em ~35s ela já acelerou quase tudo que tinha pra
     * acelerar
     * - Fase 2: secs = 50f → demora MAIS tempo pra chegar no máximo (sobe mais
     * devagar)
     * - Fase 3: secs = 55f → sobe ainda mais devagar, mas começo e topo são mais
     * rápidos
     */
    public float worldSpeedPx() {
        float base; // velocidade de partida da fase
        float add; // quanto essa velocidade ainda pode crescer
        float secs; // "ritmo" da curva de crescimento (quanto maior, mais lenta)

        switch (fase) {
            case 1:
                // FASE 1:
                // - começa em ~420 px/s
                // - se o jogador ficar vivo bastante tempo, pode chegar perto de 420 + 420 =
                // 840 px/s
                base = 420f;
                add = 420f;

                // leva ~35s para QUASE atingir esse máximo (261, 90% etc... não é exato, é uma
                // curva suave)
                // ou seja: a fase 1 acelera mais rápido no começo.
                secs = 35f;
                break;

            case 2:
                // FASE 2:
                // - já começa mais rápida: ~460 px/s
                // - pode crescer até ~460 + 420 = 880 px/s
                base = 460f;
                add = 420f;

                // secs = 50f:
                // - a curva ramp(50f) cresce mais devagar do que ramp(35f)
                // - isso significa que a Fase 2 leva MAIS TEMPO para sair do "meio" e atingir o
                // limite máximo.
                // - resumo: ela já começa rápida, mas não "explode" a dificuldade tão cedo
                // quanto a 1.
                secs = 50f;
                break;

            default: // fase 3
                // FASE 3:
                // - a mais rápida de todas
                // - começa em ~520 px/s
                // - pode chegar perto de ~520 + 400 = 920 px/s
                base = 520f;
                add = 400f;

                // secs = 55f:
                // - cresce ainda mais devagar (leva mais tempo pra chegar perto do máximo)
                // - mas como base e add são altos, mesmo no meio da curva ela já é bem rápida.
                secs = 55f;
                break;
        }

        // Aqui JUNTAMOS TUDO:
        // ramp(secs) sempre devolve um número entre ~0 e ~1.
        //
        // - se time = 0 → ramp = 0 → speed = base + add * 0 = base
        // - se time muito grande → ramp ≈ 1 → speed = base + add * 1 = base + add
        // - se time no meio → ramp = 0.5 (ex) → speed = base + add * 0.5 (meio termo)
        return base + add * ramp(secs);
    }

    /**
     * spawnInterval()
     * ----------------
     * Decide de quanto em quanto tempo UM NOVO CARRO aparece.
     *
     * start = intervalo no começo da fase (bem mais espaçado, fácil)
     * min = intervalo mínimo (quando a fase está bem difícil, mais lotada)
     *
     * Fórmula:
     * intervalo = start - (start - min) * ramp(secs)
     *
     * Ou seja:
     * - quando ramp ≈ 0 → intervalo ≈ start (pouco trânsito)
     * - quando ramp ≈ 1 → intervalo ≈ min (muito trânsito)
     */
    public float spawnInterval() {
        float start, min, secs;

        switch (fase) {
            case 1:
                start = 1.10f; // início: 1 carro a cada 1.10s
                min = 0.45f; // final: pode chegar a 1 carro a cada 0.45s
                secs = 45f;
                break;

            case 2:
                start = 0.95f;
                min = 0.40f;
                secs = 45f;
                break;

            default: // fase 3
                start = 0.80f;
                min = 0.32f;
                secs = 40f;
                break;
        }

        float t = ramp(secs); // entre 0 e 1

        // Calcula o intervalo interpolando entre start e min
        float intervalo = start - (start - min) * t;

        // Garante que nunca fique menor que min
        return Math.max(min, intervalo);
    }

    /**
     * pDouble()
     * ---------
     * Probabilidade de spawn duplo (2 carros na mesma "onda").
     *
     * Fase 1: não tem (0%).
     * Fases 2 e 3: começa mais baixo e aumenta com o tempo usando ramp().
     */
    public float pDouble() {
        switch (fase) {
            case 1:
                return 0.0f;

            case 2:
                // Começa em 22% e pode chegar perto de 22% + 26% = 48%
                return 0.22f + 0.26f * ramp(60f);

            default: // fase 3
                // Começa em 30% e pode chegar perto de 30% + 32% = 62%
                return 0.30f + 0.32f * ramp(50f);
        }
    }

    /**
     * laneGapPx()
     * -----------
     * Distância mínima entre DOIS carros na MESMA faixa (em pixels).
     *
     * Início da fase → nível fácil → gap grande.
     * Fim da fase → nível difícil → gap menor (tudo mais apertado).
     */
    public float laneGapPx() {
        switch (fase) {
            case 1:
                // de 300 px no começo até ~300 - 120 = ~180 px no fim
                return 300f - 120f * ramp(80f);

            case 2:
                // de 260 px até ~260 - 120 = ~140 px
                return 260f - 120f * ramp(75f);

            default:
                // de 240 px até ~240 - 130 = ~110 px
                return 240f - 130f * ramp(70f);
        }
    }

    /**
     * rivalSpeedFactor()
     * -------------------
     * Multiplicador aleatório pra velocidade dos rivais.
     *
     * Exemplo:
     * worldSpeed = 500 px/s
     * factor = 1.2
     * → velocidade do carro rival = 500 * 1.2 = 600 px/s
     *
     * Cada fase aumenta a faixa [lo, hi] pra deixar rivais mais agressivos.
     */
    public float rivalSpeedFactor() {
        float lo, hi;

        switch (fase) {
            case 1:
                lo = 1.04f;
                hi = 1.20f;
                break;

            case 2:
                lo = 1.10f;
                hi = 1.30f;
                break;

            default: // fase 3
                lo = 1.18f;
                hi = 1.40f;
                break;
        }

        // Sorteia um número entre lo e hi
        return lo + rng.nextFloat() * (hi - lo);
    }

    // Getters básicos usados em outras partes do jogo
    public int getFase() {
        return fase;
    }

    public float getTime() {
        return time;
    }
}
