package aqario.mounties.mixin;


import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractHorse.class, priority = 1001)
public abstract class AbstractHorseMixin extends LivingEntity {
    @Shadow
    protected float playerJumpPendingScale;
    @Shadow
    protected boolean allowStandSliding;

    @Unique
    private double mounties$speedPercent = 0F;

    @Unique
    private boolean mounties$backKeyHeld = false;

    protected AbstractHorseMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Shadow
    public abstract LivingEntity getControllingPassenger();

    @Shadow
    public abstract boolean isStanding();

    @Shadow
    public abstract void makeMad();

//    @Inject(at = @At(value = "HEAD"), method = "getChildHealthBonus", cancellable = true)
//    private static void mounties$modifyMaxHealth(IntUnaryOperator randomIntGetter, CallbackInfoReturnable<Float> cir) {
//        cir.setReturnValue(40.0F + (float) randomIntGetter.applyAsInt(8) + (float) randomIntGetter.applyAsInt(9));
//    }

    @Inject(at = @At(value = "HEAD"), method = "getRiddenRotation", cancellable = true)
    private void mounties$customRotationControl(LivingEntity primaryPassenger, CallbackInfoReturnable<Vec2> cir) {
        if (primaryPassenger instanceof Player player) {
            float sidewaysInput = -Math.signum(player.xxa);

            double rotation = Math.atan(0.04 / Math.abs(this.getDeltaMovement().horizontalDistance() * 2)) * 180 / Math.PI;
            float clampedRotation = (float) Math.min(rotation, 5);
            if (Math.abs(sidewaysInput) == 0) {
                clampedRotation = 0;
            }

            cir.setReturnValue(new Vec2(primaryPassenger.getXRot() * 0.5F, this.getYRot() + clampedRotation * sidewaysInput));
        }
    }

    @Inject(at = @At(value = "HEAD"), method = "getRiddenInput", cancellable = true)
    private void mounties$customAccelerationControl(Player player, Vec3 input, CallbackInfoReturnable<Vec3> cir) {
        if (!this.level().isClientSide() || this.onGround() && this.playerJumpPendingScale == 0.0F && this.isStanding() && !this.allowStandSliding) {
            cir.setReturnValue(Vec3.ZERO);
            return;
        }
        float forwardInput = Math.signum(player.zza);

        double maxSpeedPercent = 1;
        double minSpeedPercent = 0;
        double acceleration = maxSpeedPercent * 0.06;

        // acceleration
        if (forwardInput > 0 && mounties$speedPercent < maxSpeedPercent) {
            mounties$speedPercent = Math.min(maxSpeedPercent, mounties$speedPercent + acceleration / (1 + mounties$speedPercent * 4));
        }
        // deceleration
        else if (forwardInput < 0 && mounties$speedPercent > -minSpeedPercent) {
            mounties$speedPercent = Math.max(-minSpeedPercent, mounties$speedPercent - acceleration / (1 + mounties$speedPercent / 5));
        }

        // epsilon check
        if (Math.abs(mounties$speedPercent) < 0.05) {
            mounties$speedPercent *= 0.95;
        }
        // another clamp just to be safe
        mounties$speedPercent = Math.max(mounties$speedPercent, 0);
        if (forwardInput >= 0) {
            mounties$backKeyHeld = false;
        }
        // TODO: fix this
        if (forwardInput < 0 && !mounties$backKeyHeld) {
            mounties$backKeyHeld = true;
            if (mounties$speedPercent <= 0 && !this.isStanding()) {
                this.makeMad();
            }
        }

        cir.setReturnValue(new Vec3(0, 0, mounties$speedPercent));
    }

    @Override
    public void onPassengerTurned(Entity passenger) {
        super.onPassengerTurned(passenger);
        this.clampPassengerYaw(passenger);
    }

    @Unique
    private void clampPassengerYaw(Entity entity) {
        entity.setYBodyRot(this.getYRot());
        float f = Mth.wrapDegrees(entity.getYRot() - this.getYRot());
        float g = Mth.clamp(f, -150.0F, 150.0F);
        entity.yRotO += g - f;
        entity.setYRot(entity.getYRot() + g - f);
        entity.setYHeadRot(entity.getYRot());
    }
}
