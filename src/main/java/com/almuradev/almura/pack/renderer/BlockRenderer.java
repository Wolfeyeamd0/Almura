/**
 * This file is part of Almura, All Rights Reserved.
 *
 * Copyright (c) 2014 AlmuraDev <http://github.com/AlmuraDev/>
 */
package com.almuradev.almura.pack.renderer;

import com.almuradev.almura.Almura;
import com.almuradev.almura.pack.IBlockClipContainer;
import com.almuradev.almura.pack.IBlockModelContainer;
import com.almuradev.almura.pack.IClipContainer;
import com.almuradev.almura.pack.INodeContainer;
import com.almuradev.almura.pack.PackUtil;
import com.almuradev.almura.pack.RotationMeta;
import com.almuradev.almura.pack.model.IModel;
import com.almuradev.almura.pack.model.PackFace;
import com.almuradev.almura.pack.model.PackModelContainer;
import com.almuradev.almura.pack.node.LightNode;
import com.almuradev.almura.pack.node.RotationNode;
import com.almuradev.almura.pack.node.property.RotationProperty;
import com.google.common.base.Optional;
import net.malisis.core.renderer.MalisisRenderer;
import net.malisis.core.renderer.RenderParameters;
import net.malisis.core.renderer.RenderType;
import net.malisis.core.renderer.element.Face;
import net.malisis.core.renderer.element.Shape;
import net.malisis.core.renderer.element.Vertex;
import net.malisis.core.renderer.element.shape.Cube;
import net.malisis.core.renderer.icon.ClippedIcon;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;
import org.lwjgl.opengl.GL11;

public class BlockRenderer extends MalisisRenderer {

    private Cube cubeModel;

    @Override
    protected void initialize() {
        cubeModel = new Cube();
    }

    @Override
    public void render() {
        shape = cubeModel;
        final Optional<PackModelContainer> modelContainer = ((IBlockModelContainer) block).getModelContainer(world, x, y, z, blockMetadata);
        if (modelContainer.isPresent()) {
            if (modelContainer.get().getModel().isPresent()) {
                final IModel model = modelContainer.get().getModel().get();
                if (model instanceof PackModelContainer.PackShape) {
                    shape = (PackModelContainer.PackShape) model;
                }
            }
        }

        shape.resetState();
        enableBlending();
        rp.useBlockBounds.set(false);

        if (shape instanceof IModel) {
            rp.renderAllFaces.set(true);
            rp.flipU.set(true);
            rp.flipV.set(true);
            rp.interpolateUV.set(false);
        } else {
            rp.flipU.set(false);
            rp.flipV.set(false);
            rp.interpolateUV.set(true);
        }

        if (shape instanceof IModel) {
            if (renderType == RenderType.ISBRH_WORLD && block instanceof INodeContainer) {
                if (handleRotation((IModel) shape)) {
                    shape.applyMatrix();
                    shape.deductParameters();
                }
            }

            if (renderType == RenderType.ISBRH_INVENTORY) {
                handleScaling((IModel) shape);
            }
        }

        drawShape(shape, rp);
    }

    @Override
    protected IIcon getIcon(RenderParameters params) {
        if (face instanceof PackFace) {
            final PackFace pface = (PackFace) face;
            final ClippedIcon[] clippedIcons;

            if (world != null) {
                clippedIcons = ((IBlockClipContainer) block).getClipIcons(world, x, y, z, blockMetadata);
            } else {
                clippedIcons = ((IClipContainer) block).getClipIcons();
            }

            if (!PackUtil.isEmptyClip(clippedIcons)) {
                if (pface.getTextureId() >= clippedIcons.length) {
                    params.icon.set(clippedIcons[0]);
                } else {
                    final ClippedIcon toSet = clippedIcons[pface.getTextureId()];
                    if (toSet == null) {
                        params.icon.set(clippedIcons[0]);
                    } else {
                        params.icon.set(toSet);
                    }
                }
            }
        }
        return super.getIcon(params);
    }

    // This override is needed because we set our own light factor values and ignore Ambient Occlusion.
    @Override
    protected int calcVertexColor(Vertex vertex, int[][] aoMatrix) {
        int color = 0xFFFFFF;

        // Vertex should use their own colors
        if (params.usePerVertexColor.get()) {
            color = vertex.getColor();
        }

        // Global color multiplier is set
        if (params.colorMultiplier.get() != null) {
            color = params.colorMultiplier.get();

        // Use block color multiplier
        } else if (block != null) {
            color = world != null ? block.colorMultiplier(world, x, y, z) : block.getRenderColor(blockMetadata);
        }

        // No AO for lines
        if (drawMode == GL11.GL_LINE) {
            return color;
        }

        // No AO for item/inventories
        if (renderType != RenderType.ISBRH_WORLD && renderType != RenderType.TESR_WORLD) {
            return color;
        }

        float factor = 1;

        // Calculate AO
        if (params.calculateAOColor.get() && aoMatrix != null && Minecraft.isAmbientOcclusionEnabled()
                && block.getLightValue(world, x, y, z) == 0) {
            factor = getBlockAmbientOcclusion(world, x + params.direction.get().offsetX, y + params.direction.get().offsetY, z
                    + params.direction.get().offsetZ);

            for (int[] anAoMatrix : aoMatrix) {
                factor += getBlockAmbientOcclusion(world, x + anAoMatrix[0], y + anAoMatrix[1], z + anAoMatrix[2]);
            }

            factor /= (aoMatrix.length + 1);
        }

        // Apply face dependent shading
        factor *= params.colorFactor.get();

        // Following additional check is to see if the block being rendered should respect Ambient Occlusions light factor or skip it.
        // Almura's light type block have always appeared in bright white.
        // Almura Start
        if (block instanceof INodeContainer) {
            LightNode node = ((INodeContainer) block).getNode(LightNode.class);
            if (node != null && node.getEmission() > 0) {
                factor = 1;
            }
        }
        // Almura End
        int r = (int) ((color >> 16 & 255) * factor);
        int g = (int) ((color >> 8 & 255) * factor);
        int b = (int) ((color & 255) * factor);

        color = r << 16 | g << 8 | b;

        return color;
    }

