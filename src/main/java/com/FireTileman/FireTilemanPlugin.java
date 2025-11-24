package com.FireTileman;

import com.google.inject.Provides;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.ObjectComposition;
import net.runelite.api.ObjectID;
import net.runelite.api.Renderable;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.callback.ClientThread;

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
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private FireTilemanOverlay overlay;

	private final Set<WorldPoint> markedTiles = new HashSet<>();
	private final java.util.Map<WorldPoint, RuneLiteObject> persistentFires = new java.util.HashMap<>();
	private static final int FIRE_MODEL_ID = 2260;

	private boolean isFireObject(int objectId)
	{
		return (objectId >= 26185 && objectId <= 26500) || objectId == ObjectID.FIRE;
	}

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
		
		loadMarkedTiles();
		restorePersistentFires();
		
		log.debug("Fire Tileman started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		
		saveMarkedTiles();
		
		for (RuneLiteObject fireObject : persistentFires.values())
		{
			if (fireObject != null)
			{
				fireObject.setActive(false);
			}
		}
		persistentFires.clear();
		
		log.debug("Fire Tileman stopped!");
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		if (client.getLocalPlayer() == null)
		{
			return;
		}

		GameObject gameObject = event.getGameObject();
		int objectId = gameObject.getId();

		if (isFireObject(objectId))
		{
			Tile tile = event.getTile();
			WorldPoint worldPoint = tile.getWorldLocation();
			WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();

			if (worldPoint.distanceTo(playerLocation) <= 1)
			{
				markedTiles.add(worldPoint);
				saveMarkedTiles();
				
				RuneLiteObject existingFire = persistentFires.remove(worldPoint);
				if (existingFire != null)
				{
					existingFire.setActive(false);
				}
				
				log.debug("Fire created at {}, marking tile", worldPoint);
			}
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
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

			if (markedTiles.contains(worldPoint) && config.showPersistentFires())
			{
				createPersistentFire(worldPoint);
				log.debug("Fire despawned at {}, creating persistent fire", worldPoint);
			}
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING)
		{
			for (RuneLiteObject fireObject : persistentFires.values())
			{
				if (fireObject != null)
				{
					fireObject.setActive(false);
				}
			}
			persistentFires.clear();
		}
		else if (event.getGameState() == GameState.LOGGED_IN)
		{
			restorePersistentFires();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("firetileman"))
		{
			return;
		}

		if (event.getKey().equals("showPersistentFires"))
		{
			clientThread.invokeLater(() ->
			{
				if (config.showPersistentFires())
				{
					restorePersistentFires();
				}
				else
				{
					for (RuneLiteObject fireObject : persistentFires.values())
					{
						if (fireObject != null)
						{
							fireObject.setActive(false);
						}
					}
					persistentFires.clear();
				}
			});
		}
	}

	private void createPersistentFire(WorldPoint worldPoint)
	{
		if (!config.showPersistentFires())
		{
			return;
		}

		if (hasFireAtLocation(worldPoint))
		{
			return;
		}

		try
		{
			RuneLiteObject existingFire = persistentFires.get(worldPoint);
			if (existingFire != null)
			{
				existingFire.setActive(false);
			}

			RuneLiteObject fireObject = client.createRuneLiteObject();
			
			net.runelite.api.Model fireModel = null;
			
			try
			{
				fireModel = client.loadModel(FIRE_MODEL_ID);
			}
			catch (Exception e)
			{
				log.warn("Could not load model {}: {}", FIRE_MODEL_ID, e.getMessage());
			}
			
			if (fireModel == null)
			{
				ObjectComposition fireComposition = client.getObjectDefinition(FIRE_MODEL_ID);
				if (fireComposition != null)
				{
					try
					{
						java.lang.reflect.Method getRenderable = fireComposition.getClass().getMethod("getRenderable");
						Renderable fireRenderable = (Renderable) getRenderable.invoke(fireComposition);
						if (fireRenderable instanceof net.runelite.api.Model)
						{
							fireModel = (net.runelite.api.Model) fireRenderable;
						}
						else if (fireRenderable != null)
						{
							try
							{
								java.lang.reflect.Method getModel = fireRenderable.getClass().getMethod("getModel");
								Object modelObj = getModel.invoke(fireRenderable);
								if (modelObj instanceof net.runelite.api.Model)
								{
									fireModel = (net.runelite.api.Model) modelObj;
								}
							}
							catch (Exception e)
							{
							}
						}
					}
					catch (Exception e)
					{
					}
				}
			}
			
			if (fireModel == null)
			{
				log.warn("Could not get Model for fire ID {}", FIRE_MODEL_ID);
				return;
			}

			fireObject.setModel(fireModel);

			LocalPoint localPoint = LocalPoint.fromWorld(client, worldPoint);
			if (localPoint == null)
			{
				log.warn("Could not convert WorldPoint {} to LocalPoint", worldPoint);
				return;
			}

			fireObject.setLocation(localPoint, client.getPlane());
			fireObject.setActive(true);

			persistentFires.put(worldPoint, fireObject);
			
			log.debug("Created persistent fire at {}", worldPoint);
		}
		catch (Exception e)
		{
			log.error("Error creating persistent fire at {}: {}", worldPoint, e.getMessage(), e);
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

	private void restorePersistentFires()
	{
		if (!config.showPersistentFires())
		{
			return;
		}
		
		if (client.getLocalPlayer() == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		
		for (WorldPoint markedTile : new HashSet<>(markedTiles))
		{
			LocalPoint localPoint = LocalPoint.fromWorld(client, markedTile);
			if (localPoint != null)
			{
				createPersistentFire(markedTile);
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

	private boolean hasFireAtLocation(WorldPoint worldPoint)
	{
		LocalPoint localPoint = LocalPoint.fromWorld(client, worldPoint);
		if (localPoint == null)
		{
			return false;
		}

		int sceneX = localPoint.getSceneX();
		int sceneY = localPoint.getSceneY();

		if (sceneX < 0 || sceneY < 0 || sceneX >= 104 || sceneY >= 104)
		{
			return false;
		}

		Tile tile = client.getScene().getTiles()[client.getPlane()][sceneX][sceneY];
		if (tile == null)
		{
			return false;
		}

		GameObject[] gameObjects = tile.getGameObjects();
		if (gameObjects != null)
		{
			for (GameObject gameObject : gameObjects)
			{
				if (gameObject != null && isFireObject(gameObject.getId()))
				{
					return true;
				}
			}
		}

		return false;
	}
}

