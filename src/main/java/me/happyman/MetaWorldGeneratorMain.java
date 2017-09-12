package me.happyman;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

public class MetaWorldGeneratorMain extends JavaPlugin
{
    private static MetaWorldGeneratorMain instance;

    public static MetaWorldGeneratorMain getInstance()
    {
        return instance;
    }

    @Override
    public void onEnable()
    {
        super.onEnable();
        instance = this;
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id)
    {
        return MetaWorldGenerator.getInstance();
    }
}