package com.makeforge.forgehud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.EnumMap;
import java.util.Map;

/**
 * Click a handle to pick a module up, move the mouse, click again to drop it.
 * The ghost box shows the real footprint so you can see exactly where it lands.
 *
 * Widget based on purpose: 1.21.11 changed the raw mouse callbacks to take a MouseButtonEvent,
 * and Buttons keep this working without depending on that signature.
 */
public class HudEditorScreen extends Screen {

	private static final int HANDLE_HEIGHT = 16;

	private final Screen parent;
	private final Map<HudModule, Button> handles = new EnumMap<>(HudModule.class);
	private HudModule held = null;

	public HudEditorScreen(Screen parent) {
		super(Component.literal("Move HUD elements"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		this.handles.clear();

		for (HudModule module : HudModule.values()) {
			if (!HudConfig.get().module(module).enabled) continue;

			int width = handleWidth(module);
			int x = HudLayout.resolveX(HudConfig.get().module(module).x, this.width, width);
			int y = HudLayout.resolveY(HudConfig.get().module(module).y, this.height, HANDLE_HEIGHT);

			Button handle = Button.builder(Component.literal(module.label), button -> {
				if (this.held == module) {
					drop();
				} else {
					this.held = module;
				}
			}).bounds(x, y, width, HANDLE_HEIGHT).build();

			this.handles.put(module, handle);
			this.addRenderableWidget(handle);
		}

		this.addRenderableWidget(Button.builder(Component.literal("Auto arrange"), button -> {
			HudLayout.autoArrange(this.width, this.height);
			this.held = null;
			this.rebuild();
		}).bounds(this.width / 2 - 155, this.height - 26, 100, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("Reset"), button -> {
			for (HudModule module : HudModule.values()) {
				HudConfig.ModuleData data = HudConfig.get().module(module);
				data.x = module.defaultX;
				data.y = module.defaultY;
			}
			this.held = null;
			HudConfig.save();
			this.rebuild();
		}).bounds(this.width / 2 - 50, this.height - 26, 100, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> this.onClose())
				.bounds(this.width / 2 + 55, this.height - 26, 100, 20).build());
	}

	private void drop() {
		this.held = null;
		HudConfig.save();
	}

	private void rebuild() {
		this.clearWidgets();
		this.init();
	}

	private int handleWidth(HudModule module) {
		return Math.max(56, Math.min(120, this.font.width(module.label) + 14));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		if (this.held != null) {
			HudConfig.ModuleData data = HudConfig.get().module(this.held);
			data.x = HudLayout.snap(mouseX / (float) Math.max(1, this.width));
			data.y = Mth.clamp((mouseY - HANDLE_HEIGHT / 2.0F) / Math.max(1, this.height), 0.0F, 1.0F);
		}

		// Ghost footprints, drawn under the handles.
		for (HudModule module : this.handles.keySet()) {
			int[] size = HudLayout.size(module);
			int gx = HudLayout.resolveX(HudConfig.get().module(module).x, this.width, size[0]);
			int gy = HudLayout.resolveY(HudConfig.get().module(module).y, this.height, size[1]);
			boolean active = this.held == module;
			graphics.fill(gx, gy, gx + size[0], gy + size[1], active ? 0x4055FF55 : 0x22FFFFFF);
			graphics.renderOutline(gx, gy, size[0], size[1], active ? 0xFF55FF55 : 0x55FFFFFF);
		}

		for (Map.Entry<HudModule, Button> entry : this.handles.entrySet()) {
			HudModule module = entry.getKey();
			int width = handleWidth(module);
			entry.getValue().setPosition(
					HudLayout.resolveX(HudConfig.get().module(module).x, this.width, width),
					HudLayout.resolveY(HudConfig.get().module(module).y, this.height, HANDLE_HEIGHT));
		}

		super.render(graphics, mouseX, mouseY, delta);

		for (Map.Entry<HudModule, Button> entry : this.handles.entrySet()) {
			Button handle = entry.getValue();
			HudIcons.draw(graphics, entry.getKey(), handle.getX() + 3, handle.getY() + 4,
					this.held == entry.getKey() ? 0xFF55FF55 : 0xFFFFFFFF);
		}

		String hint = this.held == null
				? "Click a handle to pick it up - edges and centre snap"
				: "Move the mouse, click again to drop " + this.held.label;
		graphics.drawCenteredString(this.font, hint, this.width / 2, 8, 0xFFFFFFFF);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		this.held = null;
		HudConfig.save();
		if (this.minecraft != null) {
			this.minecraft.setScreen(this.parent);
		}
	}
}
