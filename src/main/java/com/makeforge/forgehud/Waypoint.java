package com.makeforge.forgehud;

public class Waypoint {

	public String name = "Waypoint";
	public int x = 0;
	public int y = 64;
	public int z = 0;
	public String dimension = "overworld";
	/** Server address, or "singleplayer". Keeps survival waypoints off other people's servers. */
	public String world = "";
	public int colorIndex = 0;
	public boolean enabled = true;

	/** Recomputed once per frame by the renderer; not persisted. */
	public transient double cachedDistance = 0.0D;

	public Waypoint() {
	}

	public Waypoint(String name, int x, int y, int z, String dimension, String world, int colorIndex) {
		this.name = name;
		this.x = x;
		this.y = y;
		this.z = z;
		this.dimension = dimension;
		this.world = world;
		this.colorIndex = colorIndex;
	}

	public int color() {
		return WaypointColors.color(this.colorIndex);
	}

	public double distanceTo(double px, double pz) {
		double dx = this.x - px;
		double dz = this.z - pz;
		return Math.sqrt(dx * dx + dz * dz);
	}
}
