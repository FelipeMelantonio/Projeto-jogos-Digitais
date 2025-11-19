package com.felipemelantonio.motorunneriot.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.felipemelantonio.motorunneriot.MotoRunnerGame;

/**
 * Lwjgl3Launcher
 * ===============
 * Essa classe é responsável por INICIAR o jogo na versão DESKTOP
 * (Windows/Linux/Mac).
 *
 * O Android e o HTML5 têm seus próprios "launchers".
 *
 * O desktop usa o backend LWJGL3 do LibGDX, por isso esse nome.
 */
public class Lwjgl3Launcher {

    public static void main(String[] args) {

        // Essa linha cria uma nova JVM se preciso (suporte a macOS e Windows).
        // Se ela retornar true, significa que já abriu uma nova JVM e não deve
        // continuar.
        if (StartupHelper.startNewJvmIfRequired())
            return;

        // Cria a instância da aplicação e inicia o jogo
        createApplication();
    }

    /** Cria a aplicação desktop do LibGDX */
    private static Lwjgl3Application createApplication() {

        // Aqui iniciamos o jogo MotoRunnerGame,
        // passando a configuração definida ali embaixo.
        return new Lwjgl3Application(new MotoRunnerGame(), getDefaultConfiguration());
    }

    /** Configurações de janela/video da versão desktop */
    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {

        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();

        // Nome da janela do jogo
        configuration.setTitle("MotoRunnerIoT");

        // Ativa Vsync → impede que o jogo rode a FPS absurda e evita screen tearing
        configuration.useVsync(true);

        // Ajusta o FPS máximo baseado no monitor (opcional, usado como fallback)
        configuration.setForegroundFPS(
                Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);

        // Deixa o jogo em tela cheia (full screen)
        configuration.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());

        // Ícones do jogo (para barra de tarefas e janela)
        configuration.setWindowIcon(
                "libgdx128.png",
                "libgdx64.png",
                "libgdx32.png",
                "libgdx16.png");

        return configuration;
    }
}
