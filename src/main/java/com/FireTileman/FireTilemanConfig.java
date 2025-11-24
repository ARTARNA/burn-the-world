package com.FireTileman;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("firetileman")
public interface FireTilemanConfig extends Config
{
	@ConfigItem(
		keyName = "showMarkedTiles",
		name = "Show marked tiles",
		description = "Show overlay for tiles where fires were created"
	)
	default boolean showMarkedTiles()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPersistentFires",
		name = "Show persistent fires",
		description = "Keep fires visually persistent after they burn out"
	)
	default boolean showPersistentFires()
	{
		return true;
	}
}

