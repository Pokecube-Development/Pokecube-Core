package pokecube.core.world.gen.village.buildings;

import java.util.Locale;
import java.util.Map;
import java.util.Random;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import pokecube.core.events.handlers.SpawnHandler;
import pokecube.core.world.gen.template.PokecubeTemplate;
import pokecube.core.world.gen.template.PokecubeTemplates;

public abstract class TemplateStructureBase extends Village
{
    public Template          template;
    public PlacementSettings placeSettings;
    public BlockPos          templatePosition;
    public int               averageGroundLevel = -1;

    public static Rotation getFromDir(Direction dir)
    {
        Rotation rotation = Rotation.CLOCKWISE_180;
        if (dir == Direction.NORTH) rotation = Rotation.NONE;
        if (dir == Direction.EAST) rotation = Rotation.CLOCKWISE_90;
        if (dir == Direction.WEST) rotation = Rotation.COUNTERCLOCKWISE_90;
        return rotation;
    }

    public TemplateStructureBase()
    {
        placeSettings = new PlacementSettings().setIgnoreEntities(false).setReplacedBlock(null);
    }

    public TemplateStructureBase(String type, BlockPos pos, Direction dir)
    {
        this();

        Rotation rotation;
        Mirror mirror;
        if (dir == null)
        {
            rotation = Rotation.NONE;
            mirror = Mirror.NONE;
        }
        else
        {
            switch (dir)
            {
            case SOUTH:
                mirror = Mirror.LEFT_RIGHT;
                rotation = Rotation.NONE;
                break;
            case WEST:
                mirror = Mirror.LEFT_RIGHT;
                rotation = Rotation.CLOCKWISE_90;
                break;
            case EAST:
                mirror = Mirror.NONE;
                rotation = Rotation.CLOCKWISE_90;
                break;
            default:
                mirror = Mirror.NONE;
                rotation = Rotation.NONE;
            }
        }
        placeSettings.setRotation(rotation);
        placeSettings.setMirror(mirror);
        Template template = PokecubeTemplates.getTemplate(type);
        if (template == null) return;
        setup(template, pos, placeSettings, dir);
    }

    protected void setup(Template template, BlockPos pos, PlacementSettings settings, Direction dir)
    {
        this.template = template;
        this.templatePosition = pos;
        this.placeSettings = settings;
        this.setBoundingBoxFromTemplate(dir);
    }

