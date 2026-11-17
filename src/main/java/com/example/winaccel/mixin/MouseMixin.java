package com.example.winaccel.mixin;

import com.example.winaccel.WinAccelClient;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseMixin {

    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;

    @Unique
    private long winaccel$lastTurnTimeNs = System.nanoTime();

    // 保存每次 Math.round 取整后剩下的小数余量，确保慢速微调不丢步且 100% 保持整数步长
    @Unique
    private double winaccel$remDX = 0.0;

    @Unique
    private double winaccel$remDY = 0.0;

    @Inject(method = "turnPlayer", at = @At("HEAD"))
    private void winaccel$applyWindowsAcceleration(double movementTime, CallbackInfo ci) {
        long now = System.nanoTime();
        double dtMs = Math.min(50.0, Math.max(0.5, (now - this.winaccel$lastTurnTimeNs) / 1_000_000.0));
        this.winaccel$lastTurnTimeNs = now;

        if (this.accumulatedDX != 0.0 || this.accumulatedDY != 0.0) {
            double mult = WinAccelClient.calculateWin11Multiplier(this.accumulatedDX, this.accumulatedDY, dtMs);

            double scaledDX = this.accumulatedDX * mult + this.winaccel$remDX;
            double scaledDY = this.accumulatedDY * mult + this.winaccel$remDY;

            // 强制取整为整数硬件计数，100% 匹配原版鼠标灵敏度 GCD 步长
            long intDX = Math.round(scaledDX);
            long intDY = Math.round(scaledDY);

            this.winaccel$remDX = scaledDX - intDX;
            this.winaccel$remDY = scaledDY - intDY;

            this.accumulatedDX = (double) intDX;
            this.accumulatedDY = (double) intDY;
        }
    }
}
