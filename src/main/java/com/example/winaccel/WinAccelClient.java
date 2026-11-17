package com.example.winaccel;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class WinAccelClient implements ClientModInitializer {
    public static boolean ENABLED = true;
    public static double MOUSE_DPI = 1350.0;
    // 默认 0.625 对应 Win11 指针速度 10/20 下的真实像素转换比例
    public static double SENSITIVITY_SCALE = 0.625;
    // 甩鼠的加速度曲线陡峭程度调节（觉得加速还是偏快可调低至 0.65，偏慢可调高至 0.95）
    public static double ACCEL_STRENGTH = 0.80;
    // 快速甩鼠时的最高倍率上限
    public static double MAX_MULTIPLIER = 2.2;

    // Windows 11 SmoothMouseXCurve / SmoothMouseYCurve 原生5段控制点斜率
    private static final double S1 = 0.21 / 0.43; // 0.48837
    private static final double DS2 = (1.31 - 0.21) / (1.25 - 0.43) - S1;
    private static final double DS3 = (8.30 - 1.31) / (3.86 - 1.25) - (S1 + DS2);
    private static final double DS4 = (140.0 - 8.30) / (40.0 - 3.86) - (S1 + DS2 + DS3);

    @Override
    public void onInitializeClient() {
        loadOrCreateConfig();
        System.out.println("[WinAccel] Loaded config: DPI=" + MOUSE_DPI
                + ", scale=" + SENSITIVITY_SCALE
                + ", accelStrength=" + ACCEL_STRENGTH
                + ", maxMult=" + MAX_MULTIPLIER);
    }

    public static void loadOrCreateConfig() {
        try {
            Path configPath = FabricLoader.getInstance().getConfigDir().resolve("winaccel.properties");
            Properties props = new Properties();
            if (Files.exists(configPath)) {
                try (InputStream in = Files.newInputStream(configPath)) {
                    props.load(in);
                }
                ENABLED = Boolean.parseBoolean(props.getProperty("enabled", "true"));
                MOUSE_DPI = Double.parseDouble(props.getProperty("mouse_dpi", "1350.0"));
                SENSITIVITY_SCALE = Double.parseDouble(props.getProperty("sensitivity_scale", "0.625"));
                ACCEL_STRENGTH = Double.parseDouble(props.getProperty("accel_strength", "0.80"));
                MAX_MULTIPLIER = Double.parseDouble(props.getProperty("max_multiplier", "2.2"));
            } else {
                props.setProperty("enabled", String.valueOf(ENABLED));
                props.setProperty("mouse_dpi", String.valueOf(MOUSE_DPI));
                props.setProperty("sensitivity_scale", String.valueOf(SENSITIVITY_SCALE));
                props.setProperty("accel_strength", String.valueOf(ACCEL_STRENGTH));
                props.setProperty("max_multiplier", String.valueOf(MAX_MULTIPLIER));
                try (OutputStream out = Files.newOutputStream(configPath)) {
                    props.store(out, "Win11 Mouse Acceleration Config (1350 DPI, Win Speed 10)");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static double calculateWin11Multiplier(double dx, double dy, double dtMs) {
        if (!ENABLED) {
            return 1.0;
        }
        double safeDt = Math.max(dtMs, 1.0);
        double dist = Math.max(Math.hypot(dx, dy), 1e-6);

        // 1350 DPI 归一化至 Windows 曲线基准 + 乘以 ACCEL_STRENGTH 控制进入高速段的快慢
        double dpiScale = Math.max(MOUSE_DPI / 400.0, 0.1);
        double vNorm = ((dist / safeDt) / dpiScale) * ACCEL_STRENGTH;

        double vOut = interpolateWinCurve(vNorm);
        double rawMult = (vOut / Math.max(vNorm, 1e-6)) * SENSITIVITY_SCALE;

        return Math.min(MAX_MULTIPLIER, Math.max(0.3, rawMult));
    }

    public static double interpolateWinCurve(double v) {
        return S1 * v
             + DS2 * Math.max(0.0, v - 0.43)
             + DS3 * Math.max(0.0, v - 1.25)
             + DS4 * Math.max(0.0, v - 3.86);
    }
}
