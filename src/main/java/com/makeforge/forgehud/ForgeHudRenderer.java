package com.makeforge.forgehud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws every ForgeHUD module. Everything goes through GuiGraphics only - no raw GL calls,
 * no custom framebuffers - which is what keeps this safe under VulkanMod.
 */
public class ForgeHudRenderer {

	private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GRAY = 0xFFAAAAAA;
	private static final int WARN = 0xFFFF5555;

	// Frame caches: these used to be recomputed (and re-sorted) per module, per frame.
	private static List<Waypoint> frameWaypoints = new ArrayList<>();
	private static long cachedBiomePos = Long.MIN_VALUE;
	private static String cachedBiome = "unknown";

	public static void render(GuiGraphics graphics) {
		HudTrackers.countFrame();

		Minecraft client = Minecraft.getInstance();
		HudConfig config = HudConfig.get();

		if (!config.masterEnabled) return;
		if (client.player == null || client.level == null) return;
		if (client.options != null && client.options.hideGui) return;
		if (client.screen != null && !(client.screen instanceof HudEditorScreen)) return;

		int screenWidth = client.getWindow().getGuiScaledWidth();
		int screenHeight = client.getWindow().getGuiScaledHeight();

		// Sorted once per frame, shared by the list and the compass.
		boolean needWaypoints = config.module(HudModule.WAYPOINTS).enabled
				|| config.module(HudModule.COMPASS).enabled;
		frameWaypoints = needWaypoints ? buildWaypoints(client) : java.util.Collections.emptyList();

		drawModule(graphics, client, HudModule.INFO, screenWidth, screenHeight, infoLines(client));
		drawModule(graphics, client, HudModule.NETHER, screenWidth, screenHeight, netherLines(client));
		drawModule(graphics, client, HudModule.NETWORK, screenWidth, screenHeight, networkLines(client));
		drawModule(graphics, client, HudModule.SPEED, screenWidth, screenHeight, speedLines());
		drawModule(graphics, client, HudModule.STATS, screenWidth, screenHeight, statLines(client));
		drawModule(graphics, client, HudModule.EFFECTS, screenWidth, screenHeight, effectLines(client));
		drawModule(graphics, client, HudModule.SESSION, screenWidth, screenHeight, sessionLines());
		drawModule(graphics, client, HudModule.DEATH, screenWidth, screenHeight, deathLines(client));
		drawModule(graphics, client, HudModule.CPS, screenWidth, screenHeight, cpsLines());

		if (config.module(HudModule.LIGHT).enabled) {
			drawLight(graphics, client, screenWidth, screenHeight);
		}
		if (config.module(HudModule.DURABILITY).enabled) {
			drawDurability(graphics, client, screenWidth, screenHeight);
		}
		if (config.module(HudModule.KEYSTROKES).enabled) {
			drawKeystrokes(graphics, client, screenWidth, screenHeight);
		}
		if (config.module(HudModule.TARGET).enabled) {
			drawTarget(graphics, client, screenWidth, screenHeight);
		}
		if (config.module(HudModule.WAYPOINTS).enabled) {
			drawWaypointList(graphics, client, screenWidth, screenHeight);
		}
		if (config.module(HudModule.COMPASS).enabled) {
			drawCompass(graphics, client, screenWidth, screenHeight);
		}
		if (config.module(HudModule.FPS_GRAPH).enabled) {
			drawFpsGraph(graphics, client, screenWidth, screenHeight);
		}
		if (config.module(HudModule.MOTION).enabled) {
			drawMotionFx(graphics, client, screenWidth, screenHeight);
		}

		ForgeToasts.render(graphics, client, screenWidth);
	}

	// ------------------------------------------------------------------ text modules

