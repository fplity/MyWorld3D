package com.blockhorizon.save;

import java.util.ArrayList;
import java.util.HashMap;

public final class SaveData {
    public static final int CURRENT_VERSION = 3;
    public int version = CURRENT_VERSION;
    public String seed;
    public long savedAt;
    public float playerX;
    public float playerY;
    public float playerZ;
    public float yaw;
    public float pitch;
    public boolean flying;
    public float health = 100f;
    public float hunger = 100f;
    public float stamina = 100f;
    public float timeOfDay = 0.17f;
    public HashMap<String, Integer> inventory = new HashMap<>();
    public int berries;
    public int selectedSlot;
    public boolean creative;
    public HashMap<String, String> edits = new HashMap<>();
    public ArrayList<String> openedChests = new ArrayList<>();
    public ArrayList<String> achievements = new ArrayList<>();
    public int questStage;
    public boolean shrineAwakened;
    public boolean companionUnlocked;
    public int blocksPlaced;
    public float distanceTravelled;
}
