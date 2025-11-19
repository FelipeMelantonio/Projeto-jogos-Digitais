package com.felipemelantonio.motorunneriot.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import java.util.Random;

public class Carro {

    // =========================
    // TEXTURAS E SORTEIO
    // =========================

    /**
     * Lista estática que guarda todas as texturas de carros possíveis.
     * É estática porque todas as instâncias de Carro compartilham esse mesmo
     * conjunto
     * de imagens, evitando carregar o mesmo arquivo várias vezes na memória.
     */
    private static Array<Texture> texturasCarros;

    /**
     * Flag de controle para saber se as texturas já foram carregadas.
     * Evita carregamento repetido.
     */
    private static boolean texturasCarregadas = false;

    /**
     * Objeto Random usado para sortear qual modelo de carro será usado
     * em cada instância criada.
     */
    private static final Random random = new Random();

    // =========================
    // ATRIBUTOS DE INSTÂNCIA
    // =========================

    /**
     * Textura específica desse carro (uma das opções em texturasCarros).
     */
    private final Texture texturaCarro;

    /**
     * Retângulo que representa a posição e o tamanho do carro na tela.
     * É usado tanto para desenhar quanto para colisão.
     */
    private final Rectangle bounds;

    /**
     * Velocidade “extra” do carro em pixels por segundo.
     * Além dessa velocidade, ele ainda é somado com a velocidade do mundo
     * (worldSpeed).
     */
    private final float velocidadePx;

    /**
     * Fator de escala da imagem do carro.
     * Define o tamanho do carro na tela em relação ao tamanho original da textura.
     */
    private static final float SCALE = 0.085f;

    /**
     * Guarda as posições X centrais de todas as faixas (lanes) possíveis.
     * Isso permite saber onde desenhar o carro em cada faixa.
     */
    private final float[] lanesX;

    /**
     * Índice da faixa em que esse carro está (0, 1, 2...).
     */
    private final int laneIndex;

    // =========================
    // INICIALIZAÇÃO ESTÁTICA
    // =========================

    /**
     * Método chamado pela GameScreen para garantir que as texturas
     * dos carros estejam carregadas antes de criar qualquer instância.
     */
    public static void initTextureIfNeeded() {
        // Apenas redireciona para o método privado que faz o trabalho real.
        initTexturesIfNeeded();
    }

    /**
     * Carrega as texturas de todos os modelos de carros apenas uma vez.
     * Depois disso, texturasCarregadas é marcado como true.
     */
    private static void initTexturesIfNeeded() {
        if (!texturasCarregadas) {
            texturasCarros = new Array<>();

            // Aqui registramos todas as variações de carros disponíveis.
            texturasCarros.add(new Texture("carro.png"));
            texturasCarros.add(new Texture("carro2.png"));
            texturasCarros.add(new Texture("carro3.png"));
            texturasCarros.add(new Texture("carro4.png"));
            texturasCarros.add(new Texture("carro5.png"));
            texturasCarros.add(new Texture("carro6.png"));
            texturasCarros.add(new Texture("carro7.png"));

            texturasCarregadas = true;
        }
    }

    // =========================
    // CONSTRUTORES
    // =========================

