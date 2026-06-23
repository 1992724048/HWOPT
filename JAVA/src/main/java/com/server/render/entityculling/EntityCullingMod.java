package com.server.render.entityculling;

import com.hwpp.mod.Config;
import com.server.render.entityculling.occlusion.OcclusionCullingInstance;
import com.server.render.entityculling.occlusion.Provider;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class EntityCullingMod {
	private static EntityCullingMod INSTANCE;
	private final OcclusionCullingInstance culling;
	private final CullTask cullTask;
	private final Thread cullThread;
	private int tickCounter;
	private boolean enabled = true;

	public EntityCullingMod() {
		INSTANCE = this;
		this.culling = new OcclusionCullingInstance(new Provider());
		this.cullTask = new CullTask(culling);
		this.cullThread = new Thread(cullTask, "EntityCulling-CullTask");
		this.cullThread.setDaemon(true);
		this.cullThread.start();
	}

	public static EntityCullingMod getInstance() {
		if (INSTANCE == null) {
			new EntityCullingMod();
		}
		return INSTANCE;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void clientTick() {
		if (!enabled) return;
		tickCounter++;
		var cfg = Config.get();
		if (tickCounter % cfg.captureRate != 0) return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;

		Vec3 camPos = mc.player.getEyePosition();
		cullTask.setCameraPos(camPos);
		cullTask.setLevel(mc.level);

		List<Entity> entities = new ArrayList<>();
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (entities.size() >= cfg.maxEntities) break;
			entities.add(entity);
		}
		cullTask.setEntities(entities);
	}

	public OcclusionCullingInstance getCulling() {
		return culling;
	}
}
