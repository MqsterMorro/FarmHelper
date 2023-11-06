package com.github.may2beez.farmhelperv2.mixin.client;

import com.github.may2beez.farmhelperv2.event.MotionUpdateEvent;
import com.github.may2beez.farmhelperv2.feature.impl.Freelock;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityPlayerSP.class, priority = Integer.MAX_VALUE)
public abstract class MixinEntityPlayerSP extends AbstractClientPlayer {
    public MixinEntityPlayerSP(World worldIn, GameProfile playerProfile) {
        super(worldIn, playerProfile);
    }

    @Override
    public void setAngles(float yaw, float pitch) {
        Freelock.getInstance().onRender();

        if (Freelock.getInstance().isRunning()) {
            Freelock.getInstance().setCameraPrevYaw(Freelock.getInstance().getCameraYaw());
            Freelock.getInstance().setCameraPrevPitch(Freelock.getInstance().getCameraPitch());
            Freelock.getInstance().setCameraYaw((float) (Freelock.getInstance().getCameraYaw() + (yaw * 0.15)));
            Freelock.getInstance().setCameraPitch((float) (Freelock.getInstance().getCameraPitch() + (pitch * -0.15)));
            Freelock.getInstance().setCameraPitch(MathHelper.clamp_float(Freelock.getInstance().getCameraPitch(), -90.0F, 90.0F));
        } else {
            super.setAngles(yaw, pitch);
        }
    }

    @Inject(method = "onUpdateWalkingPlayer", at = @At("HEAD"))
    public void onUpdateWalkingPlayer(CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new MotionUpdateEvent.Pre(this.rotationYaw, this.rotationPitch));
    }

    @Inject(method = "onUpdateWalkingPlayer", at = @At("RETURN"))
    public void onUpdateWalkingPlayerReturn(CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new MotionUpdateEvent.Post(this.rotationYaw, this.rotationPitch));
    }
}
