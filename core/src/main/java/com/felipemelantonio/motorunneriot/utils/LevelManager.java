package com.felipemelantonio.motorunneriot.utils;

import com.badlogic.gdx.utils.Array;
import com.felipemelantonio.motorunneriot.entities.Carro;

public class LevelManager {
    private int nivelAtual = 1;
    private float tempoTotal = 0;

    public void update(float delta, Array<Carro> carros) {
        tempoTotal += delta;
        // Aumenta o nível a cada 60 segundos
        if (tempoTotal > 60 && nivelAtual < 3) {
            nivelAtual++;
            tempoTotal = 0;
        }
        // Gera carros conforme o nível
        if (Math.random() < 0.02 * nivelAtual) {
            carros.add(new Carro(nivelAtual));
        }
        // Atualiza posição dos carros
        for (Carro c : carros)
            c.update(delta);
    }

    public int getNivelAtual() {
        return nivelAtual;
    }
}
