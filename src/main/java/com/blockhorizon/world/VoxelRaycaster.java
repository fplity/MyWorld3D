package com.blockhorizon.world;

import com.badlogic.gdx.math.Vector3;

public final class VoxelRaycaster {
    public record Hit(GridPos block, GridPos normal, BlockType type, float distance) {
        public GridPos placementPosition() {
            return block.add(normal.x(), normal.y(), normal.z());
        }
    }

    private VoxelRaycaster() {
    }

    public static Hit cast(VoxelWorld world, Vector3 origin, Vector3 direction, float maxDistance) {
        if (!finite(origin) || !finite(direction) || direction.isZero()
                || !Float.isFinite(maxDistance) || maxDistance < 0 || maxDistance > 256f) return null;
        Vector3 ray = new Vector3(direction).nor();
        int x = fastFloor(origin.x);
        int y = fastFloor(origin.y);
        int z = fastFloor(origin.z);
        int stepX = ray.x > 0 ? 1 : ray.x < 0 ? -1 : 0;
        int stepY = ray.y > 0 ? 1 : ray.y < 0 ? -1 : 0;
        int stepZ = ray.z > 0 ? 1 : ray.z < 0 ? -1 : 0;
        float tDeltaX = stepX == 0 ? Float.POSITIVE_INFINITY : Math.abs(1f / ray.x);
        float tDeltaY = stepY == 0 ? Float.POSITIVE_INFINITY : Math.abs(1f / ray.y);
        float tDeltaZ = stepZ == 0 ? Float.POSITIVE_INFINITY : Math.abs(1f / ray.z);
        float tMaxX = intBoundary(origin.x, ray.x);
        float tMaxY = intBoundary(origin.y, ray.y);
        float tMaxZ = intBoundary(origin.z, ray.z);
        float distance = 0f;
        GridPos normal = new GridPos(0, 0, 0);

        while (distance <= maxDistance) {
            BlockType type = world.get(x, y, z);
            if (type != null && type != BlockType.WATER) return new Hit(new GridPos(x, y, z), normal, type, distance);
            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                x += stepX;
                distance = tMaxX;
                tMaxX += tDeltaX;
                normal = new GridPos(-stepX, 0, 0);
            } else if (tMaxY < tMaxZ) {
                y += stepY;
                distance = tMaxY;
                tMaxY += tDeltaY;
                normal = new GridPos(0, -stepY, 0);
            } else {
                z += stepZ;
                distance = tMaxZ;
                tMaxZ += tDeltaZ;
                normal = new GridPos(0, 0, -stepZ);
            }
        }
        return null;
    }

    private static float intBoundary(float s, float ds) {
        if (ds == 0) return Float.POSITIVE_INFINITY;
        float floor = (float) Math.floor(s);
        return ds > 0 ? (floor + 1f - s) / ds : (s - floor) / -ds;
    }

    private static boolean finite(Vector3 v) {
        return v != null && Float.isFinite(v.x) && Float.isFinite(v.y) && Float.isFinite(v.z);
    }

    private static int fastFloor(float value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }
}
