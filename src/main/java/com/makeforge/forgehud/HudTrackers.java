package com.makeforge.forgehud;

import net.minecraft.client.Minecraft;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * All counters are computed by ForgeHUD itself instead of poking at internal fields,
 * so nothing breaks (or crashes) on VulkanMod / Sodium style rendering backends.
 */
public class HudTrackers {

	// ---- FPS (counted in the HUD render pass) ----
	private static int frames = 0;
	private static long lastFpsUpdate = 0L;
	private static int fps = 0;

	public static void countFrame() {
		frames++;
		long now = System.currentTimeMillis();
		if (lastFpsUpdate == 0L) {
			lastFpsUpdate = now;
			return;
		}
		if (now - lastFpsUpdate >= 1000L) {
			fps = (int) (frames * 1000L / (now - lastFpsUpdate));
			frames = 0;
			lastFpsUpdate = now;
		}
		pushHistory(fps, now);
	}

	public static int fps() {
		return fps;
	}

	// ---- FPS history for the graph ----
	private static final int HISTORY = 60;
	private static final int[] fpsHistory = new int[HISTORY];
	private static int historyIndex = 0;
	private static long lastHistoryPush = 0L;

	private static void pushHistory(int value, long now) {
		if (now - lastHistoryPush < 500L) return;
		lastHistoryPush = now;
		fpsHistory[historyIndex] = value;
		historyIndex = (historyIndex + 1) % HISTORY;
	}

	/** Oldest to newest. */
	public static int[] fpsHistory() {
		int[] ordered = new int[HISTORY];
		for (int i = 0; i < HISTORY; i++) {
			ordered[i] = fpsHistory[(historyIndex + i) % HISTORY];
		}
		return ordered;
	}

	// ---- CPS (left + right click) ----
	private static final Deque<Long> leftClicks = new ArrayDeque<>();
	private static final Deque<Long> rightClicks = new ArrayDeque<>();
	private static boolean leftWasDown = false;
	private static boolean rightWasDown = false;

	// ---- Speed + session + death point ----
	private static double lastX = 0.0D, lastY = 0.0D, lastZ = 0.0D;
	private static boolean hasLastPos = false;
	private static double horizontalSpeed = 0.0D;
	private static double verticalSpeed = 0.0D;
	private static final long SESSION_START = System.currentTimeMillis();
	private static boolean deathRecorded = false;

	public static double horizontalSpeed() {
		return horizontalSpeed;
	}

	public static double verticalSpeed() {
		return verticalSpeed;
	}

	public static long sessionMillis() {
		return System.currentTimeMillis() - SESSION_START;
	}

	// ---- TPS (estimated from how fast server game time advances) ----
	private static long lastGameTime = -1L;
	private static long lastRealTime = 0L;
	private static double tps = 20.0D;

	public static void tick(Minecraft client) {
		long now = System.currentTimeMillis();

		if (client.options != null) {
			boolean left = client.options.keyAttack.isDown();
			boolean right = client.options.keyUse.isDown();

			if (left && !leftWasDown) {
				leftClicks.addLast(now);
			}
			if (right && !rightWasDown) {
				rightClicks.addLast(now);
			}
			leftWasDown = left;
			rightWasDown = right;
		}

		trim(leftClicks, now);
		trim(rightClicks, now);

		if (client.player != null) {
			double x = client.player.getX();
			double y = client.player.getY();
			double z = client.player.getZ();

			if (hasLastPos) {
				double dx = x - lastX;
				double dy = y - lastY;
				double dz = z - lastZ;
				// 20 ticks per second.
				double horizontal = Math.sqrt(dx * dx + dz * dz) * 20.0D;
				double vertical = dy * 20.0D;
				horizontalSpeed = (horizontalSpeed * 0.7D) + (horizontal * 0.3D);
				verticalSpeed = (verticalSpeed * 0.7D) + (vertical * 0.3D);
			}
			lastX = x;
			lastY = y;
			lastZ = z;
			hasLastPos = true;

			try {
				boolean dying = client.player.isDeadOrDying();
				if (dying && !deathRecorded && client.level != null) {
					HudConfig config = HudConfig.get();
					config.hasDeath = true;
					config.deathX = client.player.blockPosition().getX();
					config.deathY = client.player.blockPosition().getY();
					config.deathZ = client.player.blockPosition().getZ();
					config.deathDimension = client.level.dimension().identifier().getPath();

					if (config.waypointDeathMarker) {
						config.waypoints.removeIf(w -> w != null && "Death".equals(w.name));
						config.waypoints.add(new Waypoint("Death",
								config.deathX, config.deathY, config.deathZ,
								config.deathDimension, worldId(client), 3));
					}

					HudConfig.save();
					deathRecorded = true;
				} else if (!dying) {
					deathRecorded = false;
				}
			} catch (Throwable ignored) {
			}
		} else {
			hasLastPos = false;
			horizontalSpeed = 0.0D;
			verticalSpeed = 0.0D;
		}

		if (client.level != null) {
			long gameTime = client.level.getGameTime();
			if (lastGameTime >= 0L && now - lastRealTime >= 1000L) {
				double elapsed = (now - lastRealTime) / 1000.0D;
				double ticks = gameTime - lastGameTime;
				double measured = ticks / elapsed;
				if (measured >= 0.0D && measured <= 40.0D) {
					// Smooth it a bit so it doesn't jitter every second.
					tps = (tps * 0.5D) + (measured * 0.5D);
				}
				lastGameTime = gameTime;
				lastRealTime = now;
			} else if (lastGameTime < 0L) {
				lastGameTime = gameTime;
				lastRealTime = now;
			}
		} else {
			lastGameTime = -1L;
			tps = 20.0D;
		}
	}

	private static void trim(Deque<Long> deque, long now) {
		while (!deque.isEmpty() && now - deque.peekFirst() > 1000L) {
			deque.pollFirst();
		}
	}

	public static int leftCps() {
		return leftClicks.size();
	}

	public static int rightCps() {
		return rightClicks.size();
	}

	public static double tps() {
		return tps;
	}

	/** Server address, or "singleplayer". Used to scope waypoints to the world you're in. */
	public static String worldId(Minecraft client) {
		try {
			if (client.getCurrentServer() != null && client.getCurrentServer().ip != null) {
				return client.getCurrentServer().ip;
			}
		} catch (Throwable ignored) {
		}
		return "singleplayer";
	}

	public static int ping(Minecraft client) {
		try {
			if (client.getConnection() != null && client.player != null) {
				var info = client.getConnection().getPlayerInfo(client.player.getUUID());
				if (info != null) {
					return info.getLatency();
				}
			}
		} catch (Throwable ignored) {
		}
		return 0;
	}
}