    // The following method override is neede because Almura's original shape format was flawed. 
    @Override
    public void applyTexture(Shape shape, RenderParameters parameters) {
        // shape.applyMatrix();
        for (Face f : shape.getFaces()) {
            face = f;
            RenderParameters params = new RenderParameters();
            params.merge(f.getParameters());
            params.merge(parameters);

            IIcon icon = getIcon(params);
            if (icon != null) {
                boolean flipU = params.flipU.get();
                boolean flipV = params.flipV.get();

                // Almura Start - Generic Shapes don't invert the NORTH / EAST face.
                if ((params.direction.get() == ForgeDirection.NORTH || params.direction.get() == ForgeDirection.EAST) && !(shape instanceof Cube)) {
                    // Only our shapes have this flaw due to it being wrong in Spoutcraft
                    flipU = !flipU;
                }
                // Almura End

                f.setTexture(icon, flipU, flipV, params.interpolateUV.get());
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerFor(Class... listClass) {
        for (Class clazz : listClass) {
            if (Block.class.isAssignableFrom(clazz) && IBlockClipContainer.class.isAssignableFrom(clazz) && IBlockModelContainer.class
                    .isAssignableFrom(clazz) && INodeContainer.class.isAssignableFrom(clazz)) {
                super.registerFor(clazz);
            } else {
                Almura.LOGGER.error("Cannot register [" + clazz.getSimpleName() + "] for " + getClass().getSimpleName());
            }
        }
    }

    @Override
    public void registerFor(Item item) {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " is only meant for blocks.");
    }

    private boolean handleRotation(IModel model) {
        final RotationNode node = ((INodeContainer) block).getNode(RotationNode.class);
        if (node == null) {
            return false;
        }
        if (!node.isEnabled()) {
            return false;
        }
        final RotationMeta.Rotation rotation = RotationMeta.Rotation.getState(blockMetadata);
        final RotationProperty property = node.getRotationProperty(rotation);
        if (property == null) {
            switch (rotation) {
                case NORTH:
                    if (node.isDefaultRotate()) {
                        model.rotate(180f, 0, -1, 0);
                    }
                    break;
                case SOUTH:
                    break;
                case WEST:
                    if (node.isDefaultRotate()) {
                        model.rotate(90f, 0, -1, 0);
                    }
                    break;
                case EAST:
                    if (node.isDefaultRotate()) {
                        model.rotate(90f, 0, 1, 0);
                    }
                    break;
                case DOWN_NORTH:
                    if (node.isDefaultRotate()) {
                        model.rotate(180f, 0, -1, 0);
                    }
                    if (node.isDefaultMirrorRotate()) {
                        model.rotate(90f, -1, 0, 0);
                    }
                    break;
                case DOWN_SOUTH:
                    break;
                case DOWN_WEST:
                    if (node.isDefaultRotate()) {
                        model.rotate(90f, 0, -1, 0);
                    }
                    if (node.isDefaultMirrorRotate()) {
                        model.rotate(180f, -1, 0, 0);
                    }
                    break;
                case DOWN_EAST:
                    if (node.isDefaultRotate()) {
                        model.rotate(90f, 0, 1, 0);
                    }
                    if (node.isDefaultMirrorRotate()) {
                        model.rotate(180f, -1, 0, 0);
                    }
                    break;
                case UP_NORTH:
                    if (node.isDefaultRotate()) {
                        model.rotate(180f, 0, -1, 0);
                    }
                    break;
                case UP_SOUTH:
                    break;
                case UP_WEST:
                    if (node.isDefaultRotate()) {
                        model.rotate(90f, 0, -1, 0);
                    }
                    break;
                case UP_EAST:
                    if (node.isDefaultRotate()) {
                        model.rotate(90f, 0, 1, 0);
                    }
                    break;
            }
        } else {
            model.rotate(property.getAngle(), property.getX().getId(), property.getY().getId(), property.getZ().getId());
        }

        return true;
    }

    private void handleScaling(IModel model) {
        double max = Double.MIN_VALUE;
        for (Face fe : model.getFaces()) {
            for (Vertex vt : model.getVertexes(fe)) {
                max = Math.max(vt.getX(), max);
                max = Math.max(vt.getY(), max);
                max = Math.max(vt.getZ(), max);
            }
        }
        model.scale((float) (1 / max));
    }
}
