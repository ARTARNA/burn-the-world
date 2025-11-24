package com.example;

import com.google.inject.Provides;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.ObjectID;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Fire Tileman",
	description = "Marks tiles when fires are created"
)
public class FireTilemanPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private FireTilemanConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private FireTilemanOverlay overlay;

	private final Set<WorldPoint> markedTiles = new HashSet<>();

	private boolean isFireObject(int objectId)
	{
		return (objectId >= 26185 && objectId <= 26500) || objectId == ObjectID.FIRE;
	}

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
		
		loadMarkedTiles();
		
		log.debug("Fire Tileman started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		
		saveMarkedTiles();
		
		log.debug("Fire Tileman stopped!");
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		GameObject gameObject = event.getGameObject();
		int objectId = gameObject.getId();

		if (isFireObject(objectId))
		{
			Tile tile = event.getTile();
			WorldPoint worldPoint = tile.getWorldLocation();

			markedTiles.add(worldPoint);
			saveMarkedTiles();
			log.debug("Fire created at {}, marking tile", worldPoint);
		}
	}

	private void saveMarkedTiles()
	{
		StringBuilder sb = new StringBuilder();
		for (WorldPoint wp : markedTiles)
		{
			if (sb.length() > 0)
			{
				sb.append(";");
			}
			sb.append(wp.getX()).append(",").append(wp.getY()).append(",").append(wp.getPlane());
		}
		configManager.setConfiguration("firetileman", "markedTiles", sb.toString());
	}

	private void loadMarkedTiles()
	{
		String saved = configManager.getConfiguration("firetileman", "markedTiles");
		if (saved != null && !saved.isEmpty())
		{
			String[] tiles = saved.split(";");
			for (String tile : tiles)
			{
				String[] coords = tile.split(",");
				if (coords.length >= 3)
				{
					try
					{
						int x = Integer.parseInt(coords[0]);
						int y = Integer.parseInt(coords[1]);
						int plane = Integer.parseInt(coords[2]);
						markedTiles.add(new WorldPoint(x, y, plane));
					}
					catch (NumberFormatException e)
					{
						log.warn("Failed to parse saved tile: {}", tile);
					}
				}
			}
		}
	}

	@Provides
	FireTilemanConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FireTilemanConfig.class);
	}

	Set<WorldPoint> getMarkedTiles()
	{
		return markedTiles;
	}

	boolean isFireObjectInstance(int objectId)
	{
		return isFireObject(objectId);
	}
}

