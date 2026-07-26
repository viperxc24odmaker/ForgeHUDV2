package com.makeforge.forgehud;

public class HudColors {

	public static final int[] PRESETS = {
			0xFFFFFFFF, // white
			0xFFFFD966, // gold
			0xFF66FF99, // mint
			0xFF66CCFF, // sky
			0xFFFF7B7B, // coral
			0xFFC28BFF, // violet
			0xFFAAAAAA  // grey
	};

	public static final String[] NAMES = {
			"White", "Gold", "Mint", "Sky", "Coral", "Violet", "Grey"
	};

	public static int current() {
		int index = HudConfig.get().colorIndex;
		if (index < 0 || index >= PRESETS.length) index = 0;
		return PRESETS[index];
	}

	public static String currentName() {
		int index = HudConfig.get().colorIndex;
		if (index < 0 || index >= NAMES.length) index = 0;
		return NAMES[index];
	}

	public static void cycle() {
		HudConfig.get().colorIndex = (HudConfig.get().colorIndex + 1) % PRESETS.length;
	}
}
