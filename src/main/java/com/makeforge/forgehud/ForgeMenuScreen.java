package com.makeforge.forgehud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public class ForgeMenuScreen extends ForgeScreenBase {

	private enum Tab {
		MODULES("Modules"),
		LAYOUT("Layout"),
		WAYPOINTS("Waypoints"),
		VISUALS("Visuals"),
		ABOUT("About");

		final String title;

		Tab(String title) {
			this.title = title;
		}
	}

	private static Tab activeTab = Tab.MODULES;

	private int panelX;
	private int panelY;
	private int panelWidth;
	private int panelHeight;
	private int contentX;
	private int contentY;
	private int contentWidth;
	private int contentHeight;

	private int waypointPage = 0;

	public ForgeMenuScreen() {
		super("ForgeHUD");
	}

	@Override
	protected void init() {
		clearZones();

		this.panelWidth = Math.min(this.width - 20, 392);
		this.panelHeight = Math.min(this.height - 20, 214);
		this.panelX = (this.width - this.panelWidth) / 2;
		this.panelY = (this.height - this.panelHeight) / 2;

		int sidebarWidth = 76;
		this.contentX = this.panelX + sidebarWidth + 8;
		this.contentY = this.panelY + 26;
		this.contentWidth = this.panelWidth - sidebarWidth - 16;
		this.contentHeight = this.panelHeight - 34;

		int tabY = this.panelY + 26;
		for (Tab tab : Tab.values()) {
			Tab target = tab;
			Zone zone = addZone(this.panelX + 6, tabY, sidebarWidth - 6, 16, tab.title, () -> {
				activeTab = target;
				this.waypointPage = 0;
				this.rebuild();
			});
			zone.selected = activeTab == tab;
			tabY += 18;
		}

		Zone close = addZone(this.panelX + 6, this.panelY + this.panelHeight - 22,
				sidebarWidth - 6, 16, "Close", this::onClose);
		close.danger = false;

		switch (activeTab) {
			case MODULES -> buildModules();
			case LAYOUT -> buildLayout();
			case WAYPOINTS -> buildWaypoints();
			case VISUALS -> buildVisuals();
			case ABOUT -> {
			}
		}
	}

	// ------------------------------------------------------------------ tabs

	private void buildModules() {
		int columns = 3;
		int gap = 6;
		int columnWidth = (this.contentWidth - gap * (columns - 1)) / columns;

		int[] columnY = new int[columns];
		for (int i = 0; i < columns; i++) {
			columnY[i] = this.contentY;
		}

		int column = 0;
		int bottom = this.contentY + this.contentHeight;

		for (HudGroup group : HudGroup.values()) {
			int count = 0;
			for (HudModule module : HudModule.values()) {
				if (module.group == group) count++;
			}
			int needed = 14 + count * 16 + 6;

			if (columnY[column] + needed > bottom && column < columns - 1) {
				column++;
			}

			int x = this.contentX + column * (columnWidth + gap);
			int y = columnY[column];

			addHeader(x, y, columnWidth, group.title);
			y += 14;

			for (HudModule module : HudModule.values()) {
				if (module.group != group) continue;

				HudConfig.ModuleData data = HudConfig.get().module(module);
				Zone zone = addZone(x, y, columnWidth, 15, module.label, () -> {
					data.enabled = !data.enabled;
					HudConfig.save();
					this.rebuild();
				});
				zone.icon = module;
				zone.toggle = true;
				zone.on = data.enabled;
				y += 16;
			}

			columnY[column] = y + 6;
		}
	}

	private void buildLayout() {
		int width = this.contentWidth;
		int y = this.contentY;

		Zone arrange = addZone(this.contentX, y, width, 16, "Auto arrange everything", () -> {
			HudLayout.autoArrange(this.width, this.height);
			ForgeToasts.show("HUD auto arranged");
		});
		arrange.value = "run";
		y += 18;

		Zone move = addZone(this.contentX, y, width, 16, "Move modules by hand", () -> {
			if (this.minecraft != null) this.minecraft.setScreen(new HudEditorScreen(this));
		});
		move.value = "open";
		y += 18;

		Zone reset = addZone(this.contentX, y, width, 16, "Reset all positions", () -> {
			for (HudModule module : HudModule.values()) {
				HudConfig.ModuleData data = HudConfig.get().module(module);
				data.x = module.defaultX;
				data.y = module.defaultY;
			}
			HudConfig.save();
			ForgeToasts.show("Positions reset");
		});
		reset.danger = true;
		y += 22;

		Zone master = addZone(this.contentX, y, width, 16, "HUD enabled", () -> {
			HudConfig.get().masterEnabled = !HudConfig.get().masterEnabled;
			HudConfig.save();
			this.rebuild();
		});
		master.toggle = true;
		master.on = HudConfig.get().masterEnabled;
		y += 18;

		Zone shadow = addZone(this.contentX, y, width, 16, "Text shadow", () -> {
			HudConfig.get().textShadow = !HudConfig.get().textShadow;
			HudConfig.save();
			this.rebuild();
		});
		shadow.toggle = true;
		shadow.on = HudConfig.get().textShadow;
	}

	private void buildVisuals() {
		int width = this.contentWidth;
		int y = this.contentY;

		Zone accent = addZone(this.contentX, y, width, 16, "Accent colour", () -> {
			ForgeTheme.cycleAccent();
			HudConfig.save();
			this.rebuild();
		});
		accent.value = ForgeTheme.accentName();
		y += 18;

		Zone text = addZone(this.contentX, y, width, 16, "HUD text colour", () -> {
			HudColors.cycle();
			HudConfig.save();
			this.rebuild();
		});
		text.value = HudColors.currentName();
		y += 18;

		Zone panels = addZone(this.contentX, y, width, 16, "Module panels", () -> {
			HudConfig.get().panelStyle = (HudConfig.get().panelStyle + 1) % 3;
			HudConfig.save();
			this.rebuild();
		});
		panels.value = switch (HudConfig.get().panelStyle) {
			case 1 -> "subtle";
			case 2 -> "solid";
			default -> "off";
		};
		y += 18;

		Zone motion = addZone(this.contentX, y, width, 16, "Motion FX strength", () -> {
			HudConfig.get().motionIntensity = (HudConfig.get().motionIntensity + 1) % 4;
			HudConfig.save();
			this.rebuild();
		});
		motion.value = switch (HudConfig.get().motionIntensity) {
			case 1 -> "light";
			case 2 -> "medium";
			case 3 -> "heavy";
			default -> "off";
		};
		y += 18;

		Zone toasts = addZone(this.contentX, y, width, 16, "Notifications", () -> {
			HudConfig.get().toastsEnabled = !HudConfig.get().toastsEnabled;
			HudConfig.save();
			if (HudConfig.get().toastsEnabled) ForgeToasts.show("Notifications on");
			this.rebuild();
		});
		toasts.toggle = true;
		toasts.on = HudConfig.get().toastsEnabled;
		y += 18;

		Zone motionModule = addZone(this.contentX, y, width, 16, "Motion FX module", () -> {
			HudConfig.ModuleData data = HudConfig.get().module(HudModule.MOTION);
			data.enabled = !data.enabled;
			HudConfig.save();
			this.rebuild();
		});
		motionModule.toggle = true;
		motionModule.on = HudConfig.get().module(HudModule.MOTION).enabled;
		motionModule.icon = HudModule.MOTION;
	}

	private void buildWaypoints() {
		int width = this.contentWidth;
		int y = this.contentY;

		Zone add = addZone(this.contentX, y, width / 2 - 2, 16, "Add here", () -> {
			addWaypointHere();
			this.rebuild();
		});
		add.value = "B";

		Zone all = addZone(this.contentX + width / 2 + 2, y, width / 2 - 2, 16, "All worlds", () -> {
			HudConfig.get().waypointsAllWorlds = !HudConfig.get().waypointsAllWorlds;
			HudConfig.save();
			this.rebuild();
		});
		all.toggle = true;
		all.on = HudConfig.get().waypointsAllWorlds;

		y += 20;

		List<Waypoint> waypoints = HudConfig.get().waypoints;
		int rows = Math.max(1, (this.contentY + this.contentHeight - y - 20) / 16);

		// Deleting the last entry on a page would otherwise leave you staring at a blank page.
		int maxPage = Math.max(0, (waypoints.size() - 1) / rows);
		if (this.waypointPage > maxPage) this.waypointPage = maxPage;

		int start = this.waypointPage * rows;

		for (int i = start; i < Math.min(start + rows, waypoints.size()); i++) {
			Waypoint waypoint = waypoints.get(i);

			Zone row = addZone(this.contentX, y, width - 74, 15,
					waypoint.name + "  " + waypoint.x + " " + waypoint.y + " " + waypoint.z, () -> {
						waypoint.enabled = !waypoint.enabled;
						HudConfig.save();
						this.rebuild();
					});
			row.toggle = true;
			row.on = waypoint.enabled;

			Zone colour = addZone(this.contentX + width - 72, y, 36, 15,
					WaypointColors.name(waypoint.colorIndex), () -> {
						waypoint.colorIndex = (waypoint.colorIndex + 1) % WaypointColors.COLORS.length;
						HudConfig.save();
						this.rebuild();
					});
			colour.label = "";
			colour.value = WaypointColors.name(waypoint.colorIndex);

			Zone delete = addZone(this.contentX + width - 34, y, 34, 15, "Delete", () -> {
				HudConfig.get().waypoints.remove(waypoint);
				HudConfig.save();
				ForgeToasts.show("Waypoint deleted");
				this.rebuild();
			});
			delete.danger = true;

			y += 16;
		}

		int footerY = this.contentY + this.contentHeight - 16;

		addZone(this.contentX, footerY, 40, 16, "Prev", () -> {
			if (this.waypointPage > 0) {
				this.waypointPage--;
				this.rebuild();
			}
		});

		addZone(this.contentX + 44, footerY, 40, 16, "Next", () -> {
			if ((this.waypointPage + 1) * rows < HudConfig.get().waypoints.size()) {
				this.waypointPage++;
				this.rebuild();
			}
		});

		Zone clear = addZone(this.contentX + width - 60, footerY, 60, 16, "Clear all", () -> {
			HudConfig.get().waypoints.clear();
			HudConfig.save();
			ForgeToasts.show("Waypoints cleared");
			this.rebuild();
		});
		clear.danger = true;
	}

	/** Adds a waypoint at the player's feet, tagged with the current world and dimension. */
	public static void addWaypointHere() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.level == null) return;

		Waypoint waypoint = new Waypoint(
				"WP " + (HudConfig.get().waypoints.size() + 1),
				client.player.blockPosition().getX(),
				client.player.blockPosition().getY(),
				client.player.blockPosition().getZ(),
				ForgeHudRenderer.dimensionOf(client),
				HudTrackers.worldId(client),
				HudConfig.get().waypoints.size() % WaypointColors.COLORS.length);

		HudConfig.get().waypoints.add(waypoint);
		HudConfig.save();
		ForgeToasts.show("Waypoint added");
	}

	// ------------------------------------------------------------------ render

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		this.hoverX = mouseX;
		this.hoverY = mouseY;

		super.render(graphics, mouseX, mouseY, delta);

		graphics.fill(0, 0, this.width, this.height, ForgeTheme.BACKDROP);
		ForgeTheme.panel(graphics, this.panelX, this.panelY, this.panelWidth, this.panelHeight);

		// Header
		graphics.fill(this.panelX + 2, this.panelY + 18, this.panelX + this.panelWidth - 2,
				this.panelY + 19, ForgeTheme.PANEL_EDGE);
		graphics.drawString(this.font, "FORGEHUD", this.panelX + 10, this.panelY + 7, ForgeTheme.accent(), false);
		graphics.drawString(this.font, "v2.0.0", this.panelX + 10 + this.font.width("FORGEHUD") + 6,
				this.panelY + 7, ForgeTheme.TEXT_DIM, false);

		String hint = "Right Shift menu  -  Right Ctrl toggle  -  B waypoint";
		graphics.drawString(this.font, hint,
				this.panelX + this.panelWidth - 6 - this.font.width(hint), this.panelY + 7,
				ForgeTheme.TEXT_DIM, false);

		// Sidebar separator
		graphics.fill(this.panelX + 82, this.panelY + 22, this.panelX + 83,
				this.panelY + this.panelHeight - 6, ForgeTheme.PANEL_EDGE);

		renderZones(graphics);

		if (activeTab == Tab.ABOUT) {
			renderAbout(graphics);
		}
		if (activeTab == Tab.WAYPOINTS) {
			String world = HudConfig.get().waypointsAllWorlds
					? "showing every world"
					: "world: " + HudTrackers.worldId(Minecraft.getInstance());
			graphics.drawString(this.font, world, this.contentX + 92,
					this.contentY + this.contentHeight - 12, ForgeTheme.TEXT_DIM, false);
		}
	}

	private void renderAbout(GuiGraphics graphics) {
		String[] lines = {
				"ForgeHUD by MakeForge / PixelForge Studios",
				"",
				"17 modules, fully movable, fully optional.",
				"Drawn entirely with GuiGraphics - no mixins,",
				"no raw GL, no framebuffers, no textures.",
				"That is why it stays stable under VulkanMod.",
				"",
				"Right Shift  -  this menu",
				"Right Ctrl   -  toggle the whole HUD",
				"B            -  drop a waypoint"
		};

		int y = this.contentY + 2;
		for (String line : lines) {
			graphics.drawString(this.font, line, this.contentX, y,
					line.startsWith("Right") || line.startsWith("B ")
							? ForgeTheme.accent() : ForgeTheme.TEXT_DIM, false);
			y += 11;
		}
	}

	@Override
	public void onClose() {
		HudConfig.save();
		if (this.minecraft != null) {
			this.minecraft.setScreen(null);
		}
	}
}
