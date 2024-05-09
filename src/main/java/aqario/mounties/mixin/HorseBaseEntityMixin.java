package aqario.mounties.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.IntUnaryOperator;

@Mixin(HorseBaseEntity.class)
public abstract class HorseBaseEntityMixin extends LivingEntity {
    @Shadow
    protected float jumpStrength;
    @Shadow
    protected boolean jumping;

    @Unique
    private double mounties$prevSpeedPercent = 0F;

    protected HorseBaseEntityMixin(EntityType<? extends LivingEntity> type, World world) {
        super(type, world);
    }

    @Shadow
    public abstract LivingEntity getPrimaryPassenger();

    @Shadow
    public abstract boolean isAngry();

    @Shadow
    public abstract void rear();

    @Shadow
    protected abstract void initCustomGoals();

    @Inject(at = @At(value = "HEAD"), method = "getChildHealthBonus", cancellable = true)
    private static void mounties$modifyMaxHealth(IntUnaryOperator randomIntGetter, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(40.0F + (float)randomIntGetter.applyAsInt(8) + (float)randomIntGetter.applyAsInt(9));
    }

    @Inject(at = @At(value = "HEAD"), method = "getRotationsFromRider", cancellable = true)
    private void mounties$rotation(LivingEntity primaryPassenger, CallbackInfoReturnable<Vec2f> cir) {
        if (primaryPassenger instanceof PlayerEntity player) {
            float sidewaysMovement = player.sidewaysSpeed * 0.25F;

            if (mounties$prevSpeedPercent <= 0 && sidewaysMovement != 0 && !this.isAngry()) {
                this.rear();
            }

            double rotation = Math.atan(0.02 / Math.abs(this.getVelocity().horizontalLength() * 2)) * 180 / Math.PI;
            double clampedRotation = Math.min(rotation, 5);
            if (Math.abs(sidewaysMovement) == 0) {
                clampedRotation = 0;
            }

            cir.setReturnValue(new Vec2f(primaryPassenger.getPitch() * 0.5F, (float) (this.getYaw() + (clampedRotation * (sidewaysMovement < 0 ? 1 : -1)))));
        }
    }

    @Inject(at = @At(value = "HEAD"), method = "getControlledMovementInput", cancellable = true)
    private void mounties$acceleration(PlayerEntity player, Vec3d input, CallbackInfoReturnable<Vec3d> cir) {
        if (this.isOnGround() && this.jumpStrength == 0.0F && this.isAngry() && !this.jumping) {
            cir.setReturnValue(Vec3d.ZERO);
            return;
        }
        float forwardMovement = player.forwardSpeed;

        double maxSpeed = 1;
        double minSpeed = 0;
        double acceleration = maxSpeed * 0.025;

        if (forwardMovement > 0 && mounties$prevSpeedPercent < maxSpeed) {
            mounties$prevSpeedPercent = Math.min(maxSpeed, mounties$prevSpeedPercent + acceleration / (1 + mounties$prevSpeedPercent * 4));
        }
        else if (forwardMovement < 0 && mounties$prevSpeedPercent > -minSpeed) {
            mounties$prevSpeedPercent = Math.max(-minSpeed, mounties$prevSpeedPercent - acceleration / (1 + mounties$prevSpeedPercent / 2));
        }

        if (Math.abs(mounties$prevSpeedPercent) < 0.05) {
            mounties$prevSpeedPercent *= 0.95;
        }
        mounties$prevSpeedPercent = Math.max(mounties$prevSpeedPercent, 0);
        if (mounties$prevSpeedPercent <= 0 && forwardMovement < 0 && !this.isAngry()) {
            this.rear();
        }

        cir.setReturnValue(new Vec3d(0, 0, mounties$prevSpeedPercent));
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return super.damage(source, amount);
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/HorseBaseEntity;canRear()Z"), method = "initGoals", cancellable = true)
    private void mounties$cancelBucking(CallbackInfo ci) {
        this.initCustomGoals();
        ci.cancel();
    }

    @Override
    public void onPassengerLookAround(Entity passenger) {
        super.onPassengerLookAround(passenger);
        this.clampPassengerYaw(passenger);
    }

    @Unique
    private void clampPassengerYaw(Entity entity) {
        entity.setBodyYaw(this.getYaw());
        float f = MathHelper.wrapDegrees(entity.getYaw() - this.getYaw());
        float g = MathHelper.clamp(f, -150.0F, 150.0F);
        entity.prevYaw += g - f;
        entity.setYaw(entity.getYaw() + g - f);
        entity.setHeadYaw(entity.getYaw());
    }
}
