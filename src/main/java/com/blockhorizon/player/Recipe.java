package com.blockhorizon.player;

import com.blockhorizon.world.BlockType;

import java.util.Map;

public record Recipe(String name, String description, Map<BlockType, Integer> cost, Map<BlockType, Integer> output) {
}
