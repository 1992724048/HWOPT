package com.server.render.entityculling.occlusion;

public class OcclusionCullingInstance {
    private int tracingDistance;
    private final DataProvider dataProvider;
    private final java.util.Map<Long, Boolean> cache = new java.util.HashMap<>();

    public OcclusionCullingInstance(int tracingDistance, DataProvider dataProvider) {
        this.tracingDistance = tracingDistance;
        this.dataProvider = dataProvider;
    }

    public void setTracingDistance(int tracingDistance) {
        this.tracingDistance = tracingDistance;
        resetCache();
    }

    public void resetCache() {
        cache.clear();
    }

    public boolean isAABBVisible(Vec3d aabbMin, Vec3d aabbMax, Vec3d camera) {
        double cx = (aabbMin.x + aabbMax.x) / 2.0;
        double cy = (aabbMin.y + aabbMax.y) / 2.0;
        double cz = (aabbMin.z + aabbMax.z) / 2.0;

        double dx = cx - camera.x;
        double dy = cy - camera.y;
        double dz = cz - camera.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist < 1.5) return true;

        int steps = Math.min((int) Math.ceil(dist), tracingDistance);
        double sx = dx / steps;
        double sy = dy / steps;
        double sz = dz / steps;

        int chunkX = Integer.MIN_VALUE;
        for (int i = 1; i < steps; i++) {
            int bx = (int) Math.floor(camera.x + sx * i);
            int by = (int) Math.floor(camera.y + sy * i);
            int bz = (int) Math.floor(camera.z + sz * i);

            int cX = bx >> 4;
            if (cX != chunkX) {
                chunkX = cX;
                if (!dataProvider.prepareChunk(cX, bz >> 4)) return true;
            }

            long key = (((long) bx) << 42) | (((long) by) << 21) | ((long) bz);
            Boolean cached = cache.get(key);
            if (cached != null) {
                if (cached) return false;
                continue;
            }

            boolean opaque = dataProvider.isOpaqueFullCube(bx, by, bz);
            if (opaque) {
                cache.put(key, true);
                return false;
            }
            cache.put(key, false);
        }

        dataProvider.cleanup();
        return true;
    }
}
