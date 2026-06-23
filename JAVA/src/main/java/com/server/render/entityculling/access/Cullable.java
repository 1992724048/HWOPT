package com.server.render.entityculling.access;

public interface Cullable {
	boolean isCulled();
	void setCulled(boolean culled);
	boolean isForcedVisible();
	void setTimeout(long ticks);
	boolean isOutOfCamera();
	void setOutOfCamera(boolean outOfCamera);
}
