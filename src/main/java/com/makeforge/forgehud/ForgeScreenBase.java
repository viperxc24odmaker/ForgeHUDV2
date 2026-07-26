package com.makeforge.forgehud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Base for ForgeHUD's custom interface.
 *
 * Every clickable area is a real (empty label) vanilla Button underneath, and ForgeHUD paints
 * its own row over the top of it after super.render(). That means the whole UI looks nothing
 * like vanilla, while click handling still rides on the widget system that has been stable
 * for years - no dependency on the 1.21.11 MouseButtonEvent signature at all.
 */
public abstract class ForgeScreenBase extends Screen {

	protected int hoverX = -1;
	protected int hoverY = -1;

	private final List<Zone> zones = new ArrayList<>();

	protected static class Zone {
		public int x;
		public int y;
		public int width;
		public int height;
		public String label;
		public String value = null;
		public HudModule icon = null;
		public boolean toggle = false;
		public boolean on = false;
		public boolean selected = false;
		public boolean danger = false;
		public boolean header = false;
	}

	protected ForgeScreenBase(String title) {
		super(Component.literal(title));
	}

	protected void clearZones() {
		this.zones.clear();
	}

	/** Non-interactive section label. No button is registered for it. */
	protected Zone addHeader(int x, int y, int width, String label) {
		Zone zone = new Zone();
		zone.x = x;
		zone.y = y;
		zone.width = width;
		zone.height = 12;
		zone.label = label;
		zone.header = true;
		this.zones.add(zone);
		return zone;
	}

	protected Zone addZone(int x, int y, int width, int height, String label, Runnable action) {
		this.addRenderableWidget(Button.builder(Component.empty(), button -> action.run())
				.bounds(x, y, width, height).build());

		Zone zone = new Zone();
		zone.x = x;
		zone.y = y;
		zone.width = width;
		zone.height = height;
		zone.label = label;
		this.zones.add(zone);
		return zone;
	}

	protected boolean isHovered(Zone zone) {
		return this.hoverX >= zone.x && this.hoverX <= zone.x + zone.width
				&& this.hoverY >= zone.y && this.hoverY <= zone.y + zone.height;
	}

	protected void renderZones(GuiGraphics graphics) {
		for (Zone zone : this.zones) {
			if (zone.header) {
				graphics.drawString(this.font, zone.label.toUpperCase(java.util.Locale.ROOT),
						zone.x + 2, zone.y + 2, ForgeTheme.TEXT_DIM, false);
				graphics.fill(zone.x + 2, zone.y + 11, zone.x + zone.width - 2, zone.y + 12,
						ForgeTheme.PANEL_EDGE);
				continue;
			}

			boolean hovered = isHovered(zone);
			int background = zone.selected
					? ForgeTheme.withAlpha(ForgeTheme.accent(), 0x33)
					: (hovered ? ForgeTheme.ROW_HOVER : ForgeTheme.ROW);

			graphics.fill(zone.x, zone.y, zone.x + zone.width, zone.y + zone.height, background);

			if (zone.selected || hovered) {
				ForgeTheme.accentBar(graphics, zone.x, zone.y, zone.height,
						zone.danger ? 0xFFFF4D6D : ForgeTheme.accent());
			}

			int textX = zone.x + 6;
			if (zone.icon != null) {
				HudIcons.draw(graphics, zone.icon, zone.x + 5, zone.y + (zone.height - 8) / 2,
						zone.on || !zone.toggle ? ForgeTheme.accent() : ForgeTheme.OFF);
				textX = zone.x + 16;
			}

			int labelColor = zone.danger
					? 0xFFFF7A8A
					: (zone.toggle && !zone.on ? ForgeTheme.OFF : ForgeTheme.TEXT);
			graphics.drawString(this.font, zone.label, textX, zone.y + (zone.height - 8) / 2, labelColor, false);

			if (zone.toggle) {
				ForgeTheme.pill(graphics, zone.x + zone.width - 22, zone.y + (zone.height - 8) / 2, zone.on);
			} else if (zone.value != null) {
				int valueX = zone.x + zone.width - 6 - this.font.width(zone.value);
				graphics.drawString(this.font, zone.value, valueX, zone.y + (zone.height - 8) / 2,
						ForgeTheme.accent(), false);
			}
		}
	}

	protected void rebuild() {
		this.clearWidgets();
		this.clearZones();
		this.init();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
