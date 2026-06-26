package aqario.equine.mixin;

import aqario.equine.common.config.EquineConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LeavesBlock.class)
public abstract class LeavesBlockMixin extends Block implements SimpleWaterloggedBlock {
    public LeavesBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!EquineConfig.removeLeavesCollision) {
            return super.getCollisionShape(state, level, pos, context);
        }
        if (context instanceof EntityCollisionContext entityContext) {
            Entity entity = entityContext.getEntity();
            if (!context.isAbove(Shapes.block(), pos, true)
                || level.getBlockState(pos.above()).getBlock() instanceof LeavesBlock
                || (entity != null && entity.fallDistance > 2.5F)
            ) {
                return Shapes.empty();
            }
        }
        return Shapes.block();
    }

    @Override
    public void entityInside(
        BlockState blockState,
        Level level,
        BlockPos pos,
        Entity entity,
        InsideBlockEffectApplier insideBlockEffectApplier,
        boolean bl
    ) {
        if (!EquineConfig.removeLeavesCollision) {
            return;
        }
        if (entity instanceof Player player && player.getAbilities().flying) {
            return;
        }
        if (entity instanceof LivingEntity && entity.getType() != EntityTypes.FOX && entity.getType() != EntityTypes.BEE) {
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(new Vec3(0.9, 0.9, 0.9)));
        }
    }
}