	private static List<String> infoLines(Minecraft client) {
		LocalPlayer player = client.player;
		List<String> lines = new ArrayList<>();

		BlockPos pos = player.blockPosition();
		lines.add("XYZ " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
		lines.add(facing(player) + "  |  " + HudTrackers.fps() + " fps");

		lines.add(biome(client, pos));

		long dayTime = client.level.getDayTime();
		long day = dayTime / 24000L;
		lines.add("Day " + day + "  |  " + LocalTime.now().format(CLOCK));
		return lines;
	}

	private static List<String> netherLines(Minecraft client) {
		List<String> lines = new ArrayList<>();
		try {
			BlockPos pos = client.player.blockPosition();
			String dimension = client.level.dimension().identifier().getPath();
			if (dimension.contains("nether")) {
				lines.add("Overworld " + (pos.getX() * 8) + " " + (pos.getZ() * 8));
			} else {
				lines.add("Nether " + (pos.getX() / 8) + " " + (pos.getZ() / 8));
			}
		} catch (Throwable ignored) {
		}
		return lines;
	}

	private static List<String> networkLines(Minecraft client) {
		List<String> lines = new ArrayList<>();
		int ping = HudTrackers.ping(client);
		lines.add("Ping " + (ping > 0 ? ping + " ms" : "local"));
		lines.add(String.format("TPS %.1f", HudTrackers.tps()));
		return lines;
	}

	private static List<String> speedLines() {
		List<String> lines = new ArrayList<>();
		lines.add(String.format("Speed %.2f b/s", HudTrackers.horizontalSpeed()));
		double vertical = HudTrackers.verticalSpeed();
		if (Math.abs(vertical) > 0.05D) {
			lines.add(String.format("Vert %+.2f b/s", vertical));
		}
		return lines;
	}

	private static List<String> statLines(Minecraft client) {
		List<String> lines = new ArrayList<>();
		try {
			LocalPlayer player = client.player;
			lines.add("Armor " + player.getArmorValue());
			lines.add(String.format("Saturation %.1f", player.getFoodData().getSaturationLevel()));
			int air = player.getAirSupply();
			if (air < player.getMaxAirSupply()) {
				lines.add("Air " + (air / 20) + "s");
			}
		} catch (Throwable ignored) {
		}
		return lines;
	}

	private static List<String> sessionLines() {
		long millis = HudTrackers.sessionMillis();
		long seconds = millis / 1000L;
		return List.of(String.format("Session %d:%02d:%02d", seconds / 3600L, (seconds % 3600L) / 60L, seconds % 60L));
	}

	private static List<String> deathLines(Minecraft client) {
		HudConfig config = HudConfig.get();
		if (!config.hasDeath) return List.of();

		List<String> lines = new ArrayList<>();
		lines.add("Death " + config.deathX + " " + config.deathY + " " + config.deathZ);
		try {
			String dimension = client.level.dimension().identifier().getPath();
			if (dimension.equals(config.deathDimension)) {
				double dx = client.player.getX() - config.deathX;
				double dz = client.player.getZ() - config.deathZ;
				lines.add(String.format("%.0f blocks away", Math.sqrt(dx * dx + dz * dz)));
			} else {
				lines.add("in " + config.deathDimension.replace('_', ' '));
			}
		} catch (Throwable ignored) {
		}
		return lines;
	}

	private static List<String> cpsLines() {
		return List.of("CPS " + HudTrackers.leftCps() + " | " + HudTrackers.rightCps());
	}

	private static List<String> effectLines(Minecraft client) {
		List<String> lines = new ArrayList<>();
		try {
			for (MobEffectInstance instance : client.player.getActiveEffects()) {
				String name = instance.getEffect().value().getDisplayName().getString();
				int amplifier = instance.getAmplifier();
				if (amplifier > 0) {
					name = name + " " + roman(amplifier + 1);
				}
				lines.add(name + " " + formatTicks(instance.getDuration()));
			}
		} catch (Throwable ignored) {
		}
		return lines;
	}

	private static void drawModule(GuiGraphics graphics, Minecraft client, HudModule module,
	                               int screenWidth, int screenHeight, List<String> lines) {
		if (!HudConfig.get().module(module).enabled || lines.isEmpty()) return;

		Font font = client.font;
		int width = 0;
		for (String line : lines) {
			width = Math.max(width, font.width(line));
		}
		int height = lines.size() * 10;

		int x = anchorX(module, screenWidth, width);
		int y = anchorY(module, screenHeight, height);

		int panelColor = ForgeTheme.hudPanelColor();
		if (panelColor != 0) {
			graphics.fill(x - 4, y - 2, x + width + 3, y + height + 1, panelColor);
			ForgeTheme.accentBar(graphics, x - 4, y - 2, height + 3,
					ForgeTheme.withAlpha(ForgeTheme.accent(), 0xCC));
		}

		boolean shadow = HudConfig.get().textShadow;
		int color = HudColors.current();
		boolean rightAligned = isRightAnchored(module);
		int drawY = y;
		for (String line : lines) {
			int lineX = rightAligned ? x + width - font.width(line) : x;
			graphics.drawString(font, line, lineX, drawY, color, shadow);
			drawY += 10;
		}
	}

	// ------------------------------------------------------------------ light level

	private static void drawLight(GuiGraphics graphics, Minecraft client, int screenWidth, int screenHeight) {
		int block;
		int sky;
		try {
			BlockPos pos = client.player.blockPosition();
			block = client.level.getBrightness(LightLayer.BLOCK, pos);
			sky = client.level.getBrightness(LightLayer.SKY, pos);
		} catch (Throwable ignored) {
			return;
		}

		List<String> lines = new ArrayList<>();
		lines.add("Light " + block + " block / " + sky + " sky");
		boolean spawnable = block <= 0;
		if (spawnable) {
			lines.add("MOBS CAN SPAWN");
		}

		Font font = client.font;
		int width = 0;
		for (String line : lines) {
			width = Math.max(width, font.width(line));
		}
		int x = anchorX(HudModule.LIGHT, screenWidth, width);
		int y = anchorY(HudModule.LIGHT, screenHeight, lines.size() * 10);

		boolean shadow = HudConfig.get().textShadow;
		graphics.drawString(font, lines.get(0), x, y, HudColors.current(), shadow);
		if (spawnable) {
			graphics.drawString(font, lines.get(1), x, y + 10, WARN, shadow);
		}
	}

	// ------------------------------------------------------------------ durability

	private static void drawDurability(GuiGraphics graphics, Minecraft client, int screenWidth, int screenHeight) {
		LocalPlayer player = client.player;
		Font font = client.font;

		List<ItemStack> stacks = new ArrayList<>();
		stacks.add(player.getItemBySlot(EquipmentSlot.HEAD));
		stacks.add(player.getItemBySlot(EquipmentSlot.CHEST));
		stacks.add(player.getItemBySlot(EquipmentSlot.LEGS));
		stacks.add(player.getItemBySlot(EquipmentSlot.FEET));
		stacks.add(player.getMainHandItem());
		stacks.add(player.getOffhandItem());

		List<ItemStack> shown = new ArrayList<>();
		for (ItemStack stack : stacks) {
			if (!stack.isEmpty() && stack.isDamageableItem()) {
				shown.add(stack);
			}
		}
		if (shown.isEmpty()) return;

		int height = shown.size() * 18;
		int x = anchorX(HudModule.DURABILITY, screenWidth, 60);
		int y = anchorY(HudModule.DURABILITY, screenHeight, height);

		boolean shadow = HudConfig.get().textShadow;
		for (ItemStack stack : shown) {
			int max = Math.max(1, stack.getMaxDamage());
			int remaining = max - stack.getDamageValue();
			float ratio = (float) remaining / (float) max;

			int color;
			try {
				color = 0xFF000000 | stack.getBarColor();
			} catch (Throwable t) {
				color = WHITE;
			}
			try {
				graphics.renderItem(stack, x, y);
			} catch (Throwable ignored) {
			}

			String text = remaining + (ratio <= 0.1F ? " !" : "");
			graphics.drawString(font, text, x + 20, y + 1, color, shadow);
			graphics.drawString(font, Math.round(ratio * 100.0F) + "%", x + 20, y + 10, GRAY, shadow);
			y += 18;
		}
	}

	// ------------------------------------------------------------------ keystrokes

	private static void drawKeystrokes(GuiGraphics graphics, Minecraft client, int screenWidth, int screenHeight) {
		int box = 16;
		int gap = 2;
		int width = box * 3 + gap * 2;
		int height = box * 2 + gap * 3 + 8 + 12;

		int x = anchorX(HudModule.KEYSTROKES, screenWidth, width);
		int y = anchorY(HudModule.KEYSTROKES, screenHeight, height);

		var options = client.options;
		int half = (width - gap) / 2;

		key(graphics, client, x + box + gap, y, box, box, "W", options.keyUp.isDown());
		key(graphics, client, x, y + box + gap, box, box, "A", options.keyLeft.isDown());
		key(graphics, client, x + box + gap, y + box + gap, box, box, "S", options.keyDown.isDown());
		key(graphics, client, x + (box + gap) * 2, y + box + gap, box, box, "D", options.keyRight.isDown());

		int row = y + (box + gap) * 2;
		key(graphics, client, x, row, half, 10, "LMB", options.keyAttack.isDown());
		key(graphics, client, x + half + gap, row, half, 10, "RMB", options.keyUse.isDown());

		row += 12;
		key(graphics, client, x, row, half, 8, "SHIFT", options.keyShift.isDown());
		key(graphics, client, x + half + gap, row, half, 8, "SPACE", options.keyJump.isDown());
	}

	private static void key(GuiGraphics graphics, Minecraft client, int x, int y, int w, int h,
	                        String label, boolean pressed) {
		graphics.fill(x, y, x + w, y + h, pressed ? 0xC0FFFFFF : 0x60000000);
		Font font = client.font;
		int textX = x + (w - font.width(label)) / 2;
		int textY = y + (h - 8) / 2;
		graphics.drawString(font, label, textX, textY, pressed ? 0xFF202020 : WHITE, false);
	}

	// ------------------------------------------------------------------ target health

	private static void drawTarget(GuiGraphics graphics, Minecraft client, int screenWidth, int screenHeight) {
		if (!(client.crosshairPickEntity instanceof LivingEntity target)) return;
		if (!target.isAlive()) return;

		Font font = client.font;
		int barWidth = 80;
		int barHeight = 4;
		int height = 8 + 2 + barHeight;

		int x = anchorX(HudModule.TARGET, screenWidth, barWidth);
		int y = anchorY(HudModule.TARGET, screenHeight, height);

		String name = target.getDisplayName() != null ? target.getDisplayName().getString() : "Mob";
		float health = target.getHealth();
		float maxHealth = Math.max(1.0F, target.getMaxHealth());
		String hp = String.format("%.0f/%.0f", health, maxHealth);

		int armor = 0;
		try {
			armor = target.getArmorValue();
		} catch (Throwable ignored) {
		}
		if (armor > 0) {
			hp = hp + "  armor " + armor;
		}

		boolean shadow = HudConfig.get().textShadow;
		graphics.drawString(font, name, x + (barWidth - font.width(name)) / 2, y, HudColors.current(), shadow);
		graphics.drawString(font, hp, x + (barWidth - font.width(hp)) / 2, y + 10, GRAY, shadow);

		int barY = y + 21;
		int filled = Mth.clamp(Math.round(barWidth * (health / maxHealth)), 0, barWidth);
		graphics.fill(x - 1, barY - 1, x + barWidth + 1, barY + barHeight + 1, 0x90000000);
		graphics.fill(x, barY, x + barWidth, barY + barHeight, 0xFF3B3B3B);
		graphics.fill(x, barY, x + filled, barY + barHeight, healthColor(health / maxHealth));
	}

	private static int healthColor(float ratio) {
		if (ratio > 0.5F) return 0xFF55FF55;
		if (ratio > 0.25F) return 0xFFFFAA00;
		return 0xFFFF5555;
	}


	// ------------------------------------------------------------------ waypoints

	public static String dimensionOf(Minecraft client) {
		try {
			return client.level.dimension().identifier().getPath();
		} catch (Throwable t) {
			return "overworld";
		}
	}

	/** True if this waypoint belongs to the world (and dimension) the player is currently in. */
	public static boolean isHere(Waypoint waypoint, String dimension, String world) {
		if (waypoint == null || !waypoint.enabled) return false;
		if (waypoint.dimension != null && !waypoint.dimension.isEmpty()
				&& !waypoint.dimension.equals(dimension)) {
			return false;
		}
		if (HudConfig.get().waypointsAllWorlds) return true;
		if (waypoint.world == null || waypoint.world.isEmpty()) return true; // legacy entries
		return waypoint.world.equals(world);
	}

	/**
	 * Sorted nearest-first. Distances are computed once each and cached on the waypoint,
	 * instead of recomputing a square root inside every comparison.
	 */
	private static List<Waypoint> buildWaypoints(Minecraft client) {
		List<Waypoint> active = new ArrayList<>();
		String dimension = dimensionOf(client);
		String world = HudTrackers.worldId(client);
		double px = client.player.getX();
		double pz = client.player.getZ();

		for (Waypoint waypoint : HudConfig.get().waypoints) {
			if (!isHere(waypoint, dimension, world)) continue;
			waypoint.cachedDistance = waypoint.distanceTo(px, pz);
			active.add(waypoint);
		}
		active.sort((a, b) -> Double.compare(a.cachedDistance, b.cachedDistance));
		return active;
	}

	private static void drawWaypointList(GuiGraphics graphics, Minecraft client, int screenWidth, int screenHeight) {
		List<Waypoint> waypoints = frameWaypoints;
		if (waypoints.isEmpty()) return;

		Font font = client.font;
		int shown = Math.min(waypoints.size(), 6);

		List<String> labels = new ArrayList<>();
		for (int i = 0; i < shown; i++) {
			Waypoint waypoint = waypoints.get(i);
			labels.add(waypoint.name + " " + (int) Math.round(waypoint.cachedDistance) + "m");
		}

		int width = 0;
		for (String label : labels) {
			width = Math.max(width, font.width(label) + 8);
		}

		int x = anchorX(HudModule.WAYPOINTS, screenWidth, width);
		int y = anchorY(HudModule.WAYPOINTS, screenHeight, shown * 10);

		boolean shadow = HudConfig.get().textShadow;
		for (int i = 0; i < shown; i++) {
			Waypoint waypoint = waypoints.get(i);
			graphics.fill(x, y + 2, x + 5, y + 7, waypoint.color());
			graphics.drawString(font, labels.get(i), x + 8, y, waypoint.color(), shadow);
			y += 10;
		}
	}

	private static void drawCompass(GuiGraphics graphics, Minecraft client, int screenWidth, int screenHeight) {
		int barWidth = 180;
		int barHeight = 14;

		int x = anchorX(HudModule.COMPASS, screenWidth, barWidth);
		int y = anchorY(HudModule.COMPASS, screenHeight, barHeight);
		int centerX = x + barWidth / 2;

		graphics.fill(x, y, x + barWidth, y + barHeight, 0x60000000);

		float yaw = client.player.getYRot();
		Font font = client.font;
		boolean shadow = HudConfig.get().textShadow;

		// Cardinal directions: south = 0, west = 90, north = 180, east = -90.
		drawMarker(graphics, font, centerX, x, barWidth, y, Mth.wrapDegrees(0.0F - yaw), "S", 0xFFCCCCCC, shadow);
		drawMarker(graphics, font, centerX, x, barWidth, y, Mth.wrapDegrees(90.0F - yaw), "W", 0xFFCCCCCC, shadow);
		drawMarker(graphics, font, centerX, x, barWidth, y, Mth.wrapDegrees(180.0F - yaw), "N", 0xFFFF7B7B, shadow);
		drawMarker(graphics, font, centerX, x, barWidth, y, Mth.wrapDegrees(-90.0F - yaw), "E", 0xFFCCCCCC, shadow);

		for (Waypoint waypoint : frameWaypoints) {
			double dx = waypoint.x - client.player.getX();
			double dz = waypoint.z - client.player.getZ();
			float target = (float) Math.toDegrees(Math.atan2(-dx, dz));
			float relative = Mth.wrapDegrees(target - yaw);
			if (Math.abs(relative) > 60.0F) continue;

			int markerX = centerX + Math.round(relative / 60.0F * (barWidth / 2.0F));
			graphics.fill(markerX - 2, y + 2, markerX + 2, y + 6, waypoint.color());

			String label = (int) Math.round(waypoint.cachedDistance) + "m";
			graphics.drawString(font, label, markerX - font.width(label) / 2, y + 7, waypoint.color(), shadow);
		}

		// Centre tick so you know which way you are actually looking.
		graphics.fill(centerX, y, centerX + 1, y + barHeight, 0xFFFFFFFF);
	}

	private static void drawMarker(GuiGraphics graphics, Font font, int centerX, int barX, int barWidth,
	                               int y, float relative, String label, int color, boolean shadow) {
		if (Math.abs(relative) > 60.0F) return;
		int markerX = centerX + Math.round(relative / 60.0F * (barWidth / 2.0F));
		graphics.drawString(font, label, markerX - font.width(label) / 2, y + 1, color, shadow);
	}


	// ------------------------------------------------------------------ fps graph + motion fx

	private static String biome(Minecraft client, BlockPos pos) {
		long key = pos.asLong();
		if (key == cachedBiomePos) return cachedBiome;
		cachedBiomePos = key;
		try {
			cachedBiome = client.level.getBiome(pos).unwrapKey()
					.map(biomeKey -> biomeKey.identifier().getPath().replace('_', ' '))
					.orElse("unknown");
		} catch (Throwable t) {
			cachedBiome = "unknown";
		}
		return cachedBiome;
	}

	private static void drawFpsGraph(GuiGraphics graphics, Minecraft client, int screenWidth, int screenHeight) {
		int[] history = HudTrackers.fpsHistory();
		int width = 80;
		int height = 34;

		int x = anchorX(HudModule.FPS_GRAPH, screenWidth, width);
		int y = anchorY(HudModule.FPS_GRAPH, screenHeight, height);

		int peak = 1;
		for (int value : history) {
			if (value > peak) peak = value;
		}

		int panelColor = ForgeTheme.hudPanelColor();
		if (panelColor != 0) {
			graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, panelColor);
		}

		int graphTop = y + 10;
		int graphHeight = height - 10;
		graphics.fill(x, graphTop + graphHeight, x + width, graphTop + graphHeight + 1,
				ForgeTheme.withAlpha(ForgeTheme.accent(), 0x66));

		int step = Math.max(1, history.length / width + 1);
		for (int i = 0; i < history.length; i += step) {
			int barHeight = Math.round(graphHeight * (history[i] / (float) peak));
			int barX = x + Math.round(i / (float) history.length * width);
			int barWidth = Math.max(1, width / history.length);
			graphics.fill(barX, graphTop + graphHeight - barHeight, barX + barWidth, graphTop + graphHeight,
					ForgeTheme.withAlpha(ForgeTheme.accent(), 0xCC));
		}

		graphics.drawString(client.font, HudTrackers.fps() + " fps  peak " + peak, x, y,
				HudColors.current(), HudConfig.get().textShadow);
	}

