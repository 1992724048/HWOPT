package com.server.network.mixin;

import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.components.debug.DebugScreenProfile;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(DebugScreenEntries.class)
public interface DebugScreenEntriesAccessor {
    @Accessor("PROFILES_MUTABLE")
    static Map<DebugScreenProfile, Map<Identifier, DebugScreenEntryStatus>> getProfilesMutable() {
        throw new AssertionError();
    }
}
