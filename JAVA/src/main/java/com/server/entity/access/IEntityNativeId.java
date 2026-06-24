package com.server.entity.access;

public interface IEntityNativeId {
	int hwopt$getNativeId();
	void hwopt$setNativeId(int id);
	int hwopt$getCollisionCount();
	void hwopt$setCollisionCount(int count);
	void hwopt$extractBoundingBox(double[] arr, int offset);
	void hwopt$extractPosition(double[] arr, int offset);
}