    /** (abstract) Helper method to write subclass data to NBT */
    @Override
    protected void writeStructureToNBT(CompoundNBT tagCompound)
    {
        if (templatePosition == null)
        {
            if (boundingBox == null) templatePosition = new BlockPos(0, 0, 0);
            else templatePosition = new BlockPos(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
        }

        tagCompound.putInt("TPX", this.templatePosition.getX());
        tagCompound.putInt("TPY", this.templatePosition.getY());
        tagCompound.putInt("TPZ", this.templatePosition.getZ());
        tagCompound.putString("TN", ((PokecubeTemplate) getTemplate()).name);
        tagCompound.setByte("Rot", (byte) placeSettings.getRotation().ordinal());
    }

    /** (abstract) Helper method to read subclass data from NBT */
    protected void readStructureFromNBT(CompoundNBT tagCompound)
    {
        this.templatePosition = new BlockPos(tagCompound.getInt("TPX"), tagCompound.getInt("TPY"),
                tagCompound.getInt("TPZ"));
        String name = tagCompound.getString("TN");
        if (name.isEmpty()) name = PokecubeTemplates.POKECENTER;
        try
        {
            placeSettings.setRotation(Rotation.values()[tagCompound.getByte("Rot")]);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        template = PokecubeTemplates.getTemplate(name);
    }

    private void offsetBox(StructureBoundingBox boxIn)
    {
        Rotation rotation = this.placeSettings.getRotation();
        Mirror mirror = this.placeSettings.getMirror();
        BlockPos blockpos = this.template.transformedSize(rotation);
        int dx = 0;
        int dz = 0;
        int dpx = 0, dpz = 0;
        switch (rotation)
        {
        case NONE:
            switch (mirror)
            {
            case LEFT_RIGHT:
                dx = -1;
                dz = -1;
                dpz = blockpos.getZ() - 2;
                dpx = -1;
                break;
            case NONE:
                dx = -1;
                dz = 1;
                dpx = -1;
                dpz = 1;
                break;
            default:
                break;
            }
            break;
        case CLOCKWISE_90:
            switch (mirror)
            {
            case NONE:
                dx = -1;
                dz = -1;
                dpz = -1;
                dpx = blockpos.getX() - 2;
                break;
            case LEFT_RIGHT:
                dz = -1;
                dx = 1;
                dpx = 1;
                dpz = -1;
                break;
            default:
                break;
            }
            break;
        default:
            break;
        }
        templatePosition = templatePosition.add(dpx, 0, dpz);
        boxIn.offset(dx, 0, dz);
    }

    /** second Part of Structure generating, this for example places Spiderwebs,
     * Mob Spawners, it closes Mineshafts at the end, it adds Fences... */
    @Override
    public boolean addComponentParts(World worldIn, Random randomIn, StructureBoundingBox boxIn)
    {
        if (template == null) return false;
        /** This means templates cannot be spawned in worlds which are not valid
         * for pokemob stuff to occur in. */
        if (!SpawnHandler.canSpawnInWorld(worldIn)) return false;
        try
        {
            if (this.averageGroundLevel < 0)
            {
                Map<BlockPos, String> map = this.template.getDataBlocks(new BlockPos(0, 0, 0), this.placeSettings);
                for (BlockPos blockpos : map.keySet())
                {
                    String s = map.get(blockpos);
                    if (s.toLowerCase(Locale.ENGLISH).startsWith("floor"))
                    {
                        setOffset(-blockpos.getY());
                        blockpos = blockpos.add(templatePosition);
                        if (boxIn.isVecInside(blockpos))
                        {
                            averageGroundLevel = Math.max(worldIn.getTopSolidOrLiquidBlock(blockpos).getY(),
                                    worldIn.dimension.getAverageGroundLevel() - 1);
                        }
                        break;
                    }
                }
                if (this.averageGroundLevel < 0) this.averageGroundLevel = this.getAverageGroundLevel(worldIn, boxIn);
                if (this.averageGroundLevel < 0) { return true; }
            }
            StructureBoundingBox buildBox = new StructureBoundingBox(boundingBox);
            buildBox.offset(0, this.averageGroundLevel - buildBox.minY + getOffset(), 0);
            this.templatePosition = new BlockPos(templatePosition.getX(), buildBox.minY, templatePosition.getZ());
            this.placeSettings.setIgnoreEntities(worldIn.isRemote).setBoundingBox(boxIn);
            this.template.addBlocksToWorld(worldIn, this.templatePosition, this.placeSettings);
            Map<BlockPos, String> map = this.template.getDataBlocks(this.templatePosition, this.placeSettings);
            for (BlockPos blockpos : map.keySet())
            {
                String s = map.get(blockpos);
                this.handleDataMarker(s, blockpos, worldIn, randomIn, buildBox);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return true;
    }

    public abstract Template getTemplate();

    public abstract int getOffset();

    public abstract void setOffset(int offset);

    protected abstract void handleDataMarker(String marker, BlockPos pos, World world, Random rand,
            StructureBoundingBox box);

    public void setBoundingBoxFromTemplate(Direction dir)
    {
        BlockPos size = template.getSize();
        boundingBox = StructureBoundingBox.getComponentToAddBoundingBox(templatePosition.getX(),
                templatePosition.getY(), templatePosition.getZ(), 0, 0, 0, size.getX(), size.getY(), size.getZ(), dir);
        templatePosition = new BlockPos(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
        offsetBox(boundingBox);
    }

    @Override
    public void offset(int x, int y, int z)
    {
        super.offset(x, y, z);
        this.templatePosition = this.templatePosition.add(x, y, z);
    }

}
