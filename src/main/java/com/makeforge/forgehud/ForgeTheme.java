package com.makeforge.forgehud;

import net.minecraft.client.gui.GuiGraphics;

/**
 * The look of ForgeHUD: accent colours, panels, pills, dividers.
 * Everything is fill() rectangles - no textures, no nine-slice sprites, no blit calls.
 */
public class ForgeTheme {

	public static final int[] ACCENTS = {
			0xFF5AC8FA, // ice
			0xFF9B7BFF, // violet
			0xFF4CE080, // lime
			0xFFFF7A45, // ember
			0xFFFF4D6D, // rose
			0xFFFFC53D  // amber
	};

	public static final String[] ACCENT_NAMES = {
			"Ice", "Violet", "Lime", "Ember", "Rose", "Amber"
	};

	public static final int BACKDROP = 0xE60D1017;
	public static final int PANEL = 0xF0141A24;
	public static final int PANEL_EDGE = 0xFF232C3A;
	public static final int ROW = 0xFF19202C;
	public static final int ROW_HOVER = 0xFF232E3E;
	public static final int TEXT = 0xFFE6EDF5;
	public static final int TEXT_DIM = 0xFF7C8899;
	public static final int OFF = 0xFF44505F;

	public static int accent() {
		int index = HudConfig.get().accentIndex;
		if (index < 0 || index >= ACCENTS.length) index = 0;
		return ACCENTS[index];
	}

	public static String accentName() {
		int index = HudConfig.get().accentIndex;
		if (index < 0 || index >= ACCENT_NAMES.length) index = 0;
		return ACCENT_NAMES[index];
	}

	public static void cycleAccent() {
		HudConfig.get().accentIndex = (HudConfig.get().accentIndex + 1) % ACCENTS.length;
	}

	/** Panel with clipped corners, so it reads as a designed box instead of a plain rectangle. */
	public static void panel(GuiGraphics graphics, int x, int y, int width, int height, int fill, int edge) {
		int right = x + width;
		int bottom = y + height;

		graphics.fill(x + 2, y, right - 2, bottom, fill);
		graphics.fill(x, y + 2, x + 2, bottom - 2, fill);
		graphics.fill(right - 2, y + 2, right, bottom - 2, fill);

		graphics.fill(x + 2, y, right - 2, y + 1, edge);
		graphics.fill(x + 2, bottom - 1, right - 2, bottom, edge);
		graphics.fill(x, y + 2, x + 1, bottom - 2, edge);
		graphics.fill(right - 1, y + 2, right, bottom - 2, edge);
		graphics.fill(x + 1, y + 1, x + 2, y + 2, edge);
		graphics.fill(right - 2, y + 1, right - 1, y + 2, edge);
		graphics.fill(x + 1, bottom - 2, x + 2, bottom - 1, edge);
		graphics.fill(right - 2, bottom - 2, right - 1, bottom - 1, edge);
	}

	public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
		panel(graphics, x, y, width, height, PANEL, PANEL_EDGE);
	}

	/** Vertical accent bar used on the left of rows and HUD panels. */
	public static void accentBar(GuiGraphics graphics, int x, int y, int height, int color) {
		graphics.fill(x, y, x + 2, y + height, color);
	}

	public static void divider(GuiGraphics graphics, int x, int y, int width) {
		graphics.fill(x, y, x + width, y + 1, PANEL_EDGE);
	}

	/** Small ON/OFF pill. */
	public static void pill(GuiGraphics graphics, int x, int y, boolean on) {
		int width = 16;
		int height = 8;
		graphics.fill(x, y, x + width, y + height, on ? withAlpha(accent(), 0x88) : 0xFF2A323E);
		int knobX = on ? x + width - 7 : x + 1;
		graphics.fill(knobX, y + 1, knobX + 6, y + height - 1, on ? accent() : OFF);
	}

	public static int withAlpha(int color, int alpha) {
		return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
	}

	/** Panel opacity behind HUD modules: 0 = off, 1 = subtle, 2 = solid. */
	public static int hudPanelColor() {
		return switch (HudConfig.get().panelStyle) {
			case 1 -> 0x40000000;
			case 2 -> 0x99070B12;
			default -> 0;
		};
	}
}
