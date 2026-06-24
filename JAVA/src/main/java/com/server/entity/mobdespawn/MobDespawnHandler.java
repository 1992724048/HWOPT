package com.server.entity.mobdespawn;

import com.hwpp.mod.Config;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Objects;
import java.util.regex.Pattern;

public class MobDespawnHandler {
    public static void setPersistence(Mob entity, EquipmentSlot slot) {
        if (!Config.CONFIG.mobDespawnEnabled.get()) return;

        ItemStack itemStack = entity.getItemBySlot(slot);
        CustomData component = itemStack.get(DataComponents.CUSTOM_DATA);
        CompoundTag nbt = component != null ? component.copyTag() : new CompoundTag();
        nbt.putBoolean("picked", true);
        itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));

        var mobId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        boolean isProtected = Config.CONFIG.mobDespawnProtectedMobs.get().contains(mobId.toString());
        entity.persistenceRequired = isProtected || !hasDespawnableName(entity);
    }

    public static boolean hasDespawnableName(Mob entity) {
        if (entity.hasCustomName()) {
            return matchesStackedName(Objects.requireNonNull(entity.getCustomName()).getString(), entity);
        }
        return true;
    }

    private static boolean matchesStackedName(String customName, Mob entity) {
        String localizedName = Component.translatable(entity.getType().getDescriptionId()).getString();
        return Pattern.compile(Pattern.quote(localizedName) + " x\\d+").matcher(customName).find();
    }

    public static void dropEquipment(Mob entity) {
        if (!Config.CONFIG.mobDespawnEnabled.get()) return;
        var level = entity.level();
        if (!(level instanceof ServerLevel serverLevel)) return;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                entity.spawnAtLocation(serverLevel, stack.copy());
                entity.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
    }
}
