package aqario.mounties.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    protected LivingEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Unique
    public boolean shouldStepDown() {
        BlockPos pos = this.getBlockPos();
        return !this.isOnGround()
            && this.fallDistance > 0f
            && this.fallDistance < 0.3f
            && !this.getWorld().getBlockState(pos.down()).getCollisionShape(this.getWorld(), pos.down()).isEmpty();
//            || !this.getWorld().getBlockState(pos.down(2)).getCollisionShape(this.getWorld(), pos.down(2)).isEmpty();
    }

    @Inject(method = "travelControlled", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;travel(Lnet/minecraft/util/math/Vec3d;)V"))
    private void mounties$stepDownwards(PlayerEntity player, Vec3d input, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof HorseBaseEntity && this.getPrimaryPassenger() instanceof PlayerEntity && this.shouldStepDown()) {
            this.addVelocity(new Vec3d(0, -1, 0));
        }
    }
}