    /**
     * Construtor principal do carro.
     *
     * @param laneIndex         índice da faixa em que o carro vai nascer
     * @param startY            posição inicial em Y (normalmente acima da tela)
     * @param velocidadeCarroPx velocidade “própria” do carro em px/s
     * @param laneCount         quantidade de faixas (2, 3 ou 4)
     * @param insetFactor       porcentagem de margem lateral da pista
     */
    public Carro(int laneIndex,
            float startY,
            float velocidadeCarroPx,
            int laneCount,
            float insetFactor) {

        // Garante que as texturas já foram carregadas.
        initTexturesIfNeeded();

        // Sorteia uma textura aleatória para esse carro específico.
        this.texturaCarro = texturasCarros.random();

        // Pega a largura da tela atual.
        float screenWidth = Gdx.graphics.getWidth();

        // Calcula a margem lateral da pista (para não encostar na borda da tela).
        float margem = screenWidth * insetFactor;

        // Largura “útil” da pista (onde os carros realmente circulam).
        float larguraPista = screenWidth - (margem * 2);

        // =========================
        // CÁLCULO DOS CENTROS DAS FAIXAS
        // =========================
        //
        // A ideia é transformar a largura da pista em várias faixas (lanes),
        // e para cada lane calcular uma coordenada X central.
        //
        // Usamos a mesma lógica de faixas que a Moto, para que tudo fique alinhado.

        float[] centers;
        switch (Math.max(2, laneCount)) { // garante pelo menos 2 faixas
            case 4: {
                // Com 4 faixas, uso frações fixas da pista: 1/8, 3/8, 5/8, 7/8
                float[] frac = new float[] { 1f / 8f, 3f / 8f, 5f / 8f, 7f / 8f };
                centers = new float[] {
                        margem + larguraPista * frac[0],
                        margem + larguraPista * frac[1],
                        margem + larguraPista * frac[2],
                        margem + larguraPista * frac[3]
                };
                break;
            }
            case 3: {
                // Com 3 faixas, divido a pista em 4 partes e uso as posições 1, 2 e 3.
                float esp = larguraPista / 4f;
                centers = new float[] {
                        margem + esp * 1f,
                        margem + esp * 2f,
                        margem + esp * 3f
                };
                break;
            }
            default: {
                // Com 2 faixas, divido a pista em 3 partes e uso 1 e 2.
                float esp = larguraPista / 3f;
                centers = new float[] {
                        margem + esp * 1f,
                        margem + esp * 2f
                };
                break;
            }
        }

        // =========================
        // DIMENSÕES DO CARRO
        // =========================

        // Aplica o SCALE para reduzir ou aumentar o tamanho da textura na tela.
        float width = texturaCarro.getWidth() * SCALE;
        float height = texturaCarro.getHeight() * SCALE;

        // Garante que o laneIndex esteja dentro do intervalo válido [0,
        // centers.length-1].
        this.laneIndex = Math.max(0, Math.min(laneIndex, centers.length - 1));

        // Pega a coordenada X central da faixa escolhida.
        float laneX = centers[this.laneIndex];

        // Guarda o array de centros para referência futura (se precisar).
        this.lanesX = centers;

        // Cria o retângulo de colisão/desenho do carro.
        // Colocamos o X de forma que o centro do retângulo fique alinhado com o centro
        // da faixa.
        this.bounds = new Rectangle(
                laneX - width / 2f, // X (centralizado na faixa)
                startY, // Y inicial (vem de cima)
                width, // largura em tela
                height // altura em tela
        );

        // Define a velocidade mínima para o carro não ficar "parado" em relação ao
        // cenário.
        // Se a velocidade passada for muito baixa, garantimos pelo menos 60 px/s.
        this.velocidadePx = Math.max(60f, velocidadeCarroPx);
    }

    /**
     * Construtor alternativo de compatibilidade.
     * Assume um cenário padrão com 4 faixas e inset 0.15f.
     * Útil quando não queremos passar todos os parâmetros.
     */
    public Carro(int laneIndex, float startY, float velocidadeCarroPx) {
        this(laneIndex, startY, velocidadeCarroPx, 4, 0.15f);
    }

    // =========================
    // ATUALIZAÇÃO E DESENHO
    // =========================

    /**
     * Atualiza a posição do carro ao longo do tempo.
     *
     * @param dt           delta time (tempo entre um frame e outro)
     * @param worldSpeedPx velocidade do "mundo" (velocidade base da fase)
     */
    public void update(float dt, float worldSpeedPx) {
        // O carro desce na tela de acordo com:
        // - a velocidade do mundo (worldSpeedPx)
        // - a velocidade própria do carro (velocidadePx)
        //
        // Somamos as duas velocidades para ter a sensação de fluxo de trânsito.
        bounds.y -= (worldSpeedPx + velocidadePx) * dt;
    }

    /**
     * Desenha o carro na tela usando o SpriteBatch.
     */
    public void draw(SpriteBatch batch) {
        batch.draw(texturaCarro, bounds.x, bounds.y, bounds.width, bounds.height);
    }

    // =========================
    // GETTERS
    // =========================

    /** Retorna o retângulo de colisão/posição do carro. */
    public Rectangle getBounds() {
        return bounds;
    }

    /** Retorna o índice da faixa em que o carro se encontra. */
    public int getLaneIndex() {
        return laneIndex;
    }

    // =========================
    // LIMPEZA DE RECURSOS
    // =========================

    /**
     * Libera as texturas estáticas da memória.
     * Chamado quando o jogo fecha ou quando queremos limpar recursos.
     */
    public static void disposeStatic() {
        if (texturasCarregadas && texturasCarros != null) {
            for (Texture t : texturasCarros) {
                t.dispose();
            }
            texturasCarros.clear();
            texturasCarregadas = false;
        }
    }
}