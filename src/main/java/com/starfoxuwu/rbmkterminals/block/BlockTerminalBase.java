package com.starfoxuwu.rbmkterminals.block;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.starfoxuwu.rbmkterminals.RBMKTerminals;
import com.starfoxuwu.rbmkterminals.network.ModNetwork;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A wall mounted screen, modelled after the terminal HBM's Nuclear Tech Mod bolts onto its RBMK consoles.
 * <p>
 * The block itself is a slim plate that sits flat against the wall it was placed on, the screen is painted onto the
 * outer face of that plate by {@code RenderTerminal}. Metadata is the side the screen looks at.
 */
public abstract class BlockTerminalBase extends BlockContainer {

    /** Thickness of the plate, measured from the wall. */
    public static final float THICKNESS = 2F / 16F;

    /** The plate is a little smaller than the block it lives in, so that every device keeps its own bezel. */
    public static final float MARGIN = 1F / 16F;

    @SideOnly(Side.CLIENT)
    protected IIcon icon;

    protected BlockTerminalBase() {
        super(Material.iron);
        setHardness(2.5F);
        setResistance(15F);
        setStepSound(soundTypeMetal);
        // fallback page only: with HBM installed ModBlocks moves both terminals into its machine tab, see HbmCompat
        setCreativeTab(CreativeTabs.tabRedstone);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    /** @return the direction the screen faces, derived from the block metadata */
    public static ForgeDirection getFacing(int metadata) {
        ForgeDirection facing = ForgeDirection.getOrientation(metadata);
        if (facing == ForgeDirection.UP || facing == ForgeDirection.DOWN || facing == ForgeDirection.UNKNOWN) {
            return ForgeDirection.SOUTH;
        }
        return facing;
    }

    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int meta) {
        // the panel hangs on the face that was clicked, its screen looks away from the wall
        ForgeDirection facing = ForgeDirection.getOrientation(side);
        return facing == ForgeDirection.UP || facing == ForgeDirection.DOWN ? ForgeDirection.SOUTH.ordinal() : side;
    }

    @Override
    public void setBlockBoundsForItemRender() {
        // the bounds are shared by every state of the block, so the item form needs its own plate shape
        setBlockBounds(0F, 0F, 0F, 1F, 1F, THICKNESS);
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        ForgeDirection facing = getFacing(world.getBlockMetadata(x, y, z));

        float minX = MARGIN, minY = MARGIN, minZ = MARGIN;
        float maxX = 1F - MARGIN, maxY = 1F - MARGIN, maxZ = 1F - MARGIN;

        // the plate rests on the wall, so it sits on the side the screen does not look at
        if (facing == ForgeDirection.EAST) {
            minX = 0F;
            maxX = THICKNESS;
        } else if (facing == ForgeDirection.WEST) {
            minX = 1F - THICKNESS;
            maxX = 1F;
        } else if (facing == ForgeDirection.SOUTH) {
            minZ = 0F;
            maxZ = THICKNESS;
        } else {
            minZ = 1F - THICKNESS;
            maxZ = 1F;
        }

        setBlockBounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List<AxisAlignedBB> list,
        Entity entity) {
        setBlockBoundsBasedOnState(world, x, y, z);
        super.addCollisionBoxesToList(world, x, y, z, mask, list, entity);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        setBlockBoundsBasedOnState(world, x, y, z);
        return super.getCollisionBoundingBoxFromPool(world, x, y, z);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
        setBlockBoundsBasedOnState(world, x, y, z);
        return super.getSelectedBoundingBoxFromPool(world, x, y, z);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (world.isRemote) return;

        int meta = world.getBlockMetadata(x, y, z);
        if (!isSupported(world, x, y, z, getFacing(meta))) {
            dropBlockAsItem(world, x, y, z, meta, 0);
            world.setBlockToAir(x, y, z);
        }
    }

    /** @return true if the block behind the panel can still carry it */
    protected boolean isSupported(World world, int x, int y, int z, ForgeDirection facing) {
        ForgeDirection wall = facing.getOpposite();
        int wallX = x + wall.offsetX, wallY = y + wall.offsetY, wallZ = z + wall.offsetZ;
        return world.getBlock(wallX, wallY, wallZ)
            .isSideSolid(world, wallX, wallY, wallZ, facing);
    }

    /** @return the lang keys of the lines shown in the item tooltip */
    public abstract String[] getTooltipKeys();

    /**
     * What a plain right click does. The display terminal has nothing to offer here, the input terminal opens its
     * keyboard, and the screwdriver aware versions in {@code compat.hbm} call their configuration window instead.
     */
    protected boolean onActivated(World world, EntityPlayer player, int x, int y, int z, int side, float hitX,
        float hitY, float hitZ) {
        return false;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        return onActivated(world, player, x, y, z, side, hitX, hitY, hitZ);
    }

    /**
     * Opens the screwdriver's configuration window, which is the client side counterpart of HBM's
     * {@code FMLNetworkHandler.openGui} call in {@code RBMKIndicator#onScrew}. A terminal is not an inventory, so
     * there is nothing for the server to open; it only ever sees the tag the window sends back.
     */
    protected void openTerminalConfig(World world, EntityPlayer player, int x, int y, int z) {
        if (world.isRemote) player.openGui(RBMKTerminals.instance, ModNetwork.GUI_TERMINAL_CONFIG, world, x, y, z);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        icon = register.registerIcon(RBMKTerminals.MODID + ":terminal_panel");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return icon;
    }
}
