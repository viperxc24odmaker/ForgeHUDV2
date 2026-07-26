package com.makeforge.forgehud;

public class WaypointColors {

	public static final int[] COLORS = {
			0xFF55FF55, // green
			0xFF55FFFF, // aqua
			0xFFFFFF55, // yellow
			0xFFFF5555, // red
			0xFFFF55FF, // pink
			0xFF5599FF, // blue
			0xFFFFAA00, // orange
			0xFFFFFFFF  // white
	};

	public static final String[] NAMES = {
			"Green", "Aqua", "Yellow", "Red", "Pink", "Blue", "Orange", "White"
	};

	public static int color(int index) {
		if (index < 0 || index >= COLORS.length) index = 0;
		return COLORS[index];
	}

	public static String name(int index) {
		if (index < 0 || index >= NAMES.length) index = 0;
		return NAMES[index];
	}
}
