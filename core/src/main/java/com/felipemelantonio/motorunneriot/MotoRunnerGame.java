package com.felipemelantonio.motorunneriot;

import com.badlogic.gdx.Game;
import com.felipemelantonio.motorunneriot.screens.MenuScreen;

/**
 * MotoRunnerGame
 * ==============
 * Essa é a classe PRINCIPAL do LibGDX.
 *
 * Ela funciona como o "cérebro do jogo":
 * - controla qual tela (Screen) está ativa
 * - inicializa tudo quando o jogo começa
 *
 * A classe Game já vem do LibGDX e possui:
 * setScreen(tela) → troca para uma nova tela
 * getScreen() → retorna a tela atual
 */
public class MotoRunnerGame extends Game {

    /**
     * create()
     * --------
     * Método chamado automaticamente pelo LibGDX
     * quando o jogo inicia.
     *
     * Aqui você escolhe QUAL TELA será exibida primeiro.
     */
    @Override
    public void create() {

        // Assim que o jogo abre, já vai direto para a tela de MENU.
        // O "this" é a referência do próprio jogo,
        // e é passada para o MenuScreen, pois as telas precisam
        // chamar setScreen() para trocar para outra.
        setScreen(new MenuScreen(this));
    }
}
