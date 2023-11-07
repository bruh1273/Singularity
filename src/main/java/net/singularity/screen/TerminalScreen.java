package net.singularity.screen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public abstract class TerminalScreen extends Screen {

    protected TerminalScreen(Screen previousScreen) {
        super(Text.literal("TerminalScreen"));
    }

}
