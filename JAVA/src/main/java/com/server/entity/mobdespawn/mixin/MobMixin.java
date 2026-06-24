package com.server.entity.mobdespawn.mixin;

import com.server.entity.mobdespawn.MobDespawnHandler;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobMixin extends LivingEntity {
	
	protected MobMixin(EntityType<? extends LivingEntity> entityType, Level level) {
		super(entityType, level);
	}
	
	@Inject(at = @At("TAIL"), method = "setItemSlotAndDropWhenKilled")
	private void hwopt$setItemSlotAndDropWhenKilled(EquipmentSlot slot, ItemStack itemStack, CallbackInfo info) {
		MobDespawnHandler.setPersistence((Mob) (Object) this, slot);
	}
	
	@Redirect(method = "checkDespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;discard()V"))
	private void hwopt$checkDespawn(Mob instance) {
		MobDespawnHandler.dropEquipment(instance);
		this.discard();
	}
	
}
