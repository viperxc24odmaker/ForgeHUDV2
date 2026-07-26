package com.makeforge.forgehud;

public enum HudModule {
	//    short label      group              default on   x     y
	INFO("Coords", HudGroup.WORLD, true, 0.0F, 0.02F),
	NETHER("Nether", HudGroup.WORLD, false, 0.0F, 0.20F),
	LIGHT("Light", HudGroup.WORLD, true, 0.0F, 0.34F),
	DEATH("Death", HudGroup.WORLD, true, 1.0F, 0.42F),

	STATS("Stats", HudGroup.PLAYER, true, 1.0F, 0.30F),
	DURABILITY("Gear", HudGroup.PLAYER, true, 0.0F, 0.46F),
	EFFECTS("Effects", HudGroup.PLAYER, true, 1.0F, 0.02F),
	SPEED("Speed", HudGroup.PLAYER, true, 0.0F, 0.27F),

	TARGET("Target", HudGroup.COMBAT, true, 0.5F, 0.56F),
	CPS("CPS", HudGroup.COMBAT, true, 0.0F, 0.90F),
	KEYSTROKES("Keys", HudGroup.COMBAT, true, 0.0F, 0.74F),

	NETWORK("Ping/TPS", HudGroup.SYSTEM, true, 0.0F, 0.21F),
	SESSION("Session", HudGroup.SYSTEM, false, 1.0F, 0.52F),
	FPS_GRAPH("FPS graph", HudGroup.SYSTEM, false, 1.0F, 0.62F),
	MOTION("Motion FX", HudGroup.SYSTEM, false, 0.5F, 0.5F),

	WAYPOINTS("List", HudGroup.WAYPOINT, true, 1.0F, 0.14F),
	COMPASS("Compass", HudGroup.WAYPOINT, true, 0.5F, 0.02F);

	public final String label;
	public final HudGroup group;
	public final boolean defaultEnabled;
	public final float defaultX;
	public final float defaultY;

	HudModule(String label, HudGroup group, boolean defaultEnabled, float defaultX, float defaultY) {
		this.label = label;
		this.group = group;
		this.defaultEnabled = defaultEnabled;
		this.defaultX = defaultX;
		this.defaultY = defaultY;
	}

	public String id() {
		return name().toLowerCase();
	}
}