	/**
	 * Motion FX. This is a screen space speed effect - vignette plus streaks that build with
	 * velocity. It is NOT true per-pixel motion blur: that needs framebuffer access, which is
	 * exactly the kind of thing that breaks under VulkanMod, so ForgeHUD does not go near it.
	 */
	private static void drawMotionFx(GuiGraphics graphics, Minecraft client, int screenWidth, int screenHeight) {
		int intensity = HudConfig.get().motionIntensity;
		if (intensity <= 0) return;

		double speed = HudTrackers.horizontalSpeed();
		double vertical = Math.abs(HudTrackers.verticalSpeed());
		double combined = Math.max(speed, vertical * 0.6D);

		float strength = (float) Mth.clamp((combined - 4.5D) / 9.0D, 0.0D, 1.0D);
		if (strength <= 0.02F) return;

		int maxAlpha = Mth.clamp(Math.round(strength * 26.0F * intensity), 0, 150);
		int bands = 14;

		for (int i = 0; i < bands; i++) {
			int alpha = Math.round(maxAlpha * (1.0F - (i / (float) bands)));
			if (alpha <= 0) continue;
			int color = ForgeTheme.withAlpha(0x000000, alpha);

			int inset = i * 3;
			graphics.fill(inset, 0, inset + 3, screenHeight, color);
			graphics.fill(screenWidth - inset - 3, 0, screenWidth - inset, screenHeight, color);
			if (i < bands / 2) {
				graphics.fill(0, inset, screenWidth, inset + 3, color);
				graphics.fill(0, screenHeight - inset - 3, screenWidth, screenHeight - inset, color);
			}
		}

		// Streaks: deterministic positions that scroll with time, so it reads as motion.
		int streaks = 6 + intensity * 3;
		long tick = System.currentTimeMillis() / 45L;
		int streakAlpha = Mth.clamp(Math.round(strength * 70.0F), 0, 130);
		int streakColor = ForgeTheme.withAlpha(0xFFFFFF, streakAlpha);

		for (int i = 0; i < streaks; i++) {
			int seed = i * 37;
			int rowY = Math.floorMod(seed * 61 + (int) (tick % screenHeight), Math.max(1, screenHeight));
			int length = 8 + Math.floorMod(seed * 13, 22);
			int offset = Math.floorMod((int) tick + seed * 7, 40);

			graphics.fill(2 + offset, rowY, 2 + offset + length, rowY + 1, streakColor);
			graphics.fill(screenWidth - 2 - offset - length, rowY, screenWidth - 2 - offset, rowY + 1, streakColor);
		}
	}

