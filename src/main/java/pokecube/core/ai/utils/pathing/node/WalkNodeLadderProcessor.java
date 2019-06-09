package pokecube.core.ai.utils.pathing.node;

import net.minecraft.block.BlockState;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

public class WalkNodeLadderProcessor extends WalkNodeProcessor
{

    @Override
    public int findPathOptions(PathPoint[] pathOptions, PathPoint currentPoint, PathPoint targetPoint,
            float maxDistance)
    {
        int i = super.findPathOptions(pathOptions, currentPoint, targetPoint, maxDistance);
        PathPoint pathpoint = this.getPoint(currentPoint.x, currentPoint.y + 1, currentPoint.z, Direction.UP);
        if (pathpoint != null && !pathpoint.visited && pathpoint.distanceTo(targetPoint) < maxDistance)
        {
            pathOptions[i++] = pathpoint;
        }
        pathpoint = this.getPoint(currentPoint.x, currentPoint.y - 1, currentPoint.z, Direction.DOWN);
        if (pathpoint != null && !pathpoint.visited && pathpoint.distanceTo(targetPoint) < maxDistance)
        {
            pathOptions[i++] = pathpoint;
        }
        for (Direction side : Direction.HORIZONTALS)
        {
            pathpoint = this.getPoint(currentPoint.x + side.getXOffset(), currentPoint.y - 1,
                    currentPoint.z + side.getZOffset(), Direction.DOWN);
            if (pathpoint != null && !pathpoint.visited && pathpoint.distanceTo(targetPoint) < maxDistance)
            {
                pathOptions[i++] = pathpoint;
            }
        }
        return i;
    }

    private PathPoint getPoint(int x, int y, int z, Direction direction)
    {
        if (direction == Direction.UP) return getLadder(x, y, z);
        else if (direction == Direction.DOWN) return getJumpOff(x, y, z);
        return null;
    }

    private PathPoint getJumpOff(int x, int y, int z)
    {
        for (int i = x; i < x + this.entitySizeX; ++i)
        {
            for (int j = y; j < y + this.entitySizeY; ++j)
            {
                for (int k = z; k < z + this.entitySizeZ; ++k)
                {
                    PathNodeType type = getPathNodeTypeRaw(blockaccess, i, j, k);
                    if (type != PathNodeType.OPEN && type != PathNodeType.WALKABLE) { return null; }
                }
            }
        }
        PathPoint point = openPoint(x, y, z);

        boolean laddar = false;
        for (Direction dir : Direction.HORIZONTALS)
        {
            laddar = laddar || getLadder(x + dir.getXOffset(), y, z + dir.getZOffset()) != null;
        }
        point.nodeType = PathNodeType.OPEN;
        point.costMalus += laddar ? 1 : 5;
        return point;
    }

    private PathPoint getLadder(int x, int y, int z)
    {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = x; i < x + this.entitySizeX; ++i)
        {
            for (int j = y; j < y + this.entitySizeY; ++j)
            {
                for (int k = z; k < z + this.entitySizeZ; ++k)
                {
                    BlockState BlockState = this.blockaccess.getBlockState(pos.setPos(i, j, k));

                    if (BlockState.getBlock().isLadder(BlockState, blockaccess, pos, entity))
                    {
                        PathPoint point = openPoint(x, y, z);
                        point.nodeType = PathNodeType.OPEN;
                        return point;
                    }
                }
            }
        }
        return null;
    }
}
