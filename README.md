# PicAxe Mod

**Bring your world to life by decorating it with any image from the web!**

---

## About the Mod

**PicAxe** is the ultimate decoration mod that gives you the power to apply custom images to a variety of in-game items and blocks. Using specialized tools like the **PicAxe** and **Image Configurator**, you can transform your Minecraft world with custom imagery from the internet.

Whether you want to create custom bedding, personalized shields, multi-sided art blocks, or resizable framed pictures with advanced configuration options, PicAxe provides comprehensive tools to make your Minecraft world uniquely yours.

---

## Features

### **Core Tools**
* **The PicAxe Tool**: A magical axe used to store and apply image URLs. Right-click in the air to open a modern GUI and paste your image link.
* **Image Configurator**: An advanced configuration tool that opens detailed tabbed interfaces for fine-tuning your image blocks with precision controls.

### **Image Blocks & Items**
* **Image Beds**: Convert any vanilla bed into a custom Image Bed with advanced styling options, render types, and color customization.
* **Image Banners**: Convert vanilla banners into Image Banners that wave in the wind. Supports both standing and wall-mounted variants.
* **Image Shields**: Craft a custom Image Banner with a regular shield to create a unique Image Shield that displays your chosen picture.
* **6-Sided Image Block**: A versatile block where each face can display a different image. Features lighting controls (use glowstone to light up, stick to turn off).
* **Image Frame**: A highly configurable frame block with advanced features:
  - Resizable from 1x1 up to 6x6 blocks
  - Multiple alignment options (center, corners, edges)
  - Rotation and transformation controls
  - Position offset adjustments
  - Stretch-to-fit or aspect-ratio preservation

### **Advanced Configuration**
* **Tabbed Interface System**: Modern, user-friendly configuration screens with organized tabs for different settings
* **Per-Block Customization**: Each image block type has its own specialized configuration options
* **Real-time Preview**: See changes as you make them with live preview functionality

---

## Crafting Recipes

For detailed crafting recipes and instructions, see **[RECIPES.md](RECIPES.md)**.

**Quick Reference:**
- **PicAxe Tool** - The main tool for applying images (requires Pixel Dust + Iron Axe)
- **Image Configurator** - Advanced configuration tool (requires Pixel Dust + Iron + Stick + Redstone)
- **Pixel Dust** - Essential crafting material (Redstone + Lapis + Diamond)
- **Image Frame** - Configurable display frame (Glass Pane + Pixel Dust + Log)
- **6-Sided Image Block** - Multi-face image block (Glass + Pixel Dust + Log)
- **Image Shield** - Combine Shield + Image Banner (shapeless)

---

## How to Use

### **Basic Usage**

1.  **Get an Image URL**: Find an image online that you want to use. Copy the URL to your clipboard.
2.  **Use the PicAxe Tool**: Craft a PicAxe and hold it in your hand. **Right-click in the air** to open the modern URL input screen. Paste your URL and click "Confirm". The URL is now stored on your PicAxe.
3.  **Apply Images to Blocks**:
    * **To vanilla beds or banners**: Right-click on them with the PicAxe to convert them into Image versions and apply your stored URL.
    * **To existing Image Blocks**: Right-click to apply a new image from your PicAxe.
    * **To 6-Sided Image Blocks**: Right-click on specific faces to apply images to individual sides.

### **Advanced Configuration**

4.  **Use the Image Configurator**: Craft an Image Configurator for advanced block customization. Right-click on any Image Block to open detailed configuration screens with multiple tabs:
    * **Image Tab**: Change URLs and validate image links
    * **Size & Layout Tab**: Adjust dimensions and stretch settings (Image Frames)
    * **Alignment Tab**: Control image positioning and alignment
    * **Transform Tab**: Apply rotations and transformations
    * **Position Tab**: Fine-tune X/Y/Z offsets
    * **Style Tabs**: Block-specific options (bed colors, banner styles, etc.)

### **Special Features**

5.  **6-Sided Image Block Lighting**:
    * Use **Glowstone** on the block to make it emit light
    * Use a **Stick** on a lit block to turn off the light and retrieve the glowstone
6.  **Image Frame Advanced Setup**:
    * Apply an image with the PicAxe first
    * Use the Image Configurator for detailed positioning, sizing, and transformation options
7.  **Create Image Shields**:
    * Craft an Image Banner first, then combine it with a regular Shield in a crafting grid


---

## Installation

This mod requires the **Architectury API** and supports both **Fabric** and **NeoForge** mod loaders.

**Minecraft Version**: 1.21.1
**Current Mod Version**: 1.1.2

### For Fabric:

1.  Install [Fabric Loader](https://fabricmc.net/use/installer/) (version 0.16.14 or later)
2.  Install [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api) (version 0.115.6+1.21.1 or later)
3.  Install [Architectury API](https://www.curseforge.com/minecraft/mc-mods/architectury-api) (version 13.0.8 or later)
4.  Place the PicAxe mod `.jar` file into your `mods` folder

### For NeoForge:

1.  Install [NeoForge](https://neoforged.net/) (version 21.1.168 or later)
2.  Install [Architectury API](https://www.curseforge.com/minecraft/mc-mods/architectury-api) (version 13.0.8 or later)
3.  Place the PicAxe mod `.jar` file into your `mods` folder

---

## Development & Contributing

This mod is built using the Architectury toolchain for cross-platform compatibility. The project uses Gradle for building and includes automated CI/CD workflows.

### Building from Source:
```bash
./gradlew build
```

### Project Structure:
- `common/` - Shared code between platforms
- `fabric/` - Fabric-specific implementations
- `neoforge/` - NeoForge-specific implementations

---

## Changelog & Updates

### v1.1.2
- Added an oak planks background toggle for Image Frames.
- Fixed image rotation tiling behavior so rotated textures render correctly.
- Synced Image Frame config networking updates for the new background toggle setting.

For detailed version history, check the project's commit history and releases.

