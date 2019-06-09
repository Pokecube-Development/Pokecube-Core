package pokecube.core.world.gen.village.buildings.pokecenter;

import java.util.List;
import java.util.Random;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureVillagePieces.PieceWeight;
import net.minecraftforge.fml.common.registry.VillagerRegistry.IVillageCreationHandler;
import pokecube.core.world.gen.village.buildings.TemplateStructureBase;

public class PokeCentreCreationHandler implements IVillageCreationHandler
{

    @Override
    public Village buildComponent(PieceWeight villagePiece, Start startPiece, List<StructureComponent> pieces,
            Random random, int minX, int minY, int minZ, Direction facing, int componentType)
    {
        BlockPos pos = new BlockPos(minX, minY, minZ);
        TemplateStructureBase component = new TemplatePokecenter(pos, facing);
        StructureBoundingBox structureboundingbox = component.getBoundingBox();
        structureboundingbox.maxX += 2 * facing.getXOffset();
        structureboundingbox.maxZ += 2 * facing.getZOffset();
        structureboundingbox.minX += 2 * facing.getXOffset();
        structureboundingbox.minZ += 2 * facing.getZOffset();
        StructureComponent conf = StructureComponent.findIntersecting(pieces, structureboundingbox);
        boolean conflict = conf == null;
        return conflict ? component : null;
    }

    @Override
    public Class<?> getComponentClass()
    {
        return TemplatePokecenter.class;
    }

    @Override
    public PieceWeight getVillagePieceWeight(Random random, int i)
    {
        return new PieceWeight(TemplatePokecenter.class, 100, 1);
    }

    protected static boolean canVillageGoDeeper(StructureBoundingBox par0StructureBoundingBox)
    {
        return (par0StructureBoundingBox != null) && (par0StructureBoundingBox.minY > 10);
    }

}
