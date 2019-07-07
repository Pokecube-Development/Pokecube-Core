package pokecube.core.interfaces.pokemob.commandhandlers;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.SharedMonsterAttributes;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.network.pokemobs.PacketCommand.DefaultHandler;
import thut.api.maths.Vector3;

public class MoveToHandler extends DefaultHandler
{
    Vector3 location;
    float   speed;

    public MoveToHandler()
    {
    }

    public MoveToHandler(Vector3 location, Float speed)
    {
        this.location = location.copy();
        this.speed = Math.abs(speed);
    }

    @Override
    public void handleCommand(IPokemob pokemob) throws Exception
    {
        this.speed = (float) Math.min(this.speed, pokemob.getEntity().getAttribute(
                SharedMonsterAttributes.MOVEMENT_SPEED).getValue());
        pokemob.getEntity().getNavigator().setPath(pokemob.getEntity().getNavigator().getPathToXYZ(this.location.x,
                this.location.y, this.location.z), this.speed);
    }

    @Override
    public void readFromBuf(ByteBuf buf)
    {
        super.readFromBuf(buf);
        this.location = Vector3.readFromBuff(buf);
        this.speed = buf.readFloat();
    }

    @Override
    public void writeToBuf(ByteBuf buf)
    {
        super.writeToBuf(buf);
        this.location.writeToBuff(buf);
        buf.writeFloat(this.speed);
    }
}
