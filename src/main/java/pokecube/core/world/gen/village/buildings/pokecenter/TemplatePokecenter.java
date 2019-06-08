package pokecube.core.world.gen.village.buildings.pokecenter;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import pokecube.core.world.gen.template.PokecubeTemplates;
import pokecube.core.world.gen.village.buildings.TemplateStructure;

public class TemplatePokecenter extends TemplateStructure
{
    public TemplatePokecenter()
    {
        super();
        setOffset(-2);
    }

    public TemplatePokecenter(BlockPos pos, Direction dir)
    {
        super(PokecubeTemplates.POKECENTER, pos, dir);
    }

    @Override
    public Template getTemplate()
    {
        if (template != null) return template;
        return template = PokecubeTemplates.getTemplate(PokecubeTemplates.POKECENTER);
    }
}
