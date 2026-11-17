package com.example.winaccel;

import net.fabricmc.api.ClientModInitializer;

public class WinAccelClient implements ClientModInitializer {
    public static boolean ENABLED = true;
    public static double MOUSE_DPI = 1350.0;
    public static double SENSITIVITY_SCALE = 1.0;

    // Windows 11 SmoothMouseXCurve / SmoothMouseYCurve 原生5段折线斜率增量 (400 DPI 基准)
    private static final double S1 = 0.21 / 0.43;
    private static final double DS2 = (1.31 - 0.21) / (1.25 - 0.43) - S1;
    private static final double DS3 = (8.30 - 1.31) / (3.86 - 1.25) - (S1 + DS2);
    private static final double DS4 = (140.0 - 8.30) / (40.0 - 3.86) - (S1 + DS2 + DS3);

    @Override
    public void onInitializeClient() {
        System.out.println("[WinAccel] 26.1 Mouse Speed & Acceleration Restorer Loaded (1350 DPI Mode).");
    }

    public static double calculateWin11Multiplier(double dx, double dy, double dtMs) {
        if (!ENABLED) {
            return 1.0;
        }
        double safeDt = Math.max(dtMs, 0.5);
        double dist = Math.max(Math.hypot(dx, dy), 1e-6);
        double dpiScale = Math.max(MOUSE_DPI / 400.0, 0.1);

        // 将当前 1350 DPI 下的物理速度归一化至 Windows 曲线基准 (counts/ms)
        double vNorm = (dist / safeDt) / dpiScale;
        double vOut = interpolateWinCurve(vNorm);

        double mult = (vOut / vNorm) * SENSITIVITY_SCALE;
        return Math.min(4.5, Math.max(0.3, mult));
    }

    public static double interpolateWinCurve(double v) {
        return S1 * v
             + DS2 * Math.max(0.0, v - 0.43)
             + DS3 * Math.max(0.0, v - 1.25)
             + DS4 * Math.max(0.0, v - 3.86);
    }
}