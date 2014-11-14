/**
 * This file is part of Almura, All Rights Reserved.
 *
 * Copyright (c) 2014 AlmuraDev <http://github.com/AlmuraDev/>
 */
package com.almuradev.almura.pack;

import com.almuradev.almura.Almura;
import com.almuradev.almura.Filesystem;
import net.malisis.core.renderer.icon.MalisisIcon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

public class PackIcon extends MalisisIcon {

    public PackIcon(String packName, String textureName) {
        super(Almura.MOD_ID.toLowerCase() + ":packs/" + packName + "/" + textureName);
    }

    @Override
    public boolean hasCustomLoader(IResourceManager manager, ResourceLocation location) {
        return true;
    }

    @Override
    public boolean load(IResourceManager manager, ResourceLocation location) {
        String path = location.getResourcePath();
        //Hack to remove forced item prefix
        //TODO Figure out how to make this un-necessary one day...
        String[] split = location.getResourcePath().split("textures/items/");
        if (split.length > 1) {
            path = split[1];
        }

        if (path.startsWith("packs")) {
            //almura:packs/kfood_core/food.png
            final String[] tokens = path.split("/");
            final String packName = tokens[1];
            final String textureName = tokens[2];

            final Path texturePath = Paths.get(Filesystem.CONFIG_PACKS_PATH.toString(), packName + File.separator + textureName + ".png");

            final int mipmapLevels = Minecraft.getMinecraft().gameSettings.mipmapLevels;
            final boolean anisotropic = Minecraft.getMinecraft().gameSettings.anisotropicFiltering > 1.0F;

            try {
                BufferedImage[] textures = new BufferedImage[1 + mipmapLevels];
                textures[0] = ImageIO.read(Files.newInputStream(texturePath));
                loadSprite(textures, null, anisotropic);
                return false;
            } catch (RuntimeException e) {
                Almura.LOGGER.error("Failed to load icon [" + textureName + ".png] in pack [" + packName + "]", e);
            } catch (IOException ignored) {
            }
        }

        return true;
    }
}