package aqario.equine.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SweetBerryBushBlock.class)
public class SweetBerryBushBlockMixin {
    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    public void equine$removeBerryBushDamage(
        BlockState blockState,
        Level level,
        BlockPos blockPos,
        Entity entity,
        InsideBlockEffectApplier insideBlockEffectApplier,
        boolean bl,
        CallbackInfo ci
    ) {
        if (entity instanceof AbstractHorse || entity.isPassenger()) {
            ci.cancel();
        }
    }
}
