package pokecube.core.world.gen.template;

import java.util.Iterator;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.Template.EntityInfo;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.template.ITemplateProcessor;
import net.minecraftforge.common.MinecraftForge;
import pokecube.core.events.StructureEvent;

public class PokecubeTemplate extends Template
{
    public final String name;
    public String       expectedBiomeType = null;

    public PokecubeTemplate(String name)
    {
        this.name = name;
    }

    public PokecubeTemplate(Template from, String name)
    {
        this.read(from.writeToNBT(new CompoundNBT()));
        this.name = name;
    }

    /** This takes the data stored in this instance and puts them into the
     * world. */
    @Override
    public void addBlocksToWorld(World worldIn, BlockPos pos, PlacementSettings placementIn)
    {
        ITemplateProcessor processor = new TemplateProcessor(worldIn, pos, placementIn);
        this.addBlocksToWorld(worldIn, pos, processor, placementIn, 2);
        StructureEvent.BuildStructure event = new StructureEvent.BuildStructure(pos, worldIn, name, getSize(),
                placementIn);
        event.seBiomeType(expectedBiomeType);
        MinecraftForge.EVENT_BUS.post(event);
    }

    @Override
    public void addEntitiesToWorld(World worldIn, BlockPos pos, Mirror mirrorIn, Rotation rotationIn,
            @Nullable StructureBoundingBox aabb)
    {
        Iterator<EntityInfo> entities = this.entities.iterator();
        while (entities.hasNext())
        {
            EntityInfo template$entityinfo = entities.next();
            BlockPos blockpos = transformedBlockPos(template$entityinfo.blockPos, mirrorIn, rotationIn).add(pos);

            if (aabb == null || aabb.isVecInside(blockpos))
            {
                CompoundNBT CompoundNBT = template$entityinfo.entityData;
                Vec3d vec3d = transformedVec3d(template$entityinfo.pos, mirrorIn, rotationIn);
                Vec3d vec3d1 = vec3d.add(pos.getX(), pos.getY(), pos.getZ());
                ListNBT ListNBT = new ListNBT();
                ListNBT.appendTag(new NBTTagDouble(vec3d1.x));
                ListNBT.appendTag(new NBTTagDouble(vec3d1.y));
                ListNBT.appendTag(new NBTTagDouble(vec3d1.z));
                CompoundNBT.put("Pos", ListNBT);
                CompoundNBT.setUniqueId("UUID", UUID.randomUUID());
                Entity entity;

                try
                {
                    entity = EntityList.createEntityFromNBT(CompoundNBT, worldIn);
                }
                catch (Exception var15)
                {
                    entity = null;
                }

                if (entity != null)
                {
                    float f = 0;
                    switch (rotationIn)
                    {
                    case NONE:
                        switch (mirrorIn)
                        {
                        case LEFT_RIGHT:
                            f = 180;
                            break;
                        case NONE:
                            f = 0;
                            break;
                        default:
                            f = entity.getMirroredYaw(mirrorIn);
                            f -= entity.getRotatedYaw(rotationIn);
                            break;
                        }
                        break;
                    case CLOCKWISE_90:
                        switch (mirrorIn)
                        {
                        case NONE:
                            f = 90;
                            break;
                        case LEFT_RIGHT:
                            f = 270;
                            break;
                        default:
                            f = entity.getMirroredYaw(mirrorIn);
                            f -= entity.getRotatedYaw(rotationIn);
                            break;
                        }
                        break;
                    default:
                        f = entity.getMirroredYaw(mirrorIn);
                        f -= entity.getRotatedYaw(rotationIn);
                        break;
                    }
                    entity.setLocationAndAngles(vec3d1.x, vec3d1.y, vec3d1.z, f, entity.rotationPitch);
                    StructureEvent.SpawnEntity event = new StructureEvent.SpawnEntity(entity, name);
                    MinecraftForge.EVENT_BUS.post(event);
                    entities.remove();
                    if (event.getToSpawn() != null && !event.isCanceled()) worldIn.spawnEntity(event.getToSpawn());
                }
            }
        }
    }

}
