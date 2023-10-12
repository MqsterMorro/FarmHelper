package com.github.may2beez.farmhelperv2.feature.impl;

import cc.polyfrost.oneconfig.utils.Notifications;
import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.feature.IFeature;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.util.LogUtils;
import com.github.may2beez.farmhelperv2.util.RenderUtils;
import com.github.may2beez.farmhelperv2.util.helper.Clock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.awt.*;

public class AutoReconnect implements IFeature {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static AutoReconnect instance;

    public static AutoReconnect getInstance() {
        if (instance == null) {
            instance = new AutoReconnect();
        }
        return instance;
    }

    public enum State {
        NONE,
        CONNECTING,
        LOBBY,
        GARDEN,
    }

    @Getter
    @Setter
    private State state = State.NONE;

    @Setter
    private boolean enabled;

    @Getter
    private final Clock reconnectDelay = new Clock();

    @Override
    public String getName() {
        return "Reconnect";
    }

    @Override
    public boolean isRunning() {
        return enabled;
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return true;
    }

    @Override
    public boolean shouldStartAtMacroStart() {
        return false;
    }

    @Override
    public void start() {
        if (enabled) return;
        enabled = true;
        try {
            mc.getNetHandler().getNetworkManager().closeChannel(new ChatComponentText("Reconnecting in " + LogUtils.formatTime(reconnectDelay.getRemainingTime())));
        } catch (Exception e) {
            e.printStackTrace();
        }
        state = State.CONNECTING;
        LogUtils.sendDebug("[Reconnect] Reconnecting to server...");
        if (MacroHandler.getInstance().isMacroToggled())
            MacroHandler.getInstance().pauseMacro();
    }

    @Override
    public void stop() {
        if (!enabled) return;
        enabled = false;
        state = State.NONE;
        reconnectDelay.reset();
        LogUtils.sendDebug("[Reconnect] Finished reconnecting to server!");
        if (MacroHandler.getInstance().isMacroToggled()) {
            UngrabMouse.getInstance().regrabMouse();
            UngrabMouse.getInstance().ungrabMouse();
            MacroHandler.getInstance().resumeMacro();
        }
    }

    @Override
    public void resetStatesAfterMacroDisabled() {

    }

    @Override
    public boolean isToggled() {
        return FarmHelperConfig.autoReconnect;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        if (!isRunning()) return;

        if (reconnectDelay.isScheduled() && !reconnectDelay.passed()) return;

        switch (state) {
            case NONE:
                break;
            case CONNECTING:
                System.out.println("Reconnecting to server... Waiting " + LogUtils.formatTime(reconnectDelay.getRemainingTime()) + " before connecting.");
                if (reconnectDelay.passed()) {
                    try {
                        FMLClientHandler.instance().connectToServer(new GuiMultiplayer(new GuiMainMenu()), new ServerData("bozo", GameStateHandler.getInstance().getServerIP() != null ? GameStateHandler.getInstance().getServerIP() : "mc.hypixel.net", false));
                        setState(AutoReconnect.State.LOBBY);
                        reconnectDelay.schedule(7_500);
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("Failed to reconnect to server! Trying again in 5 seconds...");
                        Notifications.INSTANCE.send("Farm Helper", "Failed to reconnect to server! Trying again in 5 seconds...");
                        reconnectDelay.schedule(5_000);
                        start();
                    }
                }
                break;
            case LOBBY:
                if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.LOBBY || !GameStateHandler.getInstance().inGarden()) {
                    System.out.println("Reconnected to lobby!");
                    LogUtils.sendDebug("[Reconnect] Came back to lobby!");
                    mc.thePlayer.sendChatMessage("/skyblock");
                    state = State.GARDEN;
                    reconnectDelay.schedule(5_000);
                }
                break;
            case GARDEN:
                if (GameStateHandler.getInstance().inGarden()) {
                    System.out.println("Reconnected to garden!");
                    LogUtils.sendDebug("[Reconnect] Came back to garden!");
                    stop();
                } else {
                    mc.thePlayer.sendChatMessage("/warp garden");
                    reconnectDelay.schedule(5_000);
                }
                break;
        }
    }

    @SubscribeEvent
    public void onKeyPress(GuiScreenEvent.KeyboardInputEvent event) {
        if (!isRunning()) return;
        if (!(mc.currentScreen instanceof GuiDisconnected)) return;

        if (Keyboard.getEventKey() == Keyboard.KEY_ESCAPE) {
            stop();
            mc.displayGuiScreen(new GuiMainMenu());
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (!isRunning()) return;
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;

        String text = "Delay: " + String.format("%.1f", reconnectDelay.getRemainingTime() / 1000.0) + "s";
        RenderUtils.drawCenterTopText(text, event, Color.RED);
    }
}