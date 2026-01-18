package aqario.mounties.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    protected LivingEntityMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Unique
    public boolean shouldStepDown() {
        return !this.onGround()
            && this.fallDistance > 0f
            && this.fallDistance < 0.3f
            && this.level().getBlockStates(this.getBoundingBox().move(0, -1, 0))
            .anyMatch(BlockBehaviour.BlockStateBase::isSolidRender);
    }

    @Inject(method = "travelRidden", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;travel(Lnet/minecraft/world/phys/Vec3;)V"))
    private void mounties$stepDownwards(Player player, Vec3 input, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof AbstractHorse
            && this.getControllingPassenger() instanceof Player
            && this.shouldStepDown()
        ) {
            this.addDeltaMovement(new Vec3(0, -1, 0));
        }
    }

    @Inject(method = "getFlyingSpeed", at = @At("RETURN"), cancellable = true)
    private void mounties$increaseAirSpeed(CallbackInfoReturnable<Float> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof AbstractHorse && this.getControllingPassenger() instanceof Player) {
            cir.setReturnValue(entity.getSpeed() * 0.216f);
        }
    }

    @Redirect(method = "travelInWater", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
    private double mounties$modifySwimSpeed(LivingEntity entity, Holder<Attribute> attribute) {
        if (entity instanceof AbstractHorse && entity.onGround() && !entity.isUnderWater()) {
            return 0.8;
        }
        return entity.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY);
    }

    @Inject(method = "floatInWaterWhileRidden", at = @At("HEAD"), cancellable = true)
    private void mounties$onlySwimWhenUnsupported(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        BlockPos pos = this.blockPosition();
        if (entity instanceof AbstractHorse
            && !entity.isUnderWater()
            && this.level().getBlockState(pos.below()).isSolidRender()
        ) {
            ci.cancel();
        }
    }
}
