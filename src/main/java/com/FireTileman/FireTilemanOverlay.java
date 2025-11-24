package com.FireTileman;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class FireTilemanOverlay extends Overlay
{
	private final Client client;
	private final FireTilemanPlugin plugin;
	private final FireTilemanConfig config;

	@Inject
	private FireTilemanOverlay(Client client, FireTilemanPlugin plugin, FireTilemanConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (config.showMarkedTiles())
		{
			Set<WorldPoint> markedTiles = plugin.getMarkedTiles();
			for (WorldPoint worldPoint : markedTiles)
			{
				LocalPoint localPoint = LocalPoint.fromWorld(client, worldPoint);
				if (localPoint != null)
				{
					drawMarkedTile(graphics, localPoint);
				}
			}
		}

		return null;
	}

	private void drawMarkedTile(Graphics2D graphics, LocalPoint localPoint)
	{
		java.awt.Polygon tilePoly = Perspective.getCanvasTilePoly(client, localPoint);
		if (tilePoly == null)
		{
			return;
		}

		graphics.setColor(new Color(255, 255, 0, 100));
		graphics.fillPolygon(tilePoly);

		graphics.setColor(new Color(255, 255, 0, 255));
		graphics.drawPolygon(tilePoly);
	}
}

