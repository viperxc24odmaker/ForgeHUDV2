package com.makeforge.forgehud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple JSON config stored at config/forgehud.json.
 * Positions are stored as 0..1 screen fractions so the HUD scales with any resolution / GUI scale.
 */
public class HudConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static HudConfig INSTANCE = new HudConfig();

	/** Bumped when position maths changes so old configs get re-defaulted once. */
	public int configVersion = 0;

	public boolean masterEnabled = true;
	public boolean textShadow = true;

	/** Theme + feel. */
	public int accentIndex = 0;
	/** 0 = no panels behind modules, 1 = subtle, 2 = solid. */
	public int panelStyle = 1;
	public boolean toastsEnabled = true;
	/** Motion FX strength: 0 = off, 1 = light, 2 = medium, 3 = heavy. */
	public int motionIntensity = 2;
	/** Show waypoints from every world instead of only the current one. */
	public boolean waypointsAllWorlds = false;

	/** Index into HudColors.PRESETS. */
	public int colorIndex = 0;

	/** Last death point, remembered between sessions. */
	public boolean hasDeath = false;
	public int deathX = 0;
	public int deathY = 0;
	public int deathZ = 0;
	public String deathDimension = "";
	public Map<String, ModuleData> modules = new LinkedHashMap<>();
	public List<Waypoint> waypoints = new ArrayList<>();
	public boolean waypointDeathMarker = true;

	public static class ModuleData {
		public boolean enabled = true;
		public float x = 0.0F;
		public float y = 0.0F;

		public ModuleData() {
		}

		public ModuleData(boolean enabled, float x, float y) {
			this.enabled = enabled;
			this.x = x;
			this.y = y;
		}
	}

	public static HudConfig get() {
		return INSTANCE;
	}

	public ModuleData module(HudModule module) {
		ModuleData data = modules.get(module.id());
		if (data == null) {
			data = new ModuleData(module.defaultEnabled, module.defaultX, module.defaultY);
			modules.put(module.id(), data);
		}
		return data;
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("forgehud.json");
	}

	public static void load() {
		try {
			Path path = path();
			if (Files.exists(path)) {
				String json = Files.readString(path);
				HudConfig loaded = GSON.fromJson(json, HudConfig.class);
				if (loaded != null) {
					INSTANCE = loaded;
					if (INSTANCE.modules == null) {
						INSTANCE.modules = new LinkedHashMap<>();
					}
					if (INSTANCE.waypoints == null) {
						INSTANCE.waypoints = new ArrayList<>();
					}
				}
			}
		} catch (Exception e) {
			ForgeHudClient.LOGGER.warn("[ForgeHUD] Could not read config, using defaults.", e);
			INSTANCE = new HudConfig();
		}

		// Make sure every module has an entry.
		for (HudModule module : HudModule.values()) {
			INSTANCE.module(module);
		}

		// v2 changed positions from "fraction of free space" to "anchor point", so old
		// coordinates would land in odd places. Re-default them once, then never again.
		if (INSTANCE.configVersion < 2) {
			for (HudModule module : HudModule.values()) {
				ModuleData data = INSTANCE.module(module);
				data.x = module.defaultX;
				data.y = module.defaultY;
			}
			INSTANCE.configVersion = 2;
			ForgeHudClient.LOGGER.info("[ForgeHUD] Migrated HUD positions to the new anchor system.");
		}

		save();
	}

	public static void save() {
		try {
			Path path = path();
			Files.createDirectories(path.getParent());
			Files.writeString(path, GSON.toJson(INSTANCE));
		} catch (IOException e) {
			ForgeHudClient.LOGGER.warn("[ForgeHUD] Could not save config.", e);
		}
	}
}
