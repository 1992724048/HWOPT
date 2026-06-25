package com.server.entity.misc;

import com.hwpp.mod.Config;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PathNavigation.class)
public class PathNavigationMixin {
	@Shadow
	@Final
	protected Mob mob;
	
	@Unique
	private long hwopt$lastPathfindGameTime = 0;
	
	@Inject(method = "recomputePath", at = @At("HEAD"), cancellable = true)
	private void hwopt$onRecomputePath(CallbackInfo ci) {
		Level level = this.mob.level();
		long gameTime = level.getGameTime();
		if (gameTime - this.hwopt$lastPathfindGameTime < Config.CONFIG.pathfindCooldown.get()) {
			ci.cancel();
		} else {
			this.hwopt$lastPathfindGameTime = gameTime;
		}
	}
}
