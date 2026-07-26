package com.makeforge.forgehud;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForgeHudClient implements ClientModInitializer {

	public static final String MOD_ID = "forgehud";
	public static final Logger LOGGER = LoggerFactory.getLogger("ForgeHUD");

	public static KeyMapping openMenuKey;
	public static KeyMapping toggleHudKey;
	public static KeyMapping addWaypointKey;

	@Override
	public void onInitializeClient() {
		HudConfig.load();

		KeyMapping.Category category = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath(MOD_ID, "main"));

		openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.forgehud.open_menu",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_SHIFT,
				category));

		toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.forgehud.toggle_hud",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_CONTROL,
				category));

		addWaypointKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.forgehud.add_waypoint",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_B,
				category));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			try {
				HudTrackers.tick(client);
				while (openMenuKey.consumeClick()) {
					if (client.screen == null) {
						client.setScreen(new ForgeMenuScreen());
					}
				}
				while (addWaypointKey.consumeClick()) {
					if (client.player != null) {
						ForgeMenuScreen.addWaypointHere();
					}
				}
				while (toggleHudKey.consumeClick()) {
					HudConfig.get().masterEnabled = !HudConfig.get().masterEnabled;
					HudConfig.save();
					ForgeToasts.show(HudConfig.get().masterEnabled ? "HUD on" : "HUD off");
				}
			} catch (Throwable t) {
				LOGGER.warn("[ForgeHUD] tick error", t);
			}
		});

		// Drawn just before chat so it sits under vanilla chat / above everything else.
		HudElementRegistry.attachElementBefore(
				VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath(MOD_ID, "hud"),
				(graphics, tickCounter) -> {
					try {
						ForgeHudRenderer.render(graphics);
					} catch (Throwable t) {
						// Never crash the game over a HUD draw.
					}
				});

		LOGGER.info("[ForgeHUD] loaded - Right Shift = settings, Right Ctrl = toggle HUD, B = add waypoint.");
	}
}
