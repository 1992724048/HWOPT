package com.server.entity.mixin;

import com.server.entity.util.EntityPushSystem;
import com.server.entity.util.TempID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.entity.EntityTickList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
	@Shadow
	@Final
	private EntityTickList entityTickList;
	
	@Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At("HEAD"))
	private void hwopt$onTick(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
		TempID.tickStart();
		entityTickList.forEach(entity -> {
			if (!entity.isRemoved()) {
				TempID.addEntity(entity);
			}
		});
		EntityPushSystem.tick((ServerLevel) (Object) this);
	}
}
