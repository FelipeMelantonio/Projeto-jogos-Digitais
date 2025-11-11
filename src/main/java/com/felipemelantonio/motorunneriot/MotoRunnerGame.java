package com.felipemelantonio.motorunneriot;

import com.badlogic.gdx.Game;
import com.felipemelantonio.motorunneriot.screens.MenuScreen;

public class MotoRunnerGame extends Game {
    @Override
    public void create() {
        setScreen(new MenuScreen(this));
    }
}
