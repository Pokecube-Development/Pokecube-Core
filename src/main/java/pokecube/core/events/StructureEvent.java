package pokecube.core.events;

import net.minecraft.entity.Entity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.eventbus.api.Cancelable;

public class StructureEvent extends Event
{
    public static class BuildStructure extends StructureEvent
    {
        private final StructureBoundingBox bounds;
        private final PlacementSettings    settings;
        private final String               structure;
        private String                     structureOverride;
        private final World                world;

        public BuildStructure(BlockPos pos, World world, String name, BlockPos size, PlacementSettings settings)
        {
            this.structure = name;
            this.world = world;
            this.settings = settings;
            Direction dir = Direction.SOUTH;
            if (settings.getMirror() != null) dir = settings.getMirror().mirror(dir);
            if (settings.getRotation() != null) dir = settings.getRotation().rotate(dir);
            this.bounds = StructureBoundingBox.getComponentToAddBoundingBox(pos.getX(), pos.getY(), pos.getZ(), 0, 0, 0,
                    size.getX(), size.getY(), size.getZ(), dir);
        }

        public String getStructure()
        {
            return structure;
        }

        public World getWorld()
        {
            return world;
        }

        public PlacementSettings getSettings()
        {
            return settings;
        }

        public StructureBoundingBox getBoundingBox()
        {
            return bounds;
        }

        public String getBiomeType()
        {
            return structureOverride;
        }

        public void seBiomeType(String structureOverride)
        {
            this.structureOverride = structureOverride;
        }
    }

    @Cancelable
    public static class SpawnEntity extends StructureEvent
    {
        private Entity       toSpawn;
        private final String structure;
        private final Entity original;

        public SpawnEntity(Entity entity, String structure)
        {
            this.structure = structure;
            this.original = entity;
            this.toSpawn = original;
        }

        public void setToSpawn(Entity entity)
        {
            this.toSpawn = entity;
        }

        public Entity getEntity()
        {
            return original;
        }

        public Entity getToSpawn()
        {
            return toSpawn;
        }

        public String getStructureName()
        {
            return structure;
        }
    }
}
