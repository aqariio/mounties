package aqario.mounties.mixin;

import aqario.mounties.common.config.MountiesConfig;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@SuppressWarnings("deprecation")
@Mixin(LeavesBlock.class)
public abstract class LeavesBlockMixin extends Block implements Waterloggable {
    public LeavesBlockMixin(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (!MountiesConfig.removeLeavesCollision) {
            return super.getCollisionShape(state, world, pos, context);
        }
        if (context instanceof EntityShapeContext entityContext) {
            Entity entity = entityContext.getEntity();
            if (context.isAbove(VoxelShapes.fullCube(), pos, true) && !(world.getBlockState(pos.up()).getBlock() instanceof LeavesBlock) && entity != null && entity.fallDistance < 2.5F) {
                return VoxelShapes.fullCube();
            }
        }
        return VoxelShapes.empty();
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!MountiesConfig.removeLeavesCollision) {
            return;
        }
        if (entity instanceof PlayerEntity player && player.getAbilities().flying) {
            return;
        }
        if (entity instanceof LivingEntity && entity.getType() != EntityType.FOX && entity.getType() != EntityType.BEE) {
            entity.setVelocity(entity.getVelocity().multiply(new Vec3d(0.9, 0.9, 0.9)));
        }
    }
}
