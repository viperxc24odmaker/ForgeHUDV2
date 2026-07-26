package com.makeforge.forgehud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayDeque;
import java.util.Deque;

/** Custom drawn notifications, so ForgeHUD never has to borrow the vanilla toast look. */
public class ForgeToasts {

	private static final long LIFETIME = 2600L;
	private static final Deque<Toast> TOASTS = new ArrayDeque<>();

	private static class Toast {
		final String text;
		final long born;

		Toast(String text) {
			this.text = text;
			this.born = System.currentTimeMillis();
		}
	}

	public static void show(String text) {
		if (!HudConfig.get().toastsEnabled) return;
		if (TOASTS.size() > 4) TOASTS.pollFirst();
		TOASTS.addLast(new Toast(text));
	}

	public static void render(GuiGraphics graphics, Minecraft client, int screenWidth) {
		if (TOASTS.isEmpty()) return;

		long now = System.currentTimeMillis();
		while (!TOASTS.isEmpty() && now - TOASTS.peekFirst().born > LIFETIME) {
			TOASTS.pollFirst();
		}

		int y = 6;
		for (Toast toast : TOASTS) {
			long age = now - toast.born;
			int width = client.font.width(toast.text) + 18;
			int slide = age < 200L ? (int) ((200L - age) / 12L) : 0;
			int x = screenWidth - width - 6 + slide;

			ForgeTheme.panel(graphics, x, y, width, 16, 0xE6141A24, ForgeTheme.PANEL_EDGE);
			ForgeTheme.accentBar(graphics, x + 3, y + 3, 10, ForgeTheme.accent());
			graphics.drawString(client.font, toast.text, x + 9, y + 4, ForgeTheme.TEXT, false);

			// Drain bar showing the remaining time.
			int drain = (int) ((width - 6) * (1.0F - (age / (float) LIFETIME)));
			graphics.fill(x + 3, y + 14, x + 3 + Math.max(0, drain), y + 15, ForgeTheme.withAlpha(ForgeTheme.accent(), 0xAA));

			y += 20;
		}
	}

	public static void clear() {
		TOASTS.clear();
	}
}
