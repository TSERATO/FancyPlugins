package com.fancyinnovations.fancydialogs.loaders;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public class FancyDialogsBootstrapper implements PluginBootstrap {
    private static final Logger LOGGER = Logger.getLogger("FancyDialogs");

    @Override
    public void bootstrap(@NotNull BootstrapContext bootstrapContext) {
        LifecycleEventManager<BootstrapContext> manager = bootstrapContext.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.DATAPACK_DISCOVERY, event -> {
            try {
                // Extract the datapack from resources to a temporary location
                Path datapackPath = extractDatapack(bootstrapContext);
                if (datapackPath != null) {
                    event.registrar().discoverPack(datapackPath.toUri(), "fancydialogs_quick_actions");
                    LOGGER.info("Successfully discovered FancyDialogs quick_actions datapack");
                }
            } catch (IOException e) {
                LOGGER.severe("Failed to extract and discover FancyDialogs datapack: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private Path extractDatapack(BootstrapContext context) throws IOException {
        Path pluginDataFolder = context.getDataDirectory();
        Path datapackTargetPath = pluginDataFolder.resolve("quick_actions_datapack");

        // Create the datapack directory if it doesn't exist
        if (!Files.exists(datapackTargetPath)) {
            Files.createDirectories(datapackTargetPath);
        }

        // Copy pack.mcmeta
        copyResourceToPath("datapack/pack.mcmeta", datapackTargetPath.resolve("pack.mcmeta"));

        // Copy dialog files
        Path dialogPath = datapackTargetPath.resolve("data/fancydialogs/dialog");
        Files.createDirectories(dialogPath);
        copyResourceToPath("datapack/data/fancydialogs/dialog/quick_actions_trigger.json",
                dialogPath.resolve("quick_actions_trigger.json"));

        // Copy tag files
        Path tagPath = datapackTargetPath.resolve("data/minecraft/tags/dialog");
        Files.createDirectories(tagPath);
        copyResourceToPath("datapack/data/minecraft/tags/dialog/quick_actions.json",
                tagPath.resolve("quick_actions.json"));

        return datapackTargetPath;
    }

    private void copyResourceToPath(String resourcePath, Path targetPath) throws IOException {
        try (var inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            Files.copy(inputStream, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public @NotNull JavaPlugin createPlugin(@NotNull PluginProviderContext context) {
        return PluginBootstrap.super.createPlugin(context);
    }
}
