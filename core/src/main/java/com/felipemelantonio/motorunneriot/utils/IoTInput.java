package com.felipemelantonio.motorunneriot.utils;

public class IoTInput {
    private static float simulatedSpeed = 0;
    private static float time = 0;

    public static float getCurrentSpeed() {
        // simulação: aumenta e diminui a velocidade
        time += 0.1f;
        simulatedSpeed = 50 + (float) Math.sin(time) * 20;
        return Math.max(0, simulatedSpeed);
    }
}
