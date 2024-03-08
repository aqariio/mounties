package aqario.mounties.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HorseBaseEntity.class)
public abstract class HorseBaseEntityMixin extends LivingEntity {
    @Shadow
    protected float jumpStrength;
    @Shadow
    private boolean jumping;
    @Unique
    private double prevZMovement = 0F;

    protected HorseBaseEntityMixin(EntityType<? extends LivingEntity> type, World world) {
        super(type, world);
    }

    @Shadow
    @Nullable
    public abstract Entity getPrimaryPassenger();

    @Shadow
    public abstract boolean isAngry();

    @Shadow
    public abstract double getJumpStrength();

    @Shadow
    public abstract boolean isInAir();

    @Shadow
    public abstract void setInAir(boolean inAir);

    @Shadow
    public abstract void setAngry(boolean angry);

    @Shadow
    protected abstract void updateAnger();

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/HorseBaseEntity;getPrimaryPassenger()Lnet/minecraft/entity/LivingEntity;", ordinal = 0), method = "travel", cancellable = true)
    private void mounties$alteredControls(Vec3d movementInput, CallbackInfo info) {
        if (this.getPrimaryPassenger() instanceof PlayerEntity player) {
            float strafingMovement = player.sidewaysSpeed * 0.25F;
            float forwardMovement = player.forwardSpeed;

            boolean flag1 = (forwardMovement >= 0 && prevZMovement + forwardMovement * 0.01F < this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
            boolean flag2 = (forwardMovement <= 0 && prevZMovement + forwardMovement * 0.01F > -this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) * 0.25);

            if (this.world.isClient) {
                if (this.getVelocity().horizontalLength() <= 0 && (forwardMovement < 0 || strafingMovement != 0) && !this.isAngry()) {
                    this.updateAnger();
                }
            }

            if (flag1 || flag2) {
                prevZMovement += forwardMovement * 0.01F;
            }
            forwardMovement = prevZMovement > 0 ? 1 : 0;
            float movementSpeed = (float) Math.abs(prevZMovement);
            if (movementSpeed < 0.05) {
                prevZMovement *= 0.95;
            }
            double v = Math.atan(0.005 / movementSpeed) * 180 / Math.PI;
            double rotationChange = v < 4 ? v : 4;
            if (strafingMovement > 0) {
                this.setYaw((float) (this.getYaw() - rotationChange));
            }
            else if (strafingMovement < 0) {
                this.setYaw((float) (this.getYaw() + rotationChange));
            }

            this.prevYaw = this.getYaw();
            this.bodyYaw = this.getYaw();
            this.headYaw = this.bodyYaw;

            this.setPitch(player.getPitch() * 0.5F);
            this.setRotation(this.getYaw(), this.getPitch());

            if (this.onGround && this.jumpStrength == 0.0F && this.isAngry() && !this.jumping) {
                forwardMovement = 0.0F;
            }

            if (this.jumpStrength > 0.0F && !this.isInAir() && this.onGround) {
                double d = this.getJumpStrength() * (double)this.jumpStrength * (double)this.getJumpVelocityMultiplier();
                double e = d + this.getJumpBoostVelocityModifier();

                Vec3d vec3d = this.getVelocity();
                this.setVelocity(vec3d.x, e, vec3d.z);
                this.setInAir(true);
                this.velocityDirty = true;
                if (prevZMovement > 0.0F) {
                    float h = MathHelper.sin(this.getYaw() * (float) (Math.PI / 180.0));
                    float i = MathHelper.cos(this.getYaw() * (float) (Math.PI / 180.0));
                    this.setVelocity(this.getVelocity().add(-0.4F * h * this.jumpStrength, 0.0, 0.4F * i * this.jumpStrength));
                }

                this.jumpStrength = 0.0F;
            }

            this.flyingSpeed = this.getMovementSpeed() * 0.1F;

            if (this.isLogicalSideForUpdatingMovement()) {
                this.setMovementSpeed(movementSpeed);
                super.travel(new Vec3d(0, movementInput.y, forwardMovement));
            }
            else {
                this.setVelocity(Vec3d.ZERO);
            }

            if (this.onGround) {
                this.jumpStrength = 0.0F;
                this.setInAir(false);
            }

            this.updateLimbs(this, false);
            this.tryCheckBlockCollision();
            info.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "getAmbientSound", cancellable = true)
    private void mounties$cancelRearing(CallbackInfoReturnable<SoundEvent> cir) {
        cir.setReturnValue(null);
    }

    @Override
    public void onPassengerLookAround(Entity passenger) {
        super.onPassengerLookAround(passenger);
        this.copyEntityData(passenger);
    }

    @Unique
    private void copyEntityData(Entity entity) {
        entity.setBodyYaw(this.getYaw());
        float f = MathHelper.wrapDegrees(entity.getYaw() - this.getYaw());
        float g = MathHelper.clamp(f, -150.0F, 150.0F);
        entity.prevYaw += g - f;
        entity.setYaw(entity.getYaw() + g - f);
        entity.setHeadYaw(entity.getYaw());
    }
}
