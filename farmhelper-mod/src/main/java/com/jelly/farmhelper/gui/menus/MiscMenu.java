package com.jelly.farmhelper.gui.menus;

import com.jelly.farmhelper.gui.components.Toggle;
import gg.essential.elementa.components.UIContainer;
import net.minecraft.client.Minecraft;

public class MiscMenu extends UIContainer {
    private final Minecraft mc = Minecraft.getMinecraft();
    public MiscMenu() {
        new Toggle("Auto GodPot", "autoGodPot").setChildOf(this);
        new Toggle("Auto Cookie", "autoCookie").setChildOf(this);
        new Toggle("Ungrab Mouse", "ungrab").setChildOf(this);
        new Toggle("Debug Mode", "debugMode").setChildOf(this);
        new Toggle("Xray", "xray").setChildOf(this);
        new Toggle("Mute Game", "muteGame").setChildOf(this);
    }
}
