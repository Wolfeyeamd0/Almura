/**
 * This file is part of Almura, All Rights Reserved.
 *
 * Copyright (c) 2014 AlmuraDev <http://github.com/AlmuraDev/>
 */
package com.almuradev.almura.pack.block;

import com.almuradev.almura.Almura;
import com.almuradev.almura.Configuration;
import com.almuradev.almura.pack.IBlockClipContainer;
import com.almuradev.almura.pack.IBlockModelContainer;
import com.almuradev.almura.pack.IItemBlockInformation;
import com.almuradev.almura.pack.INodeContainer;
import com.almuradev.almura.pack.IPackObject;
import com.almuradev.almura.pack.Pack;
import com.almuradev.almura.pack.PackUtil;
import com.almuradev.almura.pack.RotationMeta;
import com.almuradev.almura.pack.mapper.GameObject;
import com.almuradev.almura.pack.model.PackModelContainer;
import com.almuradev.almura.pack.node.BreakNode;
import com.almuradev.almura.pack.node.CollisionNode;
import com.almuradev.almura.pack.node.INode;
import com.almuradev.almura.pack.node.LightNode;
import com.almuradev.almura.pack.node.RenderNode;
import com.almuradev.almura.pack.node.RotationNode;
import com.almuradev.almura.pack.node.ToolsNode;
import com.almuradev.almura.pack.node.event.AddNodeEvent;
import com.almuradev.almura.pack.node.property.DropProperty;
import com.almuradev.almura.pack.node.property.RangeProperty;
import com.almuradev.almura.pack.renderer.PackIcon;
import com.almuradev.almura.tabs.Tabs;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderState;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.malisis.core.renderer.icon.ClippedIcon;
import net.malisis.core.util.EntityUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public class PackBlock extends Block implements IPackObject, IBlockClipContainer, IBlockModelContainer, INodeContainer, IItemBlockInformation {

    public static int renderId;
    private static final int[][] field_150150_a = new int[][] {{2, 6}, {3, 7}, {2, 3}, {6, 7}, {0, 4}, {1, 5}, {0, 1}, {4, 5}};
    private final Pack pack;
    private final String identifier;
    private final Map<Integer, List<Integer>> textureCoordinates;
    private final String modelName;
    private final ConcurrentMap<Class<? extends INode<?>>, INode<?>> nodes = Maps.newConcurrentMap();
    private final String textureName;
    private final List<String> tooltip;
    private ClippedIcon[] clippedIcons;
    private Optional<PackModelContainer> modelContainer;
    private RenderNode renderNode;
    private RotationNode rotationNode;
    private BreakNode breakNode;
    private CollisionNode collisionNode;
    // ======= Stairs Logic ==============
    private boolean stairsLogic;
    private int field_150153_O;
    private boolean field_150152_N;

    public PackBlock(Pack pack, String identifier, List<String> tooltip, String textureName, Map<Integer, List<Integer>> textureCoordinates,
                     String modelName,
                     PackModelContainer modelContainer, float hardness, float resistance, boolean showInCreativeTab, String creativeTabName,
                     RotationNode rotationNode, LightNode lightNode, RenderNode renderNode, boolean stairsLogic) {
        super(Material.ground);
        this.pack = pack;
        this.identifier = identifier;
        this.textureCoordinates = textureCoordinates;
        this.textureName = textureName;
        this.modelName = modelName;
        this.renderNode = addNode(renderNode);
        this.rotationNode = addNode(rotationNode);
        this.tooltip = tooltip;
        this.stairsLogic = stairsLogic;

        setModelContainer(modelContainer);
        addNode(rotationNode);
        addNode(lightNode);
        addNode(renderNode);
        setBlockName(pack.getName() + "\\" + identifier);
        setBlockTextureName(Almura.MOD_ID + ":images/" + textureName);
        setHardness(hardness);
        setResistance(resistance);
        setLightLevel(lightNode.getEmission());
        setLightOpacity(lightNode.getOpacity());
        if (showInCreativeTab) {
            setCreativeTab(Tabs.getTabByName(creativeTabName));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderType() {
        return renderId;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        //Almura can run last
        if (!Loader.instance().hasReachedState(LoaderState.AVAILABLE)) {
            return;
        }
        blockIcon = new PackIcon(this, textureName).register((TextureMap) register);
        clippedIcons = PackUtil.generateClippedIconsFromCoordinates(blockIcon, textureName, textureCoordinates);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int type) {
        if (PackUtil.isEmptyClip(clippedIcons)) {
            return super.getIcon(side, type);
        }
        ClippedIcon sideIcon;
        if (side >= clippedIcons.length) {
            sideIcon = clippedIcons[0];
        } else {
            sideIcon = clippedIcons[side];
            if (sideIcon == null) {
                sideIcon = clippedIcons[0];
            }
        }
        return sideIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean renderAsNormalBlock() {
        return modelContainer == null && renderNode.getValue();
    }

    @Override
    public boolean isOpaqueCube() {
        return opaque;
    }

    //TODO Check this come 1.8
    @Override
    public void harvestBlock(World world, EntityPlayer player, int x, int y, int z, int metadata) {
        player.addStat(StatList.mineBlockStatArray[getIdFromBlock(this)], 1);
        final ItemStack held = player.getHeldItem();
        ToolsNode found = null;

        if (breakNode.isEnabled()) {
            for (ToolsNode toolsNode : breakNode.getValue()) {
                if (toolsNode instanceof ToolsNode.OffHand) {
                    if (held == null) {
                        found = toolsNode;
                        break;
                    }
                    continue;
                }
                if (held != null && toolsNode.getTool().minecraftObject == held.getItem()) {
                    found = toolsNode;
                    break;
                }
            }

            if (found == null) {
                found = breakNode.getToolByIdentifier("", "none");
                if (found == null) {
                    return;
                }
            }
        } else {
            return;
        }

        player.addExhaustion(found.getExhaustionRange().getValueWithinRange());
        final ArrayList<ItemStack> drops = Lists.newArrayList();
        for (DropProperty src : found.getValue().getValue()) {
            final GameObject source = src.getSource();
            final ItemStack toDrop;
            if (source.isBlock()) {
                toDrop = new ItemStack((Block) source.minecraftObject, src.getAmountProperty().getValueWithinRange(), src.getData());
            } else {
                toDrop = new ItemStack((Item) source.minecraftObject, src.getAmountProperty().getValueWithinRange(), src.getData());
            }
            if (src.getBonusProperty().getSource()) {
                final double chance = src.getBonusProperty().getValueWithinRange();
                if (RangeProperty.RANDOM.nextDouble() <= (chance / 100)) {
                    toDrop.stackSize += src.getBonusProperty().getValueWithinRange();
                }
            }
            drops.add(toDrop);
        }
        harvesters.set(player);
        if (!world.isRemote && !world.restoringBlockSnapshots) {
            final int fortune = EnchantmentHelper.getFortuneModifier(player);
            final float
                    modchance =
                    ForgeEventFactory.fireBlockHarvesting(drops, world, this, x, y, z, metadata, fortune, 1.0f, false, harvesters.get());
            for (ItemStack is : drops) {
                if (RangeProperty.RANDOM.nextFloat() <= modchance && world.getGameRules().getGameRuleBooleanValue("doTileDrops")) {
                    if (captureDrops.get()) {
                        capturedDrops.get().add(is);
                        return;
                    }
                    final float f = 0.7F;
                    final double d0 = (double) (world.rand.nextFloat() * f) + (double) (1.0F - f) * 0.5D;
                    final double d1 = (double) (world.rand.nextFloat() * f) + (double) (1.0F - f) * 0.5D;
                    final double d2 = (double) (world.rand.nextFloat() * f) + (double) (1.0F - f) * 0.5D;
                    final EntityItem item = new EntityItem(world, (double) x + d0, (double) y + d1, (double) z + d2, is);
                    item.delayBeforeCanPickup = 10;
                    world.spawnEntityInWorld(item);
                }
            }
        }
        harvesters.set(null);
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entity, ItemStack item) {
        if (rotationNode.isEnabled() && (rotationNode.isDefaultRotate() || rotationNode.isDefaultMirrorRotate())) {
            final ForgeDirection playerDir = EntityUtils.getEntityFacing(entity, false);

            if (rotationNode.isDefaultMirrorRotate()) {
                final ForgeDirection cameraDir = EntityUtils.getEntityFacing(entity, true);
                world.setBlockMetadataWithNotify(x, y, z, RotationMeta.Rotation.getState(cameraDir, playerDir).getId(), 3);
            } else {
                world.setBlockMetadataWithNotify(x, y, z, RotationMeta.Rotation.getState(ForgeDirection.NORTH, playerDir).getId(), 3);
            }
        }
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        final AxisAlignedBB vanillaBB = super.getCollisionBoundingBoxFromPool(world, x, y, z);
        if (!modelContainer.isPresent()) {
            return vanillaBB;
        }
        return modelContainer.get().getPhysics().getCollision(vanillaBB, world, x, y, z);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
        final AxisAlignedBB vanillaBB = super.getSelectedBoundingBoxFromPool(world, x, y, z);
        if (!modelContainer.isPresent()) {
            return vanillaBB;
        }
        return modelContainer.get().getPhysics().getWireframe(vanillaBB, world, x, y, z);
    }

    @Override
    public Pack getPack() {
        return pack;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public ClippedIcon[] getClipIcons(IBlockAccess access, int x, int y, int z, int metadata) {
        return clippedIcons;
    }

    @Override
    public ClippedIcon[] getClipIcons() {
        return clippedIcons;
    }

    @Override
    public Optional<PackModelContainer> getModelContainer(IBlockAccess access, int x, int y, int z, int metadata) {
        return modelContainer;
    }

    @Override
    public Optional<PackModelContainer> getModelContainer() {
        return modelContainer;
    }

    @Override
    public void setModelContainer(PackModelContainer modelContainer) {
        this.modelContainer = Optional.fromNullable(modelContainer);

        if (Configuration.IS_CLIENT && this.modelContainer.isPresent()) {
            if (this.modelContainer.get().getModel().isPresent()) {
                opaque = false;
            } else {
                opaque = renderNode.isOpaque();
            }
        }
    }

    @Override
    public boolean isSideSolid(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
        return true;
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public String getTextureName() {
        return textureName;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends INode<?>> T addNode(T node) {
        nodes.put((Class<? extends INode<?>>) node.getClass(), node);
        if (node.getClass() == BreakNode.class) {
            breakNode = (BreakNode) node;
        } else if (node.getClass() == CollisionNode.class) {
            collisionNode = (CollisionNode) node;
        }
        MinecraftForge.EVENT_BUS.post(new AddNodeEvent(this, node));
        return node;
    }

    @Override
    public void addNodes(INode<?>... nodes) {
        for (INode<?> node : nodes) {
            addNode(node);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends INode<?>> T getNode(Class<T> clazz) {
        return (T) nodes.get(clazz);
    }

    @Override
    public <T extends INode<?>> boolean hasNode(Class<T> clazz) {
        return getNode(clazz) != null;
    }

    @Override
    public String toString() {
        return "PackBlock {pack= " + pack.getName() + ", registry_name= " + pack.getName() + "\\" + identifier + "}";
    }

    @Override
    public List<String> getTooltip() {
        return tooltip;
    }



// ================================================== STAIRS LOGIC ===============================================================================

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess p_149719_1_, int p_149719_2_, int p_149719_3_, int p_149719_4_)
    {
        if (!stairsLogic) {
            super.setBlockBoundsBasedOnState(p_149719_1_, p_149719_2_, p_149719_3_, p_149719_4_);
            return;
        }

        if (this.field_150152_N)
        {
            this.setBlockBounds(0.5F * (float)(this.field_150153_O % 2), 0.5F * (float)(this.field_150153_O / 2 % 2), 0.5F * (float)(this.field_150153_O / 4 % 2), 0.5F + 0.5F * (float)(this.field_150153_O % 2), 0.5F + 0.5F * (float)(this.field_150153_O / 2 % 2), 0.5F + 0.5F * (float)(this.field_150153_O / 4 % 2));
        }
        else
        {
            this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @Override
    public void addCollisionBoxesToList(World p_149743_1_, int p_149743_2_, int p_149743_3_, int p_149743_4_, AxisAlignedBB p_149743_5_, List p_149743_6_, Entity p_149743_7_)
    {
        if (!stairsLogic) {
            super.addCollisionBoxesToList(p_149743_1_, p_149743_2_, p_149743_3_, p_149743_4_, p_149743_5_, p_149743_6_, p_149743_7_);
            return;
        }

        this.func_150147_e(p_149743_1_, p_149743_2_, p_149743_3_, p_149743_4_);
        super.addCollisionBoxesToList(p_149743_1_, p_149743_2_, p_149743_3_, p_149743_4_, p_149743_5_, p_149743_6_, p_149743_7_);
        boolean flag = this.func_150145_f(p_149743_1_, p_149743_2_, p_149743_3_, p_149743_4_);
        super.addCollisionBoxesToList(p_149743_1_, p_149743_2_, p_149743_3_, p_149743_4_, p_149743_5_, p_149743_6_, p_149743_7_);

        if (flag && this.func_150144_g(p_149743_1_, p_149743_2_, p_149743_3_, p_149743_4_))
        {
            super.addCollisionBoxesToList(p_149743_1_, p_149743_2_, p_149743_3_, p_149743_4_, p_149743_5_, p_149743_6_, p_149743_7_);
        }

        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
    }


    public void func_150147_e(IBlockAccess p_150147_1_, int p_150147_2_, int p_150147_3_, int p_150147_4_)
    {
        int l = p_150147_1_.getBlockMetadata(p_150147_2_, p_150147_3_, p_150147_4_);

        if ((l & 4) != 0)
        {
            this.setBlockBounds(0.0F, 0.5F, 0.0F, 1.0F, 1.0F, 1.0F);
        }
        else
        {
            this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.5F, 1.0F);
        }
    }


    public boolean func_150145_f(IBlockAccess p_150145_1_, int p_150145_2_, int p_150145_3_, int p_150145_4_)
    {
        int l = p_150145_1_.getBlockMetadata(p_150145_2_, p_150145_3_, p_150145_4_);
        int i1 = l & 3;
        float f = 0.5F;
        float f1 = 1.0F;

        if ((l & 4) != 0)
        {
            f = 0.0F;
            f1 = 0.5F;
        }

        float f2 = 0.0F;
        float f3 = 1.0F;
        float f4 = 0.0F;
        float f5 = 0.5F;
        boolean flag = true;
        Block block;
        int j1;
        int k1;

        if (i1 == 0)
        {
            f2 = 0.5F;
            f5 = 1.0F;
            block = p_150145_1_.getBlock(p_150145_2_ + 1, p_150145_3_, p_150145_4_);
            j1 = p_150145_1_.getBlockMetadata(p_150145_2_ + 1, p_150145_3_, p_150145_4_);

            if (func_150148_a(block) && (l & 4) == (j1 & 4))
            {
                k1 = j1 & 3;

                if (k1 == 3 && !this.func_150146_f(p_150145_1_, p_150145_2_, p_150145_3_, p_150145_4_ + 1, l))
                {
                    f5 = 0.5F;
                    flag = false;
                }
                else if (k1 == 2 && !this.func_150146_f(p_150145_1_, p_150145_2_, p_150145_3_, p_150145_4_ - 1, l))
                {
                    f4 = 0.5F;
                    flag = false;
                }
            }
        }
        else if (i1 == 1)
        {
            f3 = 0.5F;
            f5 = 1.0F;
            block = p_150145_1_.getBlock(p_150145_2_ - 1, p_150145_3_, p_150145_4_);
            j1 = p_150145_1_.getBlockMetadata(p_150145_2_ - 1, p_150145_3_, p_150145_4_);

            if (func_150148_a(block) && (l & 4) == (j1 & 4))
            {
                k1 = j1 & 3;

                if (k1 == 3 && !this.func_150146_f(p_150145_1_, p_150145_2_, p_150145_3_, p_150145_4_ + 1, l))
                {
                    f5 = 0.5F;
                    flag = false;
                }
                else if (k1 == 2 && !this.func_150146_f(p_150145_1_, p_150145_2_, p_150145_3_, p_150145_4_ - 1, l))
                {
                    f4 = 0.5F;
                    flag = false;
                }
            }
        }
        else if (i1 == 2)
        {
            f4 = 0.5F;
            f5 = 1.0F;
            block = p_150145_1_.getBlock(p_150145_2_, p_150145_3_, p_150145_4_ + 1);
            j1 = p_150145_1_.getBlockMetadata(p_150145_2_, p_150145_3_, p_150145_4_ + 1);

            if (func_150148_a(block) && (l & 4) == (j1 & 4))
            {
                k1 = j1 & 3;

                if (k1 == 1 && !this.func_150146_f(p_150145_1_, p_150145_2_ + 1, p_150145_3_, p_150145_4_, l))
                {
                    f3 = 0.5F;
                    flag = false;
                }
                else if (k1 == 0 && !this.func_150146_f(p_150145_1_, p_150145_2_ - 1, p_150145_3_, p_150145_4_, l))
                {
                    f2 = 0.5F;
                    flag = false;
                }
            }
        }
        else if (i1 == 3)
        {
            block = p_150145_1_.getBlock(p_150145_2_, p_150145_3_, p_150145_4_ - 1);
            j1 = p_150145_1_.getBlockMetadata(p_150145_2_, p_150145_3_, p_150145_4_ - 1);

            if (func_150148_a(block) && (l & 4) == (j1 & 4))
            {
                k1 = j1 & 3;

                if (k1 == 1 && !this.func_150146_f(p_150145_1_, p_150145_2_ + 1, p_150145_3_, p_150145_4_, l))
                {
                    f3 = 0.5F;
                    flag = false;
                }
                else if (k1 == 0 && !this.func_150146_f(p_150145_1_, p_150145_2_ - 1, p_150145_3_, p_150145_4_, l))
                {
                    f2 = 0.5F;
                    flag = false;
                }
            }
        }

        this.setBlockBounds(f2, f, f4, f3, f1, f5);
        return flag;
    }

    public boolean func_150144_g(IBlockAccess p_150144_1_, int p_150144_2_, int p_150144_3_, int p_150144_4_)
    {
        int l = p_150144_1_.getBlockMetadata(p_150144_2_, p_150144_3_, p_150144_4_);
        int i1 = l & 3;
        float f = 0.5F;
        float f1 = 1.0F;

        if ((l & 4) != 0)
        {
            f = 0.0F;
            f1 = 0.5F;
        }

        float f2 = 0.0F;
        float f3 = 0.5F;
        float f4 = 0.5F;
        float f5 = 1.0F;
        boolean flag = false;
        Block block;
        int j1;
        int k1;

        if (i1 == 0)
        {
            block = p_150144_1_.getBlock(p_150144_2_ - 1, p_150144_3_, p_150144_4_);
            j1 = p_150144_1_.getBlockMetadata(p_150144_2_ - 1, p_150144_3_, p_150144_4_);

            if (func_150148_a(block) && (l & 4) == (j1 & 4))
            {
                k1 = j1 & 3;

                if (k1 == 3 && !this.func_150146_f(p_150144_1_, p_150144_2_, p_150144_3_, p_150144_4_ - 1, l))
                {
                    f4 = 0.0F;
                    f5 = 0.5F;
                    flag = true;
                }
                else if (k1 == 2 && !this.func_150146_f(p_150144_1_, p_150144_2_, p_150144_3_, p_150144_4_ + 1, l))
                {
                    f4 = 0.5F;
                    f5 = 1.0F;
                    flag = true;
                }
            }
        }
        else if (i1 == 1)
        {
            block = p_150144_1_.getBlock(p_150144_2_ + 1, p_150144_3_, p_150144_4_);
            j1 = p_150144_1_.getBlockMetadata(p_150144_2_ + 1, p_150144_3_, p_150144_4_);

            if (func_150148_a(block) && (l & 4) == (j1 & 4))
            {
                f2 = 0.5F;
                f3 = 1.0F;
                k1 = j1 & 3;

                if (k1 == 3 && !this.func_150146_f(p_150144_1_, p_150144_2_, p_150144_3_, p_150144_4_ - 1, l))
                {
                    f4 = 0.0F;
                    f5 = 0.5F;
                    flag = true;
                }
                else if (k1 == 2 && !this.func_150146_f(p_150144_1_, p_150144_2_, p_150144_3_, p_150144_4_ + 1, l))
                {
                    f4 = 0.5F;
                    f5 = 1.0F;
                    flag = true;
                }
            }
        }
        else if (i1 == 2)
        {
            block = p_150144_1_.getBlock(p_150144_2_, p_150144_3_, p_150144_4_ - 1);
            j1 = p_150144_1_.getBlockMetadata(p_150144_2_, p_150144_3_, p_150144_4_ - 1);

            if (func_150148_a(block) && (l & 4) == (j1 & 4))
            {
                f4 = 0.0F;
                f5 = 0.5F;
                k1 = j1 & 3;

                if (k1 == 1 && !this.func_150146_f(p_150144_1_, p_150144_2_ - 1, p_150144_3_, p_150144_4_, l))
                {
                    flag = true;
                }
                else if (k1 == 0 && !this.func_150146_f(p_150144_1_, p_150144_2_ + 1, p_150144_3_, p_150144_4_, l))
                {
                    f2 = 0.5F;
                    f3 = 1.0F;
                    flag = true;
                }
            }
        }
        else if (i1 == 3)
        {
            block = p_150144_1_.getBlock(p_150144_2_, p_150144_3_, p_150144_4_ + 1);
            j1 = p_150144_1_.getBlockMetadata(p_150144_2_, p_150144_3_, p_150144_4_ + 1);

            if (func_150148_a(block) && (l & 4) == (j1 & 4))
            {
                k1 = j1 & 3;

                if (k1 == 1 && !this.func_150146_f(p_150144_1_, p_150144_2_ - 1, p_150144_3_, p_150144_4_, l))
                {
                    flag = true;
                }
                else if (k1 == 0 && !this.func_150146_f(p_150144_1_, p_150144_2_ + 1, p_150144_3_, p_150144_4_, l))
                {
                    f2 = 0.5F;
                    f3 = 1.0F;
                    flag = true;
                }
            }
        }

        if (flag)
        {
            this.setBlockBounds(f2, f, f4, f3, f1, f5);
        }

        return flag;
    }

    private boolean func_150146_f(IBlockAccess p_150146_1_, int p_150146_2_, int p_150146_3_, int p_150146_4_, int p_150146_5_)
    {
        Block block = p_150146_1_.getBlock(p_150146_2_, p_150146_3_, p_150146_4_);
        return func_150148_a(block) && p_150146_1_.getBlockMetadata(p_150146_2_, p_150146_3_, p_150146_4_) == p_150146_5_;
    }

    @Override
    public MovingObjectPosition collisionRayTrace(World p_149731_1_, int p_149731_2_, int p_149731_3_, int p_149731_4_, Vec3 p_149731_5_, Vec3 p_149731_6_)
    {
        if (!stairsLogic) {
            return super.collisionRayTrace(p_149731_1_, p_149731_2_, p_149731_3_, p_149731_4_, p_149731_5_, p_149731_6_);
        }

        MovingObjectPosition[] amovingobjectposition = new MovingObjectPosition[8];
        int l = p_149731_1_.getBlockMetadata(p_149731_2_, p_149731_3_, p_149731_4_);
        int i1 = l & 3;
        boolean flag = (l & 4) == 4;
        int[] aint = field_150150_a[i1 + (flag?4:0)];
        this.field_150152_N = true;
        int k1;
        int l1;
        int i2;

        for (int j1 = 0; j1 < 8; ++j1)
        {
            this.field_150153_O = j1;
            int[] aint1 = aint;
            k1 = aint.length;

            for (l1 = 0; l1 < k1; ++l1)
            {
                i2 = aint1[l1];

                if (i2 == j1)
                {
                    ;
                }
            }

            amovingobjectposition[j1] = super.collisionRayTrace(p_149731_1_, p_149731_2_, p_149731_3_, p_149731_4_, p_149731_5_, p_149731_6_);
        }

        int[] aint2 = aint;
        int k2 = aint.length;

        for (k1 = 0; k1 < k2; ++k1)
        {
            l1 = aint2[k1];
            amovingobjectposition[l1] = null;
        }

        MovingObjectPosition movingobjectposition1 = null;
        double d1 = 0.0D;
        MovingObjectPosition[] amovingobjectposition1 = amovingobjectposition;
        i2 = amovingobjectposition.length;

        for (int j2 = 0; j2 < i2; ++j2)
        {
            MovingObjectPosition movingobjectposition = amovingobjectposition1[j2];

            if (movingobjectposition != null)
            {
                double d0 = movingobjectposition.hitVec.squareDistanceTo(p_149731_6_);

                if (d0 > d1)
                {
                    movingobjectposition1 = movingobjectposition;
                    d1 = d0;
                }
            }
        }

        return movingobjectposition1;
    }

    public static boolean func_150148_a(Block p_150148_0_)
    {
        return p_150148_0_ instanceof BlockStairs || (p_150148_0_ instanceof PackBlock && ((PackBlock) p_150148_0_).stairsLogic);
    }
}
