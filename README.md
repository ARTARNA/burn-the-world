# Fire Tileman Plugin

A RuneLite plugin that marks tiles when fires are created (instead of when you walk on them) and keeps fires visually persistent after they burn out.

## Features

- **Fire-based Tile Marking**: Marks tiles only when a fire is created on them, not when you walk on them (unlike standard Tileman mode)
- **Persistent Fire Visualization**: Keeps fires visually visible even after they burn out and drop ash
- **Configurable**: Toggle showing marked tiles and persistent fires in the plugin settings

## How It Works

1. When you create a fire, the plugin detects the fire object spawn event
2. The tile where the fire was created is automatically marked
3. When the fire burns out and despawns, the plugin keeps a visual overlay at that location
4. The overlay shows an orange/red fire-like marker to indicate where fires were previously located

## Configuration

- **Show marked tiles**: Enable/disable showing tiles where fires were created
- **Show persistent fires**: Enable/disable the visual overlay for extinguished fires