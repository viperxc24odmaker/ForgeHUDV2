package com.makeforge.forgehud;

import net.minecraft.util.Mth;

/**
 * Shared sizing + auto arrangement.
 *
 * Positions are stored as an ANCHOR POINT (0..1 of the screen), not as a top-left corner.
 * How a module aligns to that point depends on where the point is:
 *   left third   -> the block's left edge is pinned
 *   middle third -> the block is centred on the point
 *   right third  -> the block's RIGHT edge is pinned
 *
 * That is the fix for modules drifting: a right-side block whose text gets wider
 * (99 fps -> 100 fps) now grows leftwards instead of shoving itself across the screen.
 */
public class HudLayout {

	public static int[] size(HudModule module) {
		return switch (module) {
			case INFO -> new int[]{110, 40};
			case NETHER -> new int[]{80, 10};
			case NETWORK -> new int[]{70, 20};
			case SPEED -> new int[]{80, 20};
			case LIGHT -> new int[]{100, 20};
			case STATS -> new int[]{80, 30};
			case DURABILITY -> new int[]{70, 90};
			case EFFECTS -> new int[]{95, 40};
			case TARGET -> new int[]{80, 26};
			case CPS -> new int[]{70, 10};
			case KEYSTROKES -> new int[]{56, 46};
			case SESSION -> new int[]{80, 10};
			case DEATH -> new int[]{95, 20};
			case WAYPOINTS -> new int[]{100, 60};
			case COMPASS -> new int[]{180, 14};
			case FPS_GRAPH -> new int[]{80, 34};
			case MOTION -> new int[]{60, 14};
		};
	}

	/** Left edge for a block of the given width, honouring the anchor alignment. */
	public static int resolveX(float fraction, int screenWidth, int contentWidth) {
		int point = Math.round(fraction * screenWidth);
		int left;
		if (fraction < 0.34F) {
			left = point;
		} else if (fraction > 0.66F) {
			left = point - contentWidth;
		} else {
			left = point - contentWidth / 2;
		}
		return Mth.clamp(left, 2, Math.max(2, screenWidth - contentWidth - 2));
	}

	public static int resolveY(float fraction, int screenHeight, int contentHeight) {
		int top = Math.round(fraction * screenHeight);
		return Mth.clamp(top, 2, Math.max(2, screenHeight - contentHeight - 2));
	}

	/** Snaps near-edge / near-centre drops so hand-placed modules line up cleanly. */
	public static float snap(float fraction) {
		if (fraction < 0.06F) return 0.0F;
		if (fraction > 0.94F) return 1.0F;
		if (fraction > 0.47F && fraction < 0.53F) return 0.5F;
		return Mth.clamp(fraction, 0.0F, 1.0F);
	}

	private static final HudModule[] LEFT_STACK = {
			HudModule.INFO, HudModule.NETHER, HudModule.NETWORK, HudModule.SPEED,
			HudModule.LIGHT, HudModule.STATS, HudModule.DURABILITY
	};

	private static final HudModule[] RIGHT_STACK = {
			HudModule.EFFECTS, HudModule.WAYPOINTS, HudModule.DEATH, HudModule.SESSION, HudModule.FPS_GRAPH
	};

	private static final HudModule[] BOTTOM_LEFT = {
			HudModule.KEYSTROKES, HudModule.CPS
	};

	/**
	 * Lays every enabled module out in tidy non-overlapping stacks for the current screen size.
	 */
	public static void autoArrange(int screenWidth, int screenHeight) {
		HudConfig config = HudConfig.get();
		float gap = 6.0F / Math.max(1, screenHeight);

		float y = 0.02F;
		for (HudModule module : LEFT_STACK) {
			HudConfig.ModuleData data = config.module(module);
			if (!data.enabled) continue;
			data.x = 0.0F;
			data.y = Mth.clamp(y, 0.0F, 0.92F);
			y += (size(module)[1] / (float) Math.max(1, screenHeight)) + gap;
		}

		y = 0.02F;
		for (HudModule module : RIGHT_STACK) {
			HudConfig.ModuleData data = config.module(module);
			if (!data.enabled) continue;
			data.x = 1.0F;
			data.y = Mth.clamp(y, 0.0F, 0.92F);
			y += (size(module)[1] / (float) Math.max(1, screenHeight)) + gap;
		}

		// Bottom left, stacked upwards from just above the hotbar area.
		float bottom = 0.86F;
		for (int i = BOTTOM_LEFT.length - 1; i >= 0; i--) {
			HudModule module = BOTTOM_LEFT[i];
			HudConfig.ModuleData data = config.module(module);
			if (!data.enabled) continue;
			data.x = 0.0F;
			data.y = Mth.clamp(bottom, 0.0F, 0.96F);
			bottom -= (size(module)[1] / (float) Math.max(1, screenHeight)) + gap;
		}

		config.module(HudModule.COMPASS).x = 0.5F;
		config.module(HudModule.COMPASS).y = 0.02F;

		config.module(HudModule.TARGET).x = 0.5F;
		config.module(HudModule.TARGET).y = 0.56F;

		HudConfig.save();
	}
}