	// ------------------------------------------------------------------ helpers

	public static int anchorX(HudModule module, int screenWidth, int contentWidth) {
		return HudLayout.resolveX(HudConfig.get().module(module).x, screenWidth, contentWidth);
	}

	public static int anchorY(HudModule module, int screenHeight, int contentHeight) {
		return HudLayout.resolveY(HudConfig.get().module(module).y, screenHeight, contentHeight);
	}

	/** Right-anchored text blocks need their lines right-aligned too, or they ripple. */
	public static boolean isRightAnchored(HudModule module) {
		return HudConfig.get().module(module).x > 0.66F;
	}

	private static String facing(LocalPlayer player) {
		return switch (player.getDirection()) {
			case NORTH -> "North (-Z)";
			case SOUTH -> "South (+Z)";
			case WEST -> "West (-X)";
			case EAST -> "East (+X)";
			default -> "-";
		};
	}

	private static String formatTicks(int ticks) {
		if (ticks < 0) return "**:**";
		int seconds = ticks / 20;
		return String.format("%d:%02d", seconds / 60, seconds % 60);
	}

	private static String roman(int value) {
		return switch (value) {
			case 1 -> "I";
			case 2 -> "II";
			case 3 -> "III";
			case 4 -> "IV";
			case 5 -> "V";
			default -> String.valueOf(value);
		};
	}
}
