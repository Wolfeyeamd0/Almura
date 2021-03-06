/**
 * This file is part of Almura, All Rights Reserved.
 *
 * Copyright (c) 2014 AlmuraDev <http://github.com/AlmuraDev/>
 */
package com.almuradev.almura.pack;

import com.almuradev.almura.Almura;
import com.almuradev.almura.Configuration;
import com.almuradev.almura.pack.block.PackBlock;
import com.almuradev.almura.pack.container.PackContainerBlock;
import com.almuradev.almura.pack.crop.PackCrops;
import com.almuradev.almura.pack.crop.PackSeeds;
import com.almuradev.almura.pack.crop.Stage;
import com.almuradev.almura.pack.item.PackFood;
import com.almuradev.almura.pack.item.PackItem;
import com.almuradev.almura.pack.mapper.EntityMapper;
import com.almuradev.almura.pack.mapper.GameObject;
import com.almuradev.almura.pack.mapper.GameObjectMapper;
import com.almuradev.almura.pack.model.PackFace;
import com.almuradev.almura.pack.model.PackMirrorFace;
import com.almuradev.almura.pack.model.PackModelContainer;
import com.almuradev.almura.pack.model.PackPhysics;
import com.almuradev.almura.pack.node.BiomeNode;
import com.almuradev.almura.pack.node.BreakNode;
import com.almuradev.almura.pack.node.CollisionNode;
import com.almuradev.almura.pack.node.ConsumptionNode;
import com.almuradev.almura.pack.node.ContainerNode;
import com.almuradev.almura.pack.node.DropsNode;
import com.almuradev.almura.pack.node.FertilizerNode;
import com.almuradev.almura.pack.node.FuelNode;
import com.almuradev.almura.pack.node.GrassNode;
import com.almuradev.almura.pack.node.GrowthNode;
import com.almuradev.almura.pack.node.LightNode;
import com.almuradev.almura.pack.node.RecipeNode;
import com.almuradev.almura.pack.node.RenderNode;
import com.almuradev.almura.pack.node.RotationNode;
import com.almuradev.almura.pack.node.SoilNode;
import com.almuradev.almura.pack.node.ToolsNode;
import com.almuradev.almura.pack.node.container.StateProperty;
import com.almuradev.almura.pack.node.property.BiomeProperty;
import com.almuradev.almura.pack.node.property.BonusProperty;
import com.almuradev.almura.pack.node.property.CollisionProperty;
import com.almuradev.almura.pack.node.property.DropProperty;
import com.almuradev.almura.pack.node.property.GameObjectProperty;
import com.almuradev.almura.pack.node.property.RangeProperty;
import com.almuradev.almura.pack.node.property.RotationProperty;
import com.almuradev.almura.pack.node.property.VariableGameObjectProperty;
import com.almuradev.almura.recipe.DuplicateRecipeException;
import com.almuradev.almura.recipe.IRecipe;
import com.almuradev.almura.recipe.IShapedRecipe;
import com.almuradev.almura.recipe.IShapelessRecipe;
import com.almuradev.almura.recipe.ISmeltRecipe;
import com.almuradev.almura.recipe.InvalidRecipeException;
import com.almuradev.almura.recipe.RecipeContainer;
import com.almuradev.almura.recipe.RecipeManager;
import com.almuradev.almura.recipe.UnknownRecipeTypeException;
import com.almuradev.almurasdk.Functions;
import com.almuradev.almurasdk.lang.LanguageRegistry;
import com.almuradev.almurasdk.lang.Languages;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.malisis.core.renderer.RenderParameters;
import net.malisis.core.renderer.element.Vertex;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;
import ninja.leaping.configurate.ConfigurationNode;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PackCreator {

    private static final char[] RECIPE_MATRIX_PLACEHOLDER = new char[]{'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i'};

    public static PackModelContainer createModelContainerFromReader(String name, ConfigurationNode reader) throws IOException {
        final ConfigurationNode boundsConfigurationNode = reader.getNode(PackKeys.NODE_BOUNDS.getKey());

        final boolean
                enableCollision =
                boundsConfigurationNode.getNode(PackKeys.USE_VANILLA_COLLISION.getKey())
                        .getBoolean(PackKeys.USE_VANILLA_COLLISION.getDefaultValue());
        List<Double> collisionCoordinates = Lists.newLinkedList();

        try {
            collisionCoordinates =
                    PackUtil.parseStringToNumericList(Double.class, boundsConfigurationNode.getNode(PackKeys.COLLISION_BOX.getKey())
                            .getString(PackKeys.COLLISION_BOX.getDefaultValue()), 6);
        } catch (NumberFormatException e) {
            if (Configuration.DEBUG_ALL || Configuration.DEBUG_PACKS) {
                Almura.LOGGER.error("Model [" + name + "] has invalid " + PackKeys.COLLISION_BOX.getKey().toLowerCase() + " coordinates. " + e
                        .getMessage(), e);
            } else {
                Almura.LOGGER.warn("Model [" + name + "] has invalid " + PackKeys.COLLISION_BOX.getKey().toLowerCase() + " coordinates. " + e
                        .getMessage());
            }
        }

        final boolean
                enableWireframe =
                boundsConfigurationNode.getNode(PackKeys.USE_VANILLA_WIREFRAME.getKey())
                        .getBoolean(PackKeys.USE_VANILLA_WIREFRAME.getDefaultValue());
        List<Double> wireframeCoordinates = Lists.newLinkedList();

        try {
            wireframeCoordinates = PackUtil.parseStringToNumericList(Double.class, boundsConfigurationNode.getNode(PackKeys.WIREFRAME_BOX.getKey())
                    .getString(PackKeys.WIREFRAME_BOX.getDefaultValue()), 6);
        } catch (NumberFormatException e) {
            if (Configuration.DEBUG_ALL || Configuration.DEBUG_PACKS) {
                Almura.LOGGER.error("Model [" + name + "] has invalid " + PackKeys.WIREFRAME_BOX.getKey().toLowerCase() + " coordinates. " + e
                        .getMessage(), e);
            } else {
                Almura.LOGGER.warn("Model [" + name + "] has invalid " + PackKeys.WIREFRAME_BOX.getKey().toLowerCase() + " coordinates. " + e
                        .getMessage());
            }
        }

        AxisAlignedBB collisionBox = null;
        if (collisionCoordinates.size() == 6) {
            collisionBox =
                    AxisAlignedBB.getBoundingBox(collisionCoordinates.get(0), collisionCoordinates.get(1), collisionCoordinates.get(2),
                            collisionCoordinates.get(3), collisionCoordinates.get(4), collisionCoordinates.get(5));
        }
        AxisAlignedBB wireframeBox = null;
        if (wireframeCoordinates.size() == 6) {
            wireframeBox =
                    AxisAlignedBB.getBoundingBox(wireframeCoordinates.get(0), wireframeCoordinates.get(1), wireframeCoordinates.get(2),
                            wireframeCoordinates.get(3), wireframeCoordinates.get(4), wireframeCoordinates.get(5));
        }

        return new PackModelContainer(name, new PackPhysics(enableCollision, enableWireframe, collisionBox, wireframeBox));
    }

    @SideOnly(Side.CLIENT)
    public static void loadShapeIntoModelContainer(PackModelContainer modelContainer, String name, ConfigurationNode reader)
            throws IOException {
        final ConfigurationNode modelConfigurationNode = reader.getNode(PackKeys.SHAPES.getKey());
        final List<PackFace> faces = Lists.newLinkedList();

        for (ConfigurationNode node : modelConfigurationNode.getChildrenList()) {
            final String rawCoordinateString = node.getNode(PackKeys.TEXTURE_COORDINATES.getKey()).getString("");
            final int textureIndex = node.getNode(PackKeys.TEXTURE.getKey()).getInt(0);

            final List<Vertex> vertices = Lists.newLinkedList();
            for (String rawCoordinate : rawCoordinateString.substring(0, rawCoordinateString.length()).split("\n")) {
                final List<Double> coordinates = Lists.newArrayList();
                try {
                    coordinates.addAll(PackUtil.parseStringToNumericList(Double.class, rawCoordinate, 3));
                } catch (NumberFormatException nfe) {
                    if (Configuration.DEBUG_ALL || Configuration.DEBUG_PACKS) {
                        Almura.LOGGER.error("Could not parse vertex in model [" + name + "]. Value: [" + rawCoordinate + "]", nfe);
                    } else {
                        Almura.LOGGER.warn("Could not parse vertex in model [" + name + "]. Value: [" + rawCoordinate + "]");
                    }
                    continue;
                }

                //Convert list of coordinates to vertex
                vertices.add(new Vertex(coordinates.get(0), coordinates.get(1), coordinates.get(2)));
            }
            final RenderParameters params = new RenderParameters();
            
            final PackFace face = new PackFace(textureIndex, vertices);
            face.setStandardUV();
            face.setParameters(params);
            face.deductParameters();
            params.textureSide.set(ForgeDirection.getOrientation(textureIndex));
            faces.add(face);
        }

        if (faces.isEmpty()) {
            Almura.LOGGER.error("Model [" + name + "] has no faces and therefore will not be loaded.");
            return;
        }

        final PackModelContainer.PackShape shape = new PackModelContainer.PackShape(faces);

        //Handle shapes that don't have at least 4 faces
        if (shape.getFaces().length < 4) {
            shape.applyMatrix();

            final PackModelContainer.PackShape copy = new PackModelContainer.PackShape(shape);
            copy.scale(-1, 1, -1);
            copy.applyMatrix();

            int length = shape.getFaces().length;
            for (int i = 0; i < 4 - length; i++) {
                shape.addFaces(new PackMirrorFace[]{
                        new PackMirrorFace((PackFace) copy.getFaces()[i >= copy.getFaces().length ? copy.getFaces().length - 1 : i])});
            }

            shape.applyMatrix();
        }

        shape.storeState();
        modelContainer.setModel(shape);
    }

    public static PackBlock createBlockFromReader(Pack pack, String name, ConfigurationNode reader) throws IOException {
        final List<String> description = PackUtil.parseNewlineStringIntoList(
                reader.getNode(PackKeys.TITLE.getKey()).getString(PackKeys.TITLE.getDefaultValue()));
        final List<String> tooltip = Lists.newLinkedList();
        if (description.size() > 1) {
            tooltip.addAll(description);
            tooltip.remove(0);
        }
        final String textureName = reader.getNode(PackKeys.TEXTURE.getKey()).getString(PackKeys.TEXTURE.getDefaultValue()).split(".png")[0];
        final String modelName = reader.getNode(PackKeys.SHAPE.getKey()).getString(PackKeys.SHAPE.getDefaultValue()).split(".shape")[0];
        PackModelContainer modelContainer = null;
        for (PackModelContainer mContainer : Pack.getModelContainers()) {
            if (mContainer.getIdentifier().equalsIgnoreCase(modelName)) {
                modelContainer = mContainer;
            }
        }
        if (modelContainer == null && (Configuration.DEBUG_ALL || Configuration.DEBUG_PACKS)) {
            Almura.LOGGER
                    .warn("Model [" + modelName + "] in [" + name + "] in pack [" + pack.getName() + "] was not found. Will render as a basic cube.");
        }
        Map<Integer, List<Integer>> textureCoordinates;
        try {
            textureCoordinates = PackUtil.parseCoordinatesFrom(reader.getNode(PackKeys.TEXTURE_COORDINATES.getKey()).getList(Functions
                    .FUNCTION_STRING_TRANSFORMER));
        } catch (NumberFormatException nfe) {
            if (!reader.getNode(PackKeys.TEXTURE_COORDINATES.getKey()).isVirtual()) {
                Almura.LOGGER.warn("Failed parsing texture coordinates in [" + name + "] in pack [" + pack.getName() + "]. " + nfe.getMessage());
            }
            textureCoordinates = Maps.newHashMap();
        }
        final boolean showInCreativeTab = reader.getNode(PackKeys.SHOW_IN_CREATIVE_TAB.getKey()).getBoolean(
                PackKeys.SHOW_IN_CREATIVE_TAB.getDefaultValue());
        final String creativeTabName = reader.getNode(PackKeys.CREATIVE_TAB.getKey()).getString(PackKeys.CREATIVE_TAB.getDefaultValue());

        final float hardness = reader.getNode(PackKeys.HARDNESS.getKey()).getFloat(PackKeys.HARDNESS.getDefaultValue());
        final float resistance = reader.getNode(PackKeys.RESISTANCE.getKey()).getFloat(PackKeys.RESISTANCE.getDefaultValue());

        final RotationNode rotationNode = createRotationNode(pack, name, reader.getNode(PackKeys.NODE_ROTATE.getKey()));
        final LightNode lightNode = createLightNode(pack, name, reader.getNode(PackKeys.NODE_LIGHT.getKey()));
        final RenderNode renderNode = createRenderNode(pack, name, reader.getNode(PackKeys.NODE_RENDER.getKey()));
        LanguageRegistry.put(Languages.ENGLISH_AMERICAN, "tile." + pack.getName() + "\\" + name + ".name", description.get(0));

        final PackBlock
                block =
                new PackBlock(pack, name, tooltip, textureName, textureCoordinates, modelName, modelContainer, hardness, resistance,
                        showInCreativeTab,
                        creativeTabName,
                        rotationNode, lightNode, renderNode);

        if (!reader.getNode(PackKeys.NODE_FUEL.getKey()).isVirtual()) {
            block.addNode(createFuelNode(pack, name, reader.getNode(PackKeys.NODE_FUEL.getKey())));
        }

        return block;
    }

    public static PackItem createItemFromReader(Pack pack, String name, ConfigurationNode reader) throws IOException {
        final List<String> description = PackUtil.parseNewlineStringIntoList(
                reader.getNode(PackKeys.TITLE.getKey()).getString(PackKeys.TITLE.getDefaultValue()));
        final List<String> tooltip = Lists.newLinkedList();
        if (description.size() > 1) {
            tooltip.addAll(description);
            tooltip.remove(0);
        }
        final String textureName = reader.getNode(PackKeys.TEXTURE.getKey()).getString(PackKeys.TEXTURE.getDefaultValue()).split(".png")[0];
        final String modelName = reader.getNode(PackKeys.SHAPE.getKey()).getString(PackKeys.SHAPE.getDefaultValue()).split(".shape")[0];
        PackModelContainer modelContainer = null;
        for (PackModelContainer mContainer : Pack.getModelContainers()) {
            if (mContainer.getIdentifier().equalsIgnoreCase(modelName)) {
                modelContainer = mContainer;
            }
        }
        if (modelContainer == null && (Configuration.DEBUG_ALL || Configuration.DEBUG_PACKS)) {
            Almura.LOGGER
                    .warn("Model [" + modelName + "] in [" + name + "] in pack [" + pack.getName() + "] was not found. Will render as a basic item.");
        }
        Map<Integer, List<Integer>> textureCoordinates;
        try {
            textureCoordinates = PackUtil.parseCoordinatesFrom(reader.getNode(PackKeys.TEXTURE_COORDINATES.getKey()).getList(Functions
                    .FUNCTION_STRING_TRANSFORMER));
        } catch (NumberFormatException nfe) {
            if (!reader.getNode(PackKeys.TEXTURE_COORDINATES.getKey()).isVirtual()) {
                Almura.LOGGER.warn("Failed parsing texture coordinates in [" + name + "] in pack [" + pack.getName() + "]. " + nfe.getMessage());
            }
            textureCoordinates = Maps.newHashMap();
        }
        final boolean showInCreativeTab = reader.getNode(PackKeys.SHOW_IN_CREATIVE_TAB.getKey()).getBoolean(
                PackKeys.SHOW_IN_CREATIVE_TAB.getDefaultValue());
        final String creativeTabName = reader.getNode(PackKeys.CREATIVE_TAB.getKey()).getString(PackKeys.CREATIVE_TAB.getDefaultValue());

        LanguageRegistry.put(Languages.ENGLISH_AMERICAN, "item." + pack.getName() + "\\" + name + ".name", description.get(0));

        final PackItem
                item =
                new PackItem(pack, name, tooltip, textureName, modelName, modelContainer, textureCoordinates, showInCreativeTab, creativeTabName);

        if (!reader.getNode(PackKeys.NODE_FUEL.getKey()).isVirtual()) {
            item.addNode(createFuelNode(pack, name, reader.getNode(PackKeys.NODE_FUEL.getKey())));
        }

        return item;
    }

    public static PackFood createFoodFromReader(Pack pack, String name, ConfigurationNode reader) throws IOException {
        final List<String> description = PackUtil.parseNewlineStringIntoList(
                reader.getNode(PackKeys.TITLE.getKey()).getString(PackKeys.TITLE.getDefaultValue()));
        final List<String> tooltip = Lists.newLinkedList();
        if (description.size() > 1) {
            tooltip.addAll(description);
            tooltip.remove(0);
        }
        final String textureName = reader.getNode(PackKeys.TEXTURE.getKey()).getString(PackKeys.TEXTURE.getDefaultValue()).split(".png")[0];
        final String modelName = reader.getNode(PackKeys.SHAPE.getKey()).getString(PackKeys.SHAPE.getDefaultValue()).split(".shape")[0];
        PackModelContainer modelContainer = null;
        for (PackModelContainer mContainer : Pack.getModelContainers()) {
            if (mContainer.getIdentifier().equalsIgnoreCase(modelName)) {
                modelContainer = mContainer;
            }
        }
        if (modelContainer == null && (Configuration.DEBUG_ALL || Configuration.DEBUG_PACKS)) {
            Almura.LOGGER
                    .warn("Model [" + modelName + "] in [" + name + "] in pack [" + pack.getName() + "] was not found. Will render as a basic cube");
        }
        Map<Integer, List<Integer>> textureCoordinates;
        try {
            textureCoordinates = PackUtil.parseCoordinatesFrom(reader.getNode(PackKeys.TEXTURE_COORDINATES.getKey()).getList(Functions
                    .FUNCTION_STRING_TRANSFORMER));
        } catch (NumberFormatException nfe) {
            if (!reader.getNode(PackKeys.TEXTURE_COORDINATES.getKey()).isVirtual()) {
                Almura.LOGGER.warn("Failed parsing texture coordinates in [" + name + "] in pack [" + pack.getName() + "]. " + nfe.getMessage());
            }
            textureCoordinates = Maps.newHashMap();
        }
        final boolean showInCreativeTab = reader.getNode(PackKeys.SHOW_IN_CREATIVE_TAB.getKey()).getBoolean(
                PackKeys.SHOW_IN_CREATIVE_TAB.getDefaultValue());
        final String creativeTabName = reader.getNode(PackKeys.CREATIVE_TAB.getKey()).getString(PackKeys.CREATIVE_TAB.getDefaultValue());
        final ConsumptionNode consumptionNode = createConsumptionNode(pack, name, reader.getNode(PackKeys.NODE_CONSUMPTION.getKey()));

        LanguageRegistry.put(Languages.ENGLISH_AMERICAN, "item." + pack.getName() + "\\" + name + ".name", description.get(0));

        final PackFood
                food =
                new PackFood(pack, name, tooltip, textureName, modelName, modelContainer, textureCoordinates, showInCreativeTab, creativeTabName,
                        consumptionNode);

        if (!reader.getNode(PackKeys.NODE_FUEL.getKey()).isVirtual()) {
            food.addNode(createFuelNode(pack, name, reader.getNode(PackKeys.NODE_FUEL.getKey())));
        }

        return food;
    }

    public static PackCrops createCropFromReader(Pack pack, String name, ConfigurationNode reader) throws IOException {
        final String title = reader.getNode(PackKeys.TITLE.getKey()).getString(PackKeys.TITLE.getDefaultValue());
        final String textureName = reader.getNode(PackKeys.TEXTURE.getKey()).getString(PackKeys.TEXTURE.getDefaultValue()).split(".png")[0];

        final int levelRequired = reader.getNode(PackKeys.LEVEL_REQUIRED.getKey()).getInt(PackKeys.LEVEL_REQUIRED.getDefaultValue());

        final Map<Integer, Stage> stages = Maps.newHashMap();

        final PackCrops crop = new PackCrops(pack, name, textureName, levelRequired, stages);

        LanguageRegistry.put(Languages.ENGLISH_AMERICAN, "tile." + pack.getName() + "\\" + name + ".name", title);

        for (Map.Entry<Object, ? extends ConfigurationNode> stageConfigurationEntry : reader.getNode(PackKeys.NODE_STAGES.getKey()).getChildrenMap()
                .entrySet()) {
            final int stageId = (Integer) stageConfigurationEntry.getKey();

            if (stageId < 0 || stageId > 15) {
                Almura.LOGGER.warn("Stage [" + stageId + "] in [" + name + "] in pack [" + pack.getName()
                        + "] is not a valid integer between 0 and 15.");
                continue;
            }

            final Stage stage = createCropStage(pack, name, crop, stageId, stageConfigurationEntry.getValue());
            if (stages.put(stageId, stage) != null) {
                Almura.LOGGER.warn("Stage [" + stageId + "] in [" + name + "] in pack [" + pack.getName()
                        + "] already exists as a stage.");
            }
        }

        return crop;
    }

    public static PackSeeds createCropSeed(Pack pack, String name, Block soil, PackCrops crop, String textureName, ConfigurationNode reader)
            throws IOException {
        final String identifier = crop.getIdentifier() + "\\seed";
        final List<String> description = PackUtil.parseNewlineStringIntoList(
                reader.getNode(PackKeys.TITLE.getKey()).getString(PackKeys.TITLE.getDefaultValue()));
        final List<String> tooltip = Lists.newLinkedList();
        if (description.size() > 1) {
            tooltip.addAll(description);
            tooltip.remove(0);
        }
        final String modelName = reader.getNode(PackKeys.SHAPE.getKey()).getString(PackKeys.SHAPE.getDefaultValue()).split(".shape")[0];
        PackModelContainer modelContainer = null;
        for (PackModelContainer mContainer : Pack.getModelContainers()) {
            if (mContainer.getIdentifier().equalsIgnoreCase(modelName)) {
                modelContainer = mContainer;
            }
        }
        if (modelContainer == null && (Configuration.DEBUG_ALL || Configuration.DEBUG_PACKS)) {
            Almura.LOGGER
                    .warn("Model [" + modelName + "] for seed in [" + crop.getIdentifier() + "] in pack [" + pack.getName()
                            + "] was not found. Will render as a basic item.");
        }
        Map<Integer, List<Integer>> textureCoordinates;
        try {
            textureCoordinates = PackUtil.parseCoordinatesFrom(reader.getNode(PackKeys.TEXTURE_COORDINATES.getKey()).getList(Functions
                    .FUNCTION_STRING_TRANSFORMER));
        } catch (NumberFormatException nfe) {
            if (!reader.getNode(PackKeys.TEXTURE_COORDINATES.getKey()).isVirtual()) {
                Almura.LOGGER.warn("Failed parsing texture coordinates in [" + name + "] in pack [" + pack.getName() + "]. " + nfe.getMessage());
            }
            textureCoordinates = Maps.newHashMap();
        }
        final boolean showInCreativeTab = reader.getNode(PackKeys.SHOW_IN_CREATIVE_TAB.getKey()).getBoolean(
                PackKeys.SHOW_IN_CREATIVE_TAB.getDefaultValue());
        final String creativeTabName = reader.getNode(PackKeys.CREATIVE_TAB.getKey()).getString(PackKeys.CREATIVE_TAB.getDefaultValue());

        LanguageRegistry.put(Languages.ENGLISH_AMERICAN, "item." + pack.getName() + "\\" + identifier + ".name", description.get(0));

        final PackSeeds
                seed =
                new PackSeeds(pack, identifier, tooltip, textureName, modelName, modelContainer, textureCoordinates, showInCreativeTab,
                        creativeTabName, crop, soil);
        seed.addNode(createGrassNode(pack, name, seed, reader.getNode(PackKeys.NODE_GRASS.getKey())));

        if (!reader.getNode(PackKeys.NODE_FUEL.getKey()).isVirtual()) {
            seed.addNode(createFuelNode(pack, identifier, reader.getNode(PackKeys.NODE_FUEL.getKey())));
        }

        return seed;
    }

    @SuppressWarnings("unchecked")
    public static Stage createCropStage(Pack pack, String name, PackCrops crop, int id, ConfigurationNode reader) {
        final String modelName = reader.getNode(PackKeys.SHAPE.getKey()).getString(PackKeys.SHAPE.getDefaultValue()).split(".shape")[0];
        PackModelContainer modelContainer = null;
        for (PackModelContainer mContainer : Pack.getModelContainers()) {
            if (mContainer.getIdentifier().equalsIgnoreCase(modelName)) {
                modelContainer = mContainer;
            }
        }
        if (modelContainer == null && (Configuration.DEBUG_ALL || Configuration.DEBUG_PACKS)) {
            Almura.LOGGER.warn("Model [" + modelName + "] in stage [" + id + "] in [" + name + "] in pack [" + pack.getName()
                    + "] was not found. Will render as a basic cube.");
        }
        Map<Integer, List<Integer>> textureCoordinates;
        try {
            textureCoordinates = PackUtil.parseCoordinatesFrom(reader.getNode(PackKeys.TEXTURE_COORDINATES.getKey()).getList(Functions
                    .FUNCTION_STRING_TRANSFORMER));
        } catch (NumberFormatException nfe) {
            if (!reader.getNode(PackKeys.TEXTURE_COORDINATES.getKey()).isVirtual()) {
                Almura.LOGGER
                        .warn("Failed parsing texture coordinates in stage [" + id + "] in [" + name + "] in pack [" + pack.getName() + "]. " + nfe
                                .getMessage());
            }
            textureCoordinates = Maps.newHashMap();
        }

        final GrowthNode growthNode = createGrowthNode(pack, crop.getIdentifier() + "\\stage\\" + id, reader.getNode(PackKeys.NODE_GROWTH.getKey()));
        final LightNode lightNode = createLightNode(pack, crop.getIdentifier() + "\\stage\\" + id, reader.getNode(PackKeys.NODE_LIGHT.getKey()));

        return new Stage(crop, id, textureCoordinates, modelName, modelContainer, growthNode, lightNode);
    }

    public static PackContainerBlock createContainerBlock(Pack pack, String name, ConfigurationNode reader) {
        final List<String> description = PackUtil.parseNewlineStringIntoList(
                reader.getNode(PackKeys.TITLE.getKey()).getString(PackKeys.TITLE.getDefaultValue()));
        final List<String> tooltip = Lists.newLinkedList();
        if (description.size() > 1) {
            tooltip.addAll(description);
            tooltip.remove(0);
        }
        final String textureName = reader.getNode(PackKeys.TEXTURE.getKey()).getString(PackKeys.TEXTURE.getDefaultValue()).split(".png")[0];
        final String modelName = reader.getNode(PackKeys.SHAPE.getKey()).getString(PackKeys.SHAPE.getDefaultValue()).split(".shape")[0];
        PackModelContainer modelContainer = null;
        for (PackModelContainer mContainer : Pack.getModelContainers()) {
            if (mContainer.getIdentifier().equalsIgnoreCase(modelName)) {
                modelContainer = mContainer;
            }
        }
        if (modelContainer == null && (Configuration.DEBUG_ALL || Configuration.DEBUG_PACKS)) {
            Almura.LOGGER
                    .warn("Model [" + modelName + "] in [" + name + "] in pack [" + pack.getName() + "] was not found. Will render as a basic cube.");
        }
        Map<Integer, List<Integer>> textureCoordinates;
        try {
            textureCoordinates = PackUtil.parseCoordinatesFrom(reader.getNode(PackKeys.TEXTURE_COORDINATES.getKey()).getList(Functions
                    .FUNCTION_STRING_TRANSFORMER));
        } catch (NumberFormatException nfe) {
            if (!reader.getNode(PackKeys.TEXTURE_COORDINATES.getKey()).isVirtual()) {
                Almura.LOGGER.warn("Failed parsing texture coordinates in [" + name + "] in pack [" + pack.getName() + "]. " + nfe.getMessage());
            }
            textureCoordinates = Maps.newHashMap();
        }
        final boolean showInCreativeTab = reader.getNode(PackKeys.SHOW_IN_CREATIVE_TAB.getKey()).getBoolean(
                PackKeys.SHOW_IN_CREATIVE_TAB.getDefaultValue());
        final String creativeTabName = reader.getNode(PackKeys.CREATIVE_TAB.getKey()).getString(PackKeys.CREATIVE_TAB.getDefaultValue());

        final float hardness = reader.getNode(PackKeys.HARDNESS.getKey()).getFloat(PackKeys.HARDNESS.getDefaultValue());
        final float resistance = reader.getNode(PackKeys.RESISTANCE.getKey()).getFloat(PackKeys.RESISTANCE.getDefaultValue());

        final RotationNode rotationNode = createRotationNode(pack, name, reader.getNode(PackKeys.NODE_ROTATE.getKey()));
        final LightNode lightNode = createLightNode(pack, name, reader.getNode(PackKeys.NODE_LIGHT.getKey()));
        final RenderNode renderNode = createRenderNode(pack, name, reader.getNode(PackKeys.NODE_RENDER.getKey()));
        final ContainerNode containerNode = createContainerNode(pack, name, reader.getNode(PackKeys.NODE_CONTAINER.getKey()));
        LanguageRegistry.put(Languages.ENGLISH_AMERICAN, "tile." + pack.getName() + "\\" + name + ".name", description.get(0));

        final PackContainerBlock
                container =
                new PackContainerBlock(pack, name, tooltip, textureName, textureCoordinates, modelName, modelContainer, hardness, resistance,
                        showInCreativeTab,
                        creativeTabName, rotationNode, lightNode, renderNode, containerNode);

        if (!reader.getNode(PackKeys.NODE_FUEL.getKey()).isVirtual()) {
            container.addNode(createFuelNode(pack, name, reader.getNode(PackKeys.NODE_CONTAINER.getKey())));
        }

        return container;
    }

    public static ContainerNode createContainerNode(Pack pack, String name, ConfigurationNode reader) {
        int size = reader.getNode(PackKeys.INVENTORY_SIZE.getKey()).getInt(PackKeys.INVENTORY_SIZE.getDefaultValue());
        int correctSize = size;
        boolean invalid = true;
        if (size < 9) {
            correctSize = 9;
        } else if (size > 54) {
            correctSize = 54;
        } else if (size % 9 != 0) {
            correctSize = 9;
        } else {
            invalid = false;
        }

        if (invalid) {
            Almura.LOGGER.warn("Container size [" + size + "] in [" + name + "] in pack [" + pack.getName()
                    + "] is invalid. Must be a multiple of [9] that does not exceed [54]. This has been set to [" + correctSize + "].");
        }

        final String title = reader.getNode(PackKeys.TITLE.getKey()).getString(PackKeys.TITLE.getDefaultValue());
        final int maxStackSize = reader.getNode(PackKeys.MAX_STACK_SIZE.getKey()).getInt(PackKeys.MAX_STACK_SIZE.getDefaultValue());
        final Set<StateProperty> states = Sets.newHashSet();
        final ConfigurationNode statesConfigurationNode = reader.getNode(PackKeys.STATE.getKey());
        for (Map.Entry<Object, ? extends ConfigurationNode> stateConfigurationNodes : statesConfigurationNode.getChildrenMap().entrySet()) {
            final String rawState = (String) stateConfigurationNodes.getKey();

            if ("HAS-CONTENTS".equalsIgnoreCase(rawState.toUpperCase()) || "FULL".equalsIgnoreCase(rawState.toUpperCase())) {
                final boolean enabled =
                        stateConfigurationNodes.getValue().getNode(PackKeys.ENABLED.getKey()).getBoolean(PackKeys.ENABLED.getDefaultValue());
                final String
                        textureName =
                        stateConfigurationNodes.getValue().getNode(PackKeys.TEXTURE.getKey()).getString(PackKeys.TEXTURE.getDefaultValue())
                                .split(".png")[0];
                final String
                        modelName =
                        stateConfigurationNodes.getValue().getNode(PackKeys.SHAPE.getKey()).getString(PackKeys.SHAPE.getDefaultValue())
                                .split(".shape")[0];
                PackModelContainer modelContainer = null;
                for (PackModelContainer mContainer : Pack.getModelContainers()) {
                    if (mContainer.getIdentifier().equalsIgnoreCase(modelName)) {
                        modelContainer = mContainer;
                    }
                }
                if (modelContainer == null && (Configuration.DEBUG_ALL || Configuration.DEBUG_PACKS)) {
                    Almura.LOGGER
                            .warn("Model [" + modelName + "] for state [" + rawState.toUpperCase() + "] in [" + name + "] in pack [" + pack.getName()
                                    + "] was not found. Will render as a basic cube.");
                }
                Map<Integer, List<Integer>> textureCoordinates;
                try {
                    textureCoordinates = PackUtil.parseCoordinatesFrom(
                            stateConfigurationNodes.getValue().getNode(PackKeys.TEXTURE_COORDINATES.getKey())
                                    .getList(Functions
                                            .FUNCTION_STRING_TRANSFORMER));
                } catch (NumberFormatException nfe) {
                    if (!stateConfigurationNodes.getValue().getNode(PackKeys.TEXTURE_COORDINATES.getKey()).isVirtual()) {
                        Almura.LOGGER.warn("Failed parsing texture coordinates for state [" + rawState.toUpperCase() + "] in [" + name + "] in pack ["
                                + pack.getName() + "]. " + nfe.getMessage());
                    }
                    textureCoordinates = Maps.newHashMap();
                }

                states.add(new StateProperty(pack, enabled, rawState, textureName, textureCoordinates, modelName, modelContainer));
            }
        }
        return new ContainerNode(title, correctSize, maxStackSize, states);
    }

    public static RecipeNode createRecipeNode(Pack pack, String name, Object result, ConfigurationNode node) {
        final Set<RecipeContainer<? extends IRecipe>> recipes = Sets.newHashSet();
        for (Map.Entry<Object, ? extends ConfigurationNode> entry : node.getChildrenMap().entrySet()) {
            int id;
            try {
                id = Integer.parseInt(entry.getKey().toString());
            } catch (NumberFormatException e) {
                if (Configuration.DEBUG_ALL || Configuration.DEBUG_PACKS) {
                    Almura.LOGGER
                            .error("Recipe id [" + entry.getKey() + "] in [" + name + "] in pack [" + pack.getName() + "] is not a valid number.", e);
                } else {
                    Almura.LOGGER
                            .warn("Recipe id [" + entry.getKey() + "] in [" + name + "] in pack [" + pack.getName() + "] is not a valid number.");
                }
                continue;
            }
            final String type = entry.getValue().getNode(PackKeys.TYPE.getKey()).getString(PackKeys.TYPE.getDefaultValue()).toUpperCase();
            try {
                switch (type) {
                    case "SHAPED":
                        recipes.add(createRecipeContainer(pack, name, IShapedRecipe.class, id, result, entry.getValue()));
                        break;
                    case "SHAPELESS":
                        recipes.add(createRecipeContainer(pack, name, IShapelessRecipe.class, id, result, entry.getValue()));
                        break;
                    case "SMELT":
                        recipes.add(createRecipeContainer(pack, name, ISmeltRecipe.class, id, result, entry.getValue()));
                        break;
                }
            } catch (UnknownRecipeTypeException | InvalidRecipeException | DuplicateRecipeException e) {
                if (Configuration.DEBUG_ALL || Configuration.DEBUG_PACKS) {
                    Almura.LOGGER.error(e.getMessage(), e);
                } else {
                    Almura.LOGGER.warn(e.getMessage());
                }
            }
        }
        return new RecipeNode(recipes);
    }

    public static RotationNode createRotationNode(Pack pack, String name, ConfigurationNode root) {
        final boolean rotationEnabled = root.getNode(PackKeys.ENABLED.getKey()).getBoolean(true);
        final boolean defaultRotateEnabled = root.getNode(PackKeys.DEFAULT_ROTATE.getKey()).getBoolean(PackKeys.DEFAULT_ROTATE.getDefaultValue());
        final boolean defaultMirrorRotateEnabled = root.getNode(PackKeys.DEFAULT_MIRROR_ROTATE.getKey()).getBoolean(
                PackKeys.DEFAULT_MIRROR_ROTATE.getDefaultValue());
        final EnumMap<RotationMeta.Rotation, RotationProperty> rotationProperties = Maps.newEnumMap(RotationMeta.Rotation.class);

        final ConfigurationNode directionRotationNode = root.getNode(PackKeys.DIRECTION.getKey());
        for (Map.Entry<Object, ? extends ConfigurationNode> entry : directionRotationNode.getChildrenMap().entrySet()) {
            final String rawRotation = (String) entry.getKey();
            final RotationMeta.Rotation rotation = RotationMeta.Rotation.getState(rawRotation);
            if (rotation == null) {
                Almura.LOGGER.warn("Rotation [" + rawRotation + "] in [" + name + "] in pack [" + pack.getName() + "] is not valid.");
                continue;
            }
            final boolean
                    specificDirectionRotationEnabled =
                    entry.getValue().getNode(PackKeys.ENABLED.getKey()).getBoolean(PackKeys.ENABLED.getDefaultValue());
            final float angle = entry.getValue().getNode(PackKeys.ANGLE.getKey()).getFloat(PackKeys.ANGLE.getDefaultValue());
            final RotationMeta.Direction
                    directionX =
                    RotationMeta.Direction.getState(
                            entry.getValue().getNode(PackKeys.DIRECTION_X.getKey()).getString(PackKeys.DIRECTION_X.getDefaultValue()));
            final RotationMeta.Direction
                    directionY =
                    RotationMeta.Direction.getState(
                            entry.getValue().getNode(PackKeys.DIRECTION_Y.getKey()).getString(PackKeys.DIRECTION_Y.getDefaultValue()));
            final RotationMeta.Direction
                    directionZ =
                    RotationMeta.Direction.getState(
                            entry.getValue().getNode(PackKeys.DIRECTION_Z.getKey()).getString(PackKeys.DIRECTION_Z.getDefaultValue()));
            final RotationProperty
                    rotationProperty =
                    new RotationProperty(specificDirectionRotationEnabled, rotation, angle, directionX, directionY, directionZ);
            rotationProperties.put(rotation, rotationProperty);
        }

        return new RotationNode(rotationEnabled, defaultRotateEnabled, defaultMirrorRotateEnabled, rotationProperties);
    }

    public static LightNode createLightNode(Pack pack, String name, ConfigurationNode root) {
        float emission = root.getNode(PackKeys.EMISSION.getKey()).getFloat(PackKeys.EMISSION.getDefaultValue());
        if (emission < 0f) {
            emission = 0f;
        }
        if (emission > 1f) {
            emission = emission / 15f;
        }
        int opacity = root.getNode(PackKeys.OPACITY.getKey()).getInt(PackKeys.OPACITY.getDefaultValue());
        if (opacity < 0) {
            opacity = 0;
        }
        if (opacity > 255) {
            opacity = 255;
        }
        final ConfigurationNode lightRequiredConfigurationNode = root.getNode(PackKeys.REQUIRED.getKey());
        final boolean enabled = lightRequiredConfigurationNode.getNode(PackKeys.ENABLED.getKey()).getBoolean(PackKeys.ENABLED.getDefaultValue());
        final int min = lightRequiredConfigurationNode.getNode(PackKeys.MIN.getKey()).getInt(PackKeys.MIN.getDefaultValue());
        final int max = lightRequiredConfigurationNode.getNode(PackKeys.MAX.getKey()).getInt(PackKeys.MAX.getDefaultValue());
        return new LightNode(emission, opacity, new RangeProperty<>(Integer.class, enabled, min, max));
    }

    @SuppressWarnings("unchecked")
    public static CollisionNode createCollisionNode(Pack pack, String name, ConfigurationNode root) {
        final boolean collisionEnabled = root.getNode(PackKeys.ENABLED.getKey()).getBoolean(PackKeys.ENABLED.getDefaultValue());
        final ConfigurationNode collisionSourcesConfigurationNode = root.getNode(PackKeys.SOURCES.getKey());
        final Set<CollisionProperty> collisionProperties = Sets.newHashSet();
        for (Map.Entry<Object, ? extends ConfigurationNode> entry : collisionSourcesConfigurationNode.getChildrenMap().entrySet()) {
            final String rawCollisionSource = (String) entry.getKey();
            final Pair<String, String> collisionSourceModidIdenfifier = GameObjectMapper.parseModidIdentifierFrom(rawCollisionSource);
            final Optional<Class<? extends Entity>> entityClazz = EntityMapper.getEntityClassRemapped(collisionSourceModidIdenfifier.getKey(),
                    collisionSourceModidIdenfifier.getValue());
            if (!entityClazz.isPresent()) {
                Almura.LOGGER.warn("Entity source [" + collisionSourceModidIdenfifier.getValue() + "] in [" + name + "] for mod ["
                        + collisionSourceModidIdenfifier.getKey() + "] in pack [" + pack.getName() + "] is not a registered entity.");
                continue;
            }
            final boolean
                    enabled = entry.getValue().getNode(PackKeys.ENABLED.getKey()).getBoolean(PackKeys.ENABLED.getDefaultValue());

            final RangeProperty<Float>
                    healthRange =
                    new RangeProperty<>(Float.class, true, PackUtil.getRange(Float.class,
                            entry.getValue()
                                    .getNode(PackKeys.HEALTH_CHANGE.getKey())
                                    .getString(PackKeys.HEALTH_CHANGE.getDefaultValue()), 0f));
            collisionProperties.add(new CollisionProperty(enabled, entityClazz.get(), healthRange));
        }

        return new CollisionNode(collisionEnabled, collisionProperties);
    }

    public static RenderNode createRenderNode(Pack pack, String name, ConfigurationNode root) {
        final boolean renderAsNormalBlock = root.getNode(PackKeys.NORMAL_CUBE.getKey()).getBoolean(PackKeys.NORMAL_CUBE.getDefaultValue());
        final boolean renderAsOpaque = root.getNode(PackKeys.OPAQUE.getKey()).getBoolean(PackKeys.OPAQUE.getDefaultValue());
        return new RenderNode(renderAsNormalBlock, renderAsOpaque);
    }

    public static BreakNode createBreakNode(Pack pack, String name, Block block, boolean addDefault, ConfigurationNode root) {
        final boolean breakEnabled = root.getNode(PackKeys.ENABLED.getKey()).getBoolean(true);
        final ConfigurationNode toolsConfigurationNode = root.getNode(PackKeys.TOOLS.getKey());
        final Set<ToolsNode> tools = Sets.newHashSet();

        for (Map.Entry<Object, ? extends ConfigurationNode> toolConfigurationNodes : toolsConfigurationNode.getChildrenMap().entrySet()) {
            final String rawToolSource = (String) toolConfigurationNodes.getKey();
            final Pair<String, String> toolSourceModIdIdentifier = GameObjectMapper.parseModidIdentifierFrom(rawToolSource);
            final Optional<GameObject>
                    tool =
                    GameObjectMapper.getGameObject(toolSourceModIdIdentifier.getKey(), toolSourceModIdIdentifier.getValue(), true);
            if (!rawToolSource.equals("none") && !tool.isPresent()) {
                Almura.LOGGER
                        .warn("Tool source [" + toolSourceModIdIdentifier.getValue() + "] in [" + name + "] for mod [" + toolSourceModIdIdentifier
                                .getKey() + "] in pack [" + pack.getName()
                                + "] is not a registered block or item.");
                continue;
            }
            final RangeProperty<Integer>
                    experienceRange =
                    new RangeProperty<>(Integer.class, true, PackUtil.getRange(Integer.class,
                            toolConfigurationNodes.getValue().getNode(PackKeys.RANGE_EXPERIENCE.getKey())
                                    .getString(PackKeys.RANGE_EXPERIENCE.getDefaultValue()), 0));
            final RangeProperty<Float>
                    exhaustionRange =
                    new RangeProperty<>(Float.class, true, PackUtil.getRange(Float.class,
                            toolConfigurationNodes.getValue().getNode(PackKeys.EXHAUSTION_CHANGE.getKey())
                                    .getString(PackKeys.EXHAUSTION_CHANGE.getDefaultValue()),
                            0.025F));
            final ConfigurationNode dropsConfigurationNode = toolConfigurationNodes.getValue().getNode(PackKeys.DROPS.getKey());
            final Set<DropProperty> drops = Sets.newHashSet();

            for (Map.Entry<Object, ? extends ConfigurationNode> dropConfigurationNodes : dropsConfigurationNode.getChildrenMap().entrySet()) {
                final String rawDropSource = (String) dropConfigurationNodes.getKey();
                final Pair<String, String> dropSourceModIdIdentifier = GameObjectMapper.parseModidIdentifierFrom(rawDropSource);
                final Optional<GameObject> drop = GameObjectMapper.getGameObject(rawDropSource, true);
                if (!drop.isPresent()) {
                    Almura.LOGGER
                            .warn("Drop source [" + dropSourceModIdIdentifier.getValue() + "] in [" + name + "] for mod [" + dropSourceModIdIdentifier
                                    .getKey()
                                    + "] in pack [" + pack.getName()
                                    + "] is not a registered block or item.");
                    continue;
                }
                final RangeProperty<Integer>
                        amountRange =
                        new RangeProperty<>(Integer.class, true, PackUtil.getRange(Integer.class,
                                dropConfigurationNodes.getValue().getNode(PackKeys.AMOUNT.getKey())
                                        .getString(PackKeys.AMOUNT.getDefaultValue()), 1));
                final int data = dropConfigurationNodes.getValue().getNode(PackKeys.DATA.getKey()).getInt(PackKeys.DATA.getDefaultValue());
                final ConfigurationNode bonusConfigurationNode = dropConfigurationNodes.getValue().getNode(PackKeys.BONUS.getKey());
                final boolean
                        bonusEnabled =
                        bonusConfigurationNode.getNode(PackKeys.ENABLED.getKey()).getBoolean(PackKeys.ENABLED.getDefaultValue());
                final RangeProperty<Integer>
                        bonusAmountRange =
                        new RangeProperty<>(Integer.class, true, PackUtil.getRange(Integer.class,
                                bonusConfigurationNode.getNode(PackKeys.AMOUNT.getKey())
                                        .getString(PackKeys.AMOUNT.getDefaultValue()), 1));
                final RangeProperty<Double>
                        bonusChanceRange =
                        new RangeProperty<>(Double.class, true, PackUtil.getRange(Double.class,
                                bonusConfigurationNode.getNode(PackKeys.CHANCE.getKey())
                                        .getString(PackKeys.CHANCE.getDefaultValue()), 100.0));
                drops.add(new DropProperty(drop.get(), amountRange, data,
                        new BonusProperty<>(Integer.class, bonusEnabled, bonusAmountRange, bonusChanceRange)));
            }

            tools.add(!tool.isPresent() ? new ToolsNode.OffHand(experienceRange, exhaustionRange, new DropsNode(drops))
                    : new ToolsNode(tool.get(), experienceRange, exhaustionRange, new DropsNode(drops)));
        }

        if (tools.isEmpty() && addDefault) {
            Set<DropProperty> temp = Sets.newConcurrentHashSet();
            temp.add(new DropProperty(GameObjectMapper.getGameObject(Almura.MOD_ID,
                    ((IPackObject) block).getPack().getName() + "\\" + ((IPackObject) block)
                            .getIdentifier(), false).get(),
                    new RangeProperty<>(Integer.class, true, new ImmutablePair<>(1, 1)), 0,
                    new BonusProperty<>(Integer.class, false, 0, 0,
                            new RangeProperty<>(Double.class, false, new ImmutablePair<>(0D, 0D)))));
            tools.add(new ToolsNode.OffHand(new RangeProperty<>(Integer.class, true, new ImmutablePair<>(1, 1)),
                    new RangeProperty<>(Float.class, true, new ImmutablePair<>(0f, 0f)), new DropsNode(temp)));
        }
        return new BreakNode(breakEnabled, tools);
    }

    public static ConsumptionNode createConsumptionNode(Pack pack, String name, ConfigurationNode root) {
        final RangeProperty<Integer>
                foodRange =
                new RangeProperty<>(Integer.class, true, PackUtil.getRange(Integer.class,
                        root.getNode(PackKeys.FOOD_CHANGE.getKey())
                                .getString(PackKeys.FOOD_CHANGE.getDefaultValue()), 0));
        final RangeProperty<Float>
                saturationRange =
                new RangeProperty<>(Float.class, true, PackUtil.getRange(Float.class,
                        root.getNode(PackKeys.SATURATION_CHANGE.getKey())
                                .getString(PackKeys.SATURATION_CHANGE.getDefaultValue()), 0f));

        final RangeProperty<Float>
                healthRange =
                new RangeProperty<>(Float.class, true, PackUtil.getRange(Float.class,
                        root.getNode(PackKeys.HEALTH_CHANGE.getKey())
                                .getString(PackKeys.HEALTH_CHANGE.getDefaultValue()), 0f));

        final boolean wolfFavorite = root.getNode(PackKeys.WOLF_FAVORITE.getKey()).getBoolean(PackKeys.WOLF_FAVORITE.getDefaultValue());
        final boolean alwaysEdible = root.getNode(PackKeys.ALWAYS_EDIBLE.getKey()).getBoolean(PackKeys.ALWAYS_EDIBLE.getDefaultValue());
        return new ConsumptionNode(true, foodRange, saturationRange, healthRange, alwaysEdible, wolfFavorite);
    }

    public static GrassNode createGrassNode(Pack pack, String name, PackSeeds seed, ConfigurationNode root) {
        final boolean enabled = root.getNode(PackKeys.ENABLED.getKey()).getBoolean(PackKeys.ENABLED.getDefaultValue());
        final String amountRaw = root.getNode(PackKeys.AMOUNT.getKey()).getString(PackKeys.AMOUNT.getDefaultValue());
        Pair<Integer, Integer> amountPair;
        try {
            amountPair = PackUtil.getRange(Integer.class, amountRaw, 1);
        } catch (NumberFormatException nfe) {
            Almura.LOGGER
                    .warn("Amount given for seed [" + seed.getIdentifier() + "] in [" + name + "] in pack [" + pack
                            + "] is not valid. Should be in the format of 1-3.");
            amountPair = new ImmutablePair<>(1, 1);
        }
        final String chanceRaw = root.getNode(PackKeys.CHANCE.getKey()).getString(PackKeys.CHANCE.getDefaultValue());
        Pair<Double, Double> chancePair;
        try {
            chancePair = PackUtil.getRange(Double.class, chanceRaw, 100.0);
        } catch (NumberFormatException nfe) {
            Almura.LOGGER.warn("Chance given for [" + seed.getIdentifier() + "] in pack [" + pack
                    + "] is not valid. Should be in the format of 10.5-20.5.");
            chancePair = new ImmutablePair<>(100.0, 100.0);
        }

        return new GrassNode(enabled, new VariableGameObjectProperty(new GameObject(Almura.MOD_ID, seed, "", 0),
                new RangeProperty<>(Integer.class, true, amountPair)),
                new RangeProperty<>(Double.class, true, chancePair));
    }

    public static SoilNode createSoilNode(Pack pack, String name, ConfigurationNode node) {
        final Pair<String, String>
                soilSourceModidIdentifier =
                GameObjectMapper.parseModidIdentifierFrom(node.getNode(PackKeys.SOURCE.getKey()).getString(PackKeys.SOURCE.getDefaultValue()));
        final Optional<GameObject>
                source =
                GameObjectMapper.getGameObject(soilSourceModidIdentifier.getKey(), soilSourceModidIdentifier.getValue(), false);
        if (!source.isPresent()) {
            Almura.LOGGER.warn("Soil source [" + soilSourceModidIdentifier.getValue() + "] in [" + name + "] for mod [" + soilSourceModidIdentifier
                    .getKey() + "] in pack [" + pack.getName() + "] is not a registered block.");
            return null;
        }

        if (!source.get().isBlock()) {
            Almura.LOGGER.warn(
                    "Soil source [" + soilSourceModidIdentifier.getValue() + "] in [" + name + "] for mod [" + soilSourceModidIdentifier.getKey()
                            + "] in pack [" + pack.getName() + "] is not a block.");
            return null;
        }

        final ConfigurationNode biomeConfigurationNode = node.getNode(PackKeys.NODE_BIOME.getKey());
        final boolean biomeNodeEnabled = biomeConfigurationNode.getNode(PackKeys.ENABLED.getKey()).getBoolean(PackKeys.ENABLED.getDefaultValue());
        final ConfigurationNode biomeSourcesConfigurationNode = biomeConfigurationNode.getNode(PackKeys.SOURCES.getKey());
        final Set<BiomeProperty> biomes = Sets.newHashSet();
        for (Map.Entry<Object, ? extends ConfigurationNode> biomeSourceConfigurationNodes : biomeSourcesConfigurationNode.getChildrenMap()
                .entrySet()) {
            final String rawBiomeSource = (String) biomeSourceConfigurationNodes.getKey();
            final BiomeGenBase biome = PackUtil.getBiome(rawBiomeSource);
            if (biome == null) {
                Almura.LOGGER
                        .warn("Biome source [" + rawBiomeSource + "] in [" + name + "] in pack [" + pack.getName() + "] is not a registered biome.");
                continue;
            }
            final Pair<Double, Double>
                    temperaturePair =
                    PackUtil.getRange(Double.class, biomeSourceConfigurationNodes.getValue().getNode(PackKeys.TEMPERATURE_REQUIRED.getKey())
                            .getString(PackKeys.TEMPERATURE_REQUIRED.getDefaultValue()), 100.0);
            final Pair<Double, Double>
                    humidityPair =
                    PackUtil.getRange(Double.class, biomeSourceConfigurationNodes.getValue().getNode(PackKeys.HUMIDITY_REQUIRED.getKey())
                            .getString(PackKeys.HUMIDITY_REQUIRED.getDefaultValue()), 100.0);
            biomes.add(new BiomeProperty(biome, new RangeProperty<>(Double.class, true, temperaturePair),
                    new RangeProperty<>(Double.class, true, humidityPair)));
        }
        final BiomeNode biomeNode = new BiomeNode(biomeNodeEnabled, biomes);
        return new SoilNode(source.get(), biomeNode);
    }

    private static GrowthNode createGrowthNode(Pack pack, String name, ConfigurationNode reader) {
        final Pair<Double, Double>
                chancePair =
                PackUtil.getRange(Double.class, reader.getNode(PackKeys.CHANCE.getKey()).getString(PackKeys.CHANCE.getDefaultValue()), 100.0);
        return new GrowthNode(new RangeProperty<>(Double.class, true, chancePair));
    }

    private static FuelNode createFuelNode(Pack pack, String name, ConfigurationNode reader) {
        final boolean isEnabled = reader.getNode(PackKeys.ENABLED.getKey()).getBoolean(true);
        final int maxBurnTime = reader.getNode(PackKeys.MAX_BURN_TIME.getKey()).getInt(PackKeys.MAX_BURN_TIME.getDefaultValue());
        return new FuelNode(isEnabled, maxBurnTime);
    }

    public static FertilizerNode createFertilizerNode(Pack pack, String name, int stageId, ConfigurationNode reader) {
        final boolean isEnabled = reader.getNode(PackKeys.ENABLED.getKey()).getBoolean(false);
        final Set<GameObjectProperty> value = Sets.newHashSet();

        for (Map.Entry<Object, ? extends ConfigurationNode> fertilizerSourceNodes : reader.getNode(PackKeys.SOURCES.getKey()).getChildrenMap()
                .entrySet()) {
            final String rawFertilizerSource = (String) fertilizerSourceNodes.getKey();
            final Optional<GameObject> gameObject = GameObjectMapper.getGameObject(rawFertilizerSource, true);
            if (!gameObject.isPresent()) {
                Almura.LOGGER
                        .warn("Fertilizer [" + rawFertilizerSource + "] in stage [" + stageId + "] in [" + name + "] in pack [" + pack.getName()
                                + "] is not a registered block or item");
                continue;
            }

            final RangeProperty<Integer>
                    amountRange =
                    new RangeProperty<>(Integer.class, true, PackUtil.getRange(Integer.class,
                            fertilizerSourceNodes.getValue().getNode(PackKeys.AMOUNT
                                    .getKey())
                                    .getString(PackKeys.AMOUNT.getDefaultValue()), 1));
            value.add(new VariableGameObjectProperty(gameObject.get(), amountRange));
        }

        return new FertilizerNode(isEnabled, value);
    }

    private static <R extends IRecipe> RecipeContainer<R> createRecipeContainer(Pack pack, String name, Class<? extends R> clazz, int id, Object res,
            ConfigurationNode reader)
            throws InvalidRecipeException, UnknownRecipeTypeException, DuplicateRecipeException {
        final int amount = reader.getNode(PackKeys.AMOUNT.getKey()).getInt(1);
        final int data = reader.getNode(PackKeys.DATA.getKey()).getInt(PackKeys.DATA.getDefaultValue());

        final ItemStack result;
        if (res instanceof Block) {
            result = new ItemStack((Block) res, amount, data);
        } else {
            result = new ItemStack((Item) res, amount, data);
        }

        List<Object> params = Lists.newLinkedList();

        if (clazz == IShapedRecipe.class || clazz == IShapelessRecipe.class) {
            for (String itemsRaw : reader.getNode(PackKeys.INGREDIENTS.getKey()).getList(Functions.FUNCTION_STRING_TRANSFORMER)) {
                final String[] itemsSplit = itemsRaw.split(" ");
                for (String identifierCombined : itemsSplit) {
                    final String[] identifierAmountSplit = identifierCombined.split(StringEscapeUtils.escapeJava(":"));
                    int ingredientAmount = 1;
                    if (identifierAmountSplit.length == 2) {
                        ingredientAmount = Integer.parseInt(identifierAmountSplit[1]);
                    }
                    final Optional<GameObject> gameObject = GameObjectMapper.getGameObject(identifierAmountSplit[0], true);
                    if (!gameObject.isPresent()) {
                        throw new InvalidRecipeException(
                                "Recipe [" + id + "] of type [" + clazz.getSimpleName().toUpperCase() + "] in [" + name + "] in pack [" + pack
                                        .getName() + "] cannot be registered. Ingredient [" + identifierCombined
                                        + "] is not a registered block or item.");
                    } else {
                        if (gameObject.get().isBlock()) {
                            final Item itemBlock = Item.getItemFromBlock((Block) gameObject.get().minecraftObject);
                            if (itemBlock != null) {
                                params.add(new ItemStack((Block) gameObject.get().minecraftObject, ingredientAmount, gameObject.get().data));
                            } else if (gameObject.get().minecraftObject instanceof BlockAir) {
                                params.add(gameObject.get().minecraftObject);
                            } else {
                                throw new InvalidRecipeException(
                                        "Game Object [" + gameObject.get().minecraftObject + "] of type [BLOCK] in recipe [" + id + "] of type ["
                                                + clazz.getSimpleName().toUpperCase() + "] in [" + name + "] in pack [" + pack.getName()
                                                + "] has no registered ItemBlock.");
                            }
                        } else if (gameObject.get().minecraftObject instanceof Item) {
                            params.add(new ItemStack((Item) gameObject.get().minecraftObject, ingredientAmount, gameObject.get().data));
                        }
                    }
                }
            }
        } else {
            final String itemRaw = reader.getNode(PackKeys.INPUT.getKey()).getString(PackKeys.INPUT.getDefaultValue());
            final Optional<GameObject> gameObject = GameObjectMapper.getGameObject(itemRaw, true);
            if (!gameObject.isPresent()) {
                throw new InvalidRecipeException(
                        "Recipe [" + id + "] of type [" + clazz.getSimpleName().toUpperCase() + "] in [" + name + "] in pack [" + pack.getName()
                                + "] cannot be registered. Input [" + itemRaw + "] is not a registered block or item.");
            } else {
                if (gameObject.get().isBlock()) {
                    final Item itemBlock = Item.getItemFromBlock((Block) gameObject.get().minecraftObject);
                    if (itemBlock != null) {
                        params.add(new ItemStack((Block) gameObject.get().minecraftObject, 1, gameObject.get().data));
                    } else if (gameObject.get().minecraftObject instanceof BlockAir) {
                        params.add(gameObject.get().minecraftObject);
                    } else {
                        throw new InvalidRecipeException(
                                "Game Object [" + gameObject.get().minecraftObject + "] of type [BLOCK] in recipe [" + id + "] of type [" + clazz
                                        .getSimpleName().toUpperCase() + "] in [" + name + "] in pack [" + pack.getName()
                                        + "] has no registered ItemBlock.");
                    }
                } else if (gameObject.get().minecraftObject instanceof Item) {
                    params.add(new ItemStack((Item) gameObject.get().minecraftObject, 1, gameObject.get().data));
                }
            }
            params.add(reader.getNode(PackKeys.EXPERIENCE.getKey()).getFloat(PackKeys.EXPERIENCE.getDefaultValue()));
        }

        if (params.isEmpty()) {
            throw new InvalidRecipeException(
                    "Recipe [" + id + "] of type [" + clazz.getSimpleName().toUpperCase() + "] in [" + name + "] in pack [" + pack.getName()
                            + "] has no parameters.");
        }

        if (clazz == IShapedRecipe.class) {
            int index = 0;
            final Map<Object, Character> objectViaParamMap = Maps.newLinkedHashMap();

            final List<Object> combinedParams = Lists.newLinkedList();

            StringBuilder lineMatrixBuilder = new StringBuilder();
            for (Object param : params) {
                if (param.getClass() != BlockAir.class) {
                    Character c = objectViaParamMap.get(param);
                    if (c == null) {
                        c = RECIPE_MATRIX_PLACEHOLDER[index];
                        objectViaParamMap.put(param, c);
                        index++;
                    }
                    lineMatrixBuilder.append(c);
                } else {
                    lineMatrixBuilder.append(" ");
                }
                if (lineMatrixBuilder.length() == 3) {
                    combinedParams.add(lineMatrixBuilder.toString());
                    lineMatrixBuilder = new StringBuilder();
                }
            }
            for (Map.Entry<Object, Character> entry : objectViaParamMap.entrySet()) {
                combinedParams.add(entry.getValue());
                combinedParams.add(entry.getKey());
            }
            params = combinedParams;
        }

        return new RecipeContainer<>(pack, name, id, RecipeManager.registerRecipe(pack, name, id, clazz, result, params));
    }
}
