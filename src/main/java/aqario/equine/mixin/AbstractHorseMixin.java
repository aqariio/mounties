package aqario.equine.mixin;

import aqario.equine.common.config.EquineConfig;
import aqario.equine.common.network.ServerboundHorseRearUpPayload;
import aqario.equine.common.util.HorseControl;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.IntUnaryOperator;

@Mixin(value = AbstractHorse.class, priority = 1001)
public abstract class AbstractHorseMixin extends LivingEntity implements HorseControl {
    @Shadow
    protected float playerJumpPendingScale;
    @Shadow
    protected boolean allowStandSliding;

    @Unique
    private double equine$speedPercent = 0F;

    @Unique
    private boolean equine$prevJump = false;

    protected AbstractHorseMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Shadow
    public abstract LivingEntity getControllingPassenger();

    @Shadow
    public abstract boolean isStanding();

    @Override
    public double equine$speedPercent() {
        return equine$speedPercent;
    }

    @Override
    public boolean equine$prevJump() {
        return equine$prevJump;
    }

    @Inject(method = "generateMaxHealth", at = @At("HEAD"), cancellable = true)
    private static void equine$modifyMaxHealth(IntUnaryOperator randomIntGetter, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue((float) EquineConfig.horseMaxHealth);
    }

    @Inject(method = "setOffspringAttribute", at = @At("HEAD"), cancellable = true)
    private static void equine$setOffspringMaxHealth(
        AgeableMob ageableMob,
        AbstractHorse abstractHorse,
        Holder<Attribute> attribute,
        double min,
        double max,
        CallbackInfo ci
    ) {
        if(attribute == Attributes.MAX_HEALTH) {
            abstractHorse.getAttribute(attribute).setBaseValue(EquineConfig.horseMaxHealth);
            ci.cancel();
        }
    }

    @Inject(method = "getRiddenRotation", at = @At("HEAD"), cancellable = true)
    private void equine$customRotationControl(LivingEntity primaryPassenger, CallbackInfoReturnable<Vec2> cir) {
        if(primaryPassenger instanceof Player player) {
            float sidewaysInput = -Math.signum(player.xxa);

            double rotationFactor = 0.08;
            double maxRotation = 10;
            double rotation = Math.atan(rotationFactor / Math.abs(this.getDeltaMovement().horizontalDistance() * 2)) * 180 / Math.PI;
            float clampedRotation = (float) Math.min(rotation, maxRotation);
            if(Math.abs(sidewaysInput) == 0) {
                clampedRotation = 0;
            }

            cir.setReturnValue(new Vec2(0.0F, this.getYRot() + clampedRotation * sidewaysInput));
        }
    }

    @Inject(method = "getRiddenInput", at = @At("HEAD"), cancellable = true)
    private void equine$customAccelerationControl(Player player, Vec3 input, CallbackInfoReturnable<Vec3> cir) {
        if(this.onGround() && this.playerJumpPendingScale == 0.0F && this.isStanding() && !this.allowStandSliding) {
            cir.setReturnValue(Vec3.ZERO);
            return;
        }
        float forwardInput = Math.signum(player.zza);

        // percentage of horse's max speed
        double maxSpeedPercent = 1;
        double minSpeedPercent = 0;
        double accelerationFactor = 0.1;
        double acceleration = maxSpeedPercent * accelerationFactor;

        // acceleration
        if(forwardInput > 0 && equine$speedPercent < maxSpeedPercent) {
            equine$speedPercent = Math.min(maxSpeedPercent, equine$speedPercent + acceleration / (1 + equine$speedPercent * 4));
        }
        // deceleration
        else if(forwardInput < 0 && equine$speedPercent > minSpeedPercent) {
            equine$speedPercent = Math.max(minSpeedPercent, equine$speedPercent - acceleration / (1 + equine$speedPercent / 5));
        }

        // epsilon check
        if(Math.abs(equine$speedPercent) < 0.05) {
            equine$speedPercent *= 0.95;
        }
        if(player.isLocalPlayer() && player instanceof LocalPlayer client) {
            if(!client.input.keyPresses.jump()) {
                equine$prevJump = false;
            }
            // rear up when back is held and jump is pressed
            if(forwardInput < 0 && client.input.keyPresses.jump() && !equine$prevJump) {
                equine$prevJump = true;
                if(equine$speedPercent == 0 && !this.isStanding()) {
                    // send rear up packet
                    ClientPlayNetworking.send(new ServerboundHorseRearUpPayload());
                }
            }
        }

        if(this.level().isClientSide()) {
            cir.setReturnValue(new Vec3(0, 0, equine$speedPercent));
        }
        else {
            cir.setReturnValue(Vec3.ZERO);
        }
    }

    @Override
    public void onPassengerTurned(Entity passenger) {
        super.onPassengerTurned(passenger);
        this.equine$clampRotation(passenger);
    }

    @Unique
    private void equine$clampRotation(Entity entity) {
        entity.setYBodyRot(this.getYRot());
        float f = Mth.wrapDegrees(entity.getYRot() - this.getYRot());
        float g = Mth.clamp(f, -150.0F, 150.0F);
        entity.yRotO += g - f;
        entity.setYRot(entity.getYRot() + g - f);
        entity.setYHeadRot(entity.getYRot());
    }
}
