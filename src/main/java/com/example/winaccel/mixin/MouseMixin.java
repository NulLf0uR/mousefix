package com.example.winaccel.mixin;

import com.example.winaccel.WinAccelClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseMixin {

    @Shadow @Final private Minecraft minecraft;
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;

    @Unique
    private long winaccel$lastTurnTimeNs = System.nanoTime();

    @Inject(method = "turnPlayer", at = @At("HEAD"))
    private void winaccel$applyWindowsAcceleration(double movementTime, CallbackInfo ci) {
        long now = System.nanoTime();
        double dtMs = Math.min(50.0, Math.max(0.5, (now - this.winaccel$lastTurnTimeNs) / 1_000_000.0));
        this.winaccel$lastTurnTimeNs = now;

        if (this.minecraft.isWindowActive() && this.minecraft.screen == null) {
            if (this.accumulatedDX != 0.0 || this.accumulatedDY != 0.0) {
                double mult = WinAccelClient.calculateWin11Multiplier(this.accumulatedDX, this.accumulatedDY, dtMs);
                this.accumulatedDX *= mult;
                this.accumulatedDY *= mult;
            }
        }
    }
}
