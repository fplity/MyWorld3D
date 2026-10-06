package com.blockhorizon.world;

import com.badlogic.gdx.math.Vector3;

public record GridPos(int x, int y, int z) {
    public GridPos add(int dx, int dy, int dz) {
        return new GridPos(x + dx, y + dy, z + dz);
    }

    public Vector3 center() {
        return new Vector3(x + 0.5f, y + 0.5f, z + 0.5f);
    }

    public String serialized() {
        return x + "," + y + "," + z;
    }

    public static GridPos parse(String value) {
        String[] parts = value.split(",");
        return new GridPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }
}
