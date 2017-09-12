package me.happyman;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

import static org.bukkit.block.Biome.TAIGA;
import static org.bukkit.block.Biome.TAIGA_COLD;

public class MetaWorldGenerator extends ChunkGenerator
{
    private static MetaWorldGenerator instance;

    public static ChunkGenerator getInstance()
    {
        if (instance == null)
        {
            instance = new MetaWorldGenerator();
        }
        return instance;
    }

    private static final long seed = -202402786409525631L;
    private static final long seed2 = 123456789012345678L;
    private static final Random handyRandy = new Random();

    private static final ArrayList<BlockPopulator> POPULATORS = new ArrayList<BlockPopulator>();
    private static boolean initialized = false;
    private static final int MAX_HEIGHT_GUESS = 256;
    private static final int WORLD_LENGTH = 42000;
    public static final int HALF_WORLD_LENGTH = WORLD_LENGTH/2;
    //            private static final OceanPoint[] OCEAN_POINTS = new OceanPoint[5];

    private static final short SEA_LEVEL = 64;

    private static final float SAND_PER_BEACH = .05f;
    private static final float PERCENT_OF_PURE_SAND_ON_BEACH = 0.6666f;
    private static final short OCEAN_DEPTH = 20;
    private static final float SAND_REACH_PER_WATERBED_DEPTH = 1f; //how far the sand extends into the ocean or river

    private static final double OCEAN_BELOW_HEIGHT = 0.5f;
    private static final double OCEAN_RATIO_OF_FALLOFF = 0.1d;
    private static final double OCEAN_RATIO_OF_FALLOFF_BEACH = 1.57d; //how much longer falloff takes when compared to the falloff from water's edge to full depth
    private static final float OCEAN_SAND_MAX_DEPTH = SAND_REACH_PER_WATERBED_DEPTH * OCEAN_DEPTH;

    private static final short RIVER_DEPTH = 4;
    private static final double RIVER_AT_HEIGHT_RANGE = 0.014d; //0.014d;
    private static final double RIVER_AT_HEIGHT_FALLOFF_RANGE = RIVER_AT_HEIGHT_RANGE/2 * .2d;
    private static final double RIVER_AT_HEIGHT_FALLOFF_RANGE_BEACH = RIVER_AT_HEIGHT_RANGE/2 * 22.66d; //ditto ^^
    private static final double RIVER_AT_HEIGHT_MAX = 0.5d + RIVER_AT_HEIGHT_RANGE/2;
    private static final double RIVER_AT_HEIGHT_MIN = 0.5d - RIVER_AT_HEIGHT_RANGE/2;
    private static final float RIVER_SAND_MAX_DEPTH = SAND_REACH_PER_WATERBED_DEPTH *RIVER_DEPTH;

    private static final short CREEK_DEPTH = 1;
    private static final double CREEK_AT_HEIGHT_RANGE = 0.0023d;
    private static final double CREEK_AT_HEIGHT_FALLOFF_RANGE = CREEK_AT_HEIGHT_RANGE/2 * 0.2d;
    private static final double CREEK_AT_HEIGHT_FALLOFF_RANGE_BEACH = CREEK_AT_HEIGHT_RANGE/2 * 100d;
    private static final double CREEK_AT_HEIGHT_MAX = 0.8d + CREEK_AT_HEIGHT_RANGE/2;
    private static final double CREEK_AT_HEIGHT_MIN = 0.8d - CREEK_AT_HEIGHT_RANGE/2;

    private static final float HERMITS_PER_HIGH_CHUNK = 0.001f;/*0.0001f * 0x100 / (0x100 - HERMIT_HEIGHT_MIN)*/;
    private static final short HERMIT_HUT_TOTAL_HEIGHT = 10;
    private static final short HERMIT_HEIGHT_MAX =  0x100 - HERMIT_HUT_TOTAL_HEIGHT;
    private static final short HERMIT_HEIGHT_MIN = 100;
    private static final Material HERMIT_HUT_WALL_MATERIAL = Material.BRICK;
    private static final Material HERMIT_HUT_PILLAR_MATERIAL = Material.LOG;
    private static final Material HERMIT_HUT_FLOOR_MATERIAL = Material.WOOD;
    private static final byte HERMIT_HUT_WOOD_SUBTYPE = 1;
    private static final Material HERMIT_HUT_ROOF_HIDDEN_MATERIAL = Material.WOOD;
    private static final Material HERMIT_HUT_STAIRS_MATERIAL = Material.SPRUCE_WOOD_STAIRS;
    private static final byte HERMIT_HUT_ENTRANCE_STAIRS_SUBTYPE = 3;
    private static final byte HERMIT_HUT_WEST_BOUND = 2;
    private static final byte HERMIT_HUT_EAST_BOUND = 13;
    private static final byte HERMIT_HUT_NORTH_BOUND = 5;
    private static final byte HERMIT_HUT_SOUTH_BOUND = 10;
    private static final byte HERMIT_HUT_BLOCK_BUFFER = 2;
    private static final byte HERMIT_HUT_WEST_BOUND_OF_DOOR = (byte)(HERMIT_HUT_WEST_BOUND + (HERMIT_HUT_EAST_BOUND - HERMIT_HUT_WEST_BOUND)/2);
    private static final byte HERMIT_HUT_EAST_BOUND_OF_DOOR = (HERMIT_HUT_EAST_BOUND - HERMIT_HUT_WEST_BOUND) % 2 == 1 ? (byte)(HERMIT_HUT_WEST_BOUND_OF_DOOR + 1) : HERMIT_HUT_WEST_BOUND_OF_DOOR;
    private static final byte HERMIT_HUT_DOOR_X = 13;
    private static final byte HERMIT_HUT_DOOR_Z = 7;

    public static final String KIT_SELLER_HERMIT_NAME = ChatColor.DARK_RED + "" + ChatColor.BOLD + "Shopkeep";
    public static final Villager.Profession KIT_SELLER_HERMIT_PROFESSION = Villager.Profession.LIBRARIAN;


    private static final int CHUNK_PROTECTION_RANGE_BLOCKS = 16*6;
    private static final int PVP_DISABLE_RANGE_BLOCKS = CHUNK_PROTECTION_RANGE_BLOCKS/3;

    public static WorldGenerationTools.ChunkProtectionLevel getWhatProtectionLevelChunkShouldBeBasedOnItsDistanceFromSpawn(int chunkX, int chunkZ)
    {
        int middleOfChunkAbsX = chunkX*0x10 + 7;
        int middleOfChunkAbsZ = chunkZ*0x10 + 7;

        if (middleOfChunkAbsX < CHUNK_PROTECTION_RANGE_BLOCKS && middleOfChunkAbsX > -CHUNK_PROTECTION_RANGE_BLOCKS &&
                middleOfChunkAbsZ < CHUNK_PROTECTION_RANGE_BLOCKS && middleOfChunkAbsZ > -CHUNK_PROTECTION_RANGE_BLOCKS)
        {
            if (middleOfChunkAbsX < PVP_DISABLE_RANGE_BLOCKS && middleOfChunkAbsX > -PVP_DISABLE_RANGE_BLOCKS &&
                    middleOfChunkAbsZ < PVP_DISABLE_RANGE_BLOCKS && middleOfChunkAbsZ > -PVP_DISABLE_RANGE_BLOCKS)
            {
                return WorldGenerationTools.ChunkProtectionLevel.DISABLE_EVERYTHING;
            }
            return WorldGenerationTools.ChunkProtectionLevel.DISABLE_BLOCK_BREAKING;
        }
        return WorldGenerationTools.ChunkProtectionLevel.UNPROTECTED;
    }
    private static final HashMap<Integer, HashMap<Integer, MyChunkData>> chunkData = new HashMap<Integer, HashMap<Integer, MyChunkData>>();

    public static class MyChunkData
    {
        public final float[][] precipitationFactors;
        public final float[][] temperatureFactors;
        public final short[][] highestBlockYs;
        public final Biome[][] biomes;

        public MyChunkData(float[][] precipitationFactors, float[][] temperatureFactors, short[][] highestBlockYs, Biome[][] biomes)
        {
            this.precipitationFactors = precipitationFactors;
            this.temperatureFactors = temperatureFactors;
            this.highestBlockYs = highestBlockYs;
            this.biomes = biomes;
        }
    }

    private static void setChunkData(int chunkX, int chunkZ, MyChunkData data)
    {
        HashMap<Integer, MyChunkData> map = chunkData.get(chunkX);
        if (map == null)
        {
            map = new HashMap<Integer, MyChunkData>();
            chunkData.put(chunkX, map);
        }
        map.put(chunkZ, data);
    }

    private static MyChunkData getChunkData(int chunkX, int chunkZ)
    {
        HashMap<Integer, MyChunkData> mapsOfZ = chunkData.get(chunkX);
        if (mapsOfZ == null)
        {
            return null;
        }
        return mapsOfZ.get(chunkZ);
    }

    private static MyChunkData getAndForgetChunkData(int chunkX, int chunkZ)
    {
        HashMap<Integer, MyChunkData> mapsOfZ = chunkData.get(chunkX);
        if (mapsOfZ == null)
        {
            return null;
        }
        MyChunkData result = mapsOfZ.get(chunkZ);
        if (result == null)
        {
            return null;
        }
        mapsOfZ.remove(chunkZ);
        if (mapsOfZ.size() == 0)
        {
            chunkData.remove(chunkX);
        }
        return result;
    }

    private static int getPowerOf2(short power)
    {
        int length = 1;
        for (short i = 0; i < power; i++)
        {
            length *= 2;
        }
        return length;
    }

    private static final byte[] TERRAIN_INTERPOLATOR_POWERS = new byte[]
            {(byte)2, (byte)3, (byte)4, (byte)5, (byte)6, (byte)7, (byte)8};

    private static final HeightAttribute[] DEFAULT_HEIGHT_ATTRIBUTES = new HeightAttribute[]
            {
                    new HeightAttribute(1.000001f,1.00f, /*.6f*/(byte)8),
                    new HeightAttribute(.6f, .56f, /*.6f*/(byte)8),
                    //transition to surface
                    new HeightAttribute(.3f, .56f, /*.9f*/(byte)8),
                    new HeightAttribute(.21f, .22f, /*.9f*/(byte)8),
                    new HeightAttribute(.17f, .25f, /*.7f*/(byte)8),
                    //transition to ground
                    new HeightAttribute(.11f, .37f, /*0.25f*/(byte)4),
                    new HeightAttribute(.03f, .37f, /*0.25f*/(byte)4),
                    //transition to bedrock
                    new HeightAttribute(0.000f,0.000f,/*.00f*/(byte)4)
            };

    private static class HeightAttribute
    {
        private final float height;
        private final float densityRequirement;
        private final byte interpolatorPowerLimit;

        private HeightAttribute(float minHeightPercent, float airPercent, byte interpolatorPowerLimit)
        {
            this.height = minHeightPercent;
            this.densityRequirement = airPercent;
            this.interpolatorPowerLimit = interpolatorPowerLimit;
        }
    }

    private static final OreVein[] ORE_VEIN_ATTRIBUTES = new OreVein[]
    {
        new OreVein(0.0005f,0, 20, 6, Material.DIAMOND_ORE),
        new OreVein(0.00640f, 125, 256, 6, Material.EMERALD_ORE),
        new OreVein(0.00300f, 0, 30, 4, Material.REDSTONE_ORE),
        new OreVein(0.00833f, 0, 60, 4, Material.IRON_ORE),
        new OreVein(0.0276f, 0, 256, 16, Material.COAL_ORE),
        new OreVein(0.0015f, 0, 30, 3, Material.GOLD_ORE),
        new OreVein(0.010f, 0, 256, 32, Material.GRAVEL)
    };

    private enum NodeOrientation
    {
        NORTH_SOUTH, EAST_WEST, UP_DOWN
    }

    private static class OreVein
    {
        private final float percent;
        private final short materialId;
        private final int lowerBound;
        private final int upperBound;
        private final int avgVeinSize;
        private static final Random r = new Random();

        public OreVein(float percent, int lowerBound, int upperBound, int avgVeinSize, Material material)
        {
            this.percent = percent;
            this.materialId = (short)material.getId();
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
            this.avgVeinSize = avgVeinSize < 0 ? -avgVeinSize : avgVeinSize;
        }
        //                public void create(long seed, byte[][] chunk, byte x, short y, byte z)
        //                {
        //                    if (avgVeinSize > 0)
        //                    {
        //                        r.setSeed(seed - (x*888 + y*888000 + z*888000000));
        //
        //                        int veinSize = r.nextInt(avgVeinSize*2-1) + 1;
        //
        //                        List<OreNode> allNodesInVein = new ArrayList<OreNode>();
        //                        NodeOrientation favoriteOrientation = NodeOrientation.values()[r.nextInt(NodeOrientation.values().length)];
        //                        allNodesInVein.add(new OreNode(allNodesInVein, chunk, x, y, z, favoriteOrientation, favoriteOrientation));
        //
        //                        while (veinSize > 0)
        //                        {
        //                            OreNode interestingNode = null;
        //                            while (interestingNode == null || !interestingNode.canCreateMoreNodes())
        //                            {
        //                                interestingNode = allNodesInVein.get(r.nextInt(allNodesInVein.size()));
        //                            }
        //                            if (interestingNode.expand(r))
        //                            {
        //                                veinSize--;
        //                            }
        //                        }
        //                    }
        //                }

        public void create(long seed, Chunk chunk, byte x, short y, byte z)
        {
            if (avgVeinSize > 0)
            {
                r.setSeed(seed - (x*888 + y*888000 + z*888000000));

                int veinSize = r.nextInt(avgVeinSize*2-1) + 1;

                List<OreVein.OreNode> allNodesInVein = new ArrayList<OreVein.OreNode>();
                NodeOrientation favoriteOrientation = NodeOrientation.values()[r.nextInt(NodeOrientation.values().length)];
                allNodesInVein.add(new OreVein.OreNode(allNodesInVein, chunk, x, y, z, favoriteOrientation, favoriteOrientation));

                while (veinSize > 0)
                {
                    OreVein.OreNode interestingNode = null;
                    while (interestingNode == null || !interestingNode.canCreateMoreNodes())
                    {
                        interestingNode = allNodesInVein.get(r.nextInt(allNodesInVein.size()));
                    }
                    if (interestingNode.expand(r))
                    {
                        veinSize--;
                    }
                }
            }
        }

        private class OreNode
        {
            private final byte x;
            private final short y;
            private final byte z;
            private int remainingBranches;
            private final Chunk chunk;
            private final NodeOrientation favoriteOrientation;
            private final List<OreVein.OreNode> allNodes;

            public OreNode(List<OreVein.OreNode> allNodesInVein, Chunk chunk, byte x, short y, byte z, NodeOrientation orientation, NodeOrientation favoriteOrientation)
            {
                this.allNodes = allNodesInVein;
                this.x = x;
                this.y = y;
                this.chunk = chunk;
                this.z = z;
                this.remainingBranches = favoriteOrientation.equals(orientation) ? 3 : 1;
                this.favoriteOrientation = favoriteOrientation;

                    /*if (chunk instanceof byte[][])
                    {
                        byte[][] castedChunk = (byte[][])chunk;
                        if (x < 16 && y < MAX_HEIGHT_GUESS && z < 16 && x >= 0 && y >= 0 && z >= 0 && getMaterialId(castedChunk, x, y, z) == (short)1)
                        {
                            setMaterial(castedChunk, x, y, z, materialId);
                        }
                    }
                    else if (chunk instanceof Chunk)
                    {
                        Chunk castedChunk = (Chunk)chunk;*/
                if (x < 16 && y < MAX_HEIGHT_GUESS && z < 16 && x >= 0 && y >= 0 && z >= 0 && WorldGenerationTools.getMaterial(chunk, x, y, z).equals(Material.STONE))
                {
                    WorldGenerationTools.setMaterial(chunk, x, y, z, materialId);
                }
                //                        }
            }

            private boolean canCreateMoreNodes()
            {
                return remainingBranches > 0;
            }

            //only returns false if the new block already existed
            private boolean expand(Random r)
            {
                if (canCreateMoreNodes())
                {
                    byte newX = x;
                    short newY = y;
                    byte newZ = z;
                    int negOrPos = r.nextInt(2) == 0 ? -1 : 1;

                    NodeOrientation newOrientation = NodeOrientation.values()[r.nextInt(NodeOrientation.values().length)];

                    switch (newOrientation)
                    {
                        case EAST_WEST:
                            newX += negOrPos;
                            break;
                        case UP_DOWN:
                            newY += negOrPos;
                            break;
                        case NORTH_SOUTH:
                            newZ += negOrPos;
                            break;
                    }

                    for (OreVein.OreNode existingNode : allNodes)
                    {
                        if (existingNode.x == newX && existingNode.y == newY && existingNode.z == newZ)
                        {
                            return false;
                        }
                    }

                    allNodes.add(new OreVein.OreNode(allNodes, chunk, newX, newY, newZ, newOrientation, favoriteOrientation));
                    remainingBranches--;
                    return true;
                }
                return false;
            }
        }
    }

    private static class GroundLayer
    {
        private Material material;
        private int remainingBlocks;

        private GroundLayer(int numberOfBlocks, Material mat)
        {
            this.material = mat;
            this.remainingBlocks = numberOfBlocks;
        }

        private boolean hasRemainingBlocks()
        {
            return remainingBlocks > 0;
        }

        private Material takeBlock()
        {
            remainingBlocks--;
            return material;
        }
    }

    public MetaWorldGenerator()
    {
        //                if (!categoriesUsingThisGenerator.contains(category))
        //                {
        //                    categoriesUsingThisGenerator.add(category);
        //                }
        initialize();
    }

    private static void initialize()
    {
        if (!initialized)
        {
            initialized = true;

            //                    POPULATORS.add(new BlockPopulator()
            //                    {
            //                        @Override
            //                        public void populate(World world, Random random, Chunk chunk)
            //                        {
            //                            //3817667513105396968L;
            //                            int maxHeight = world.getMaxHeight();
            //                            int chunkX = chunk.getX();
            //                            int chunkZ = chunk.getZ();
            //
            //                            int chunkXAbs = chunkX * 0x10;
            //                            int chunkZAbs = chunkZ * 0x10;
            //
            //
            //                            handyRandy.setSeed(seed + chunkX * 240 + chunkZ * 240402);
            //
            //                            BinarySquareInterpolator[] heightInterpolators1 = new BinarySquareInterpolator[]
            //                                    {
            //                                            new BinarySquareInterpolator(seed, (byte) 11, chunkXAbs, chunkZAbs),
            //                                            new BinarySquareInterpolator(seed, (byte)10, chunkXAbs, chunkZAbs),
            //                                            new BinarySquareInterpolator(seed, (byte) 9, chunkXAbs, chunkZAbs),
            //                                            //new BinarySquareInterpolator(seed, (byte)8, chunkXAbs, chunkZAbs),
            //                                            new BinarySquareInterpolator(seed, (byte) 7, chunkXAbs, chunkZAbs),
            //                                            //new BinarySquareInterpolator(seed, (byte)6, chunkXAbs, chunkZAbs),
            //                                            new BinarySquareInterpolator(seed, (byte) 5, chunkXAbs, chunkZAbs),
            //                                            //new BinarySquareInterpolator(seed, (byte)4, chunkXAbs, chunkZAbs),
            //                                            //new BinarySquareInterpolator(seed, (byte)3, chunkXAbs, chunkZAbs),
            //                                            //new BinarySquareInterpolator(seed, (byte)2, chunkXAbs, chunkZAbs)
            //                                    };
            //
            //                            BinarySquareInterpolator[] heightInterpolators2 = new BinarySquareInterpolator[]
            //                                    {
            //                                            //new BinarySquareInterpolator(seed2, (byte)12, chunkXAbs, chunkZAbs),
            //                                            new BinarySquareInterpolator(seed2, (byte)11, chunkXAbs, chunkZAbs),
            //                                            new BinarySquareInterpolator(seed2, (byte)10, chunkXAbs, chunkZAbs),
            //                                            new BinarySquareInterpolator(seed2, (byte)9, chunkXAbs, chunkZAbs),
            //                                            //new BinarySquareInterpolator(seed, (byte)8, chunkXAbs, chunkZAbs),
            //                                            new BinarySquareInterpolator(seed2, (byte)7, chunkXAbs, chunkZAbs),
            //                                            //new BinarySquareInterpolator(seed, (byte)6, chunkXAbs, chunkZAbs),
            //                                            new BinarySquareInterpolator(seed2, (byte)5, chunkXAbs, chunkZAbs)
            //                                            //new BinarySquareInterpolator(seed, (byte)4, chunkXAbs, chunkZAbs),
            //                                            //new BinarySquareInterpolator(seed, (byte)2, chunkXAbs, chunkZAbs)
            //                                    };
            //
            //                            //build the terrain
            //                            BinaryCubeInterpolator[] defaultInterpolaters = BinaryCubeInterpolator.getInterpolators(seed, TERRAIN_INTERPOLATOR_POWERS, chunkXAbs, 0, chunkZAbs);
            //
            //                            for (short y = 0; y < maxHeight; y++)
            //                            {
            //                                float requiredDensity = -1;
            //                                float percentHeight = (float) y / maxHeight;
            //
            //                                byte highMaxPower = 0;
            //                                byte lowMaxPower = 0;
            //                                float miniPercent = 0f;
            //                                for (byte i = 0; i < DEFAULT_HEIGHT_ATTRIBUTES.length; i++)
            //                                {
            //                                    HeightAttribute haLow = DEFAULT_HEIGHT_ATTRIBUTES[i];
            //                                    if (percentHeight >= haLow.height)
            //                                    {
            //                                        HeightAttribute haHigh = DEFAULT_HEIGHT_ATTRIBUTES[i - 1];
            //
            //                                        miniPercent = (percentHeight - haLow.height) / (haHigh.height - haLow.height);
            //                                        requiredDensity = miniPercent * haHigh.densityRequirement + (1f - miniPercent) * haLow.densityRequirement;
            //                                        highMaxPower = haHigh.interpolatorPowerLimit;
            //                                        lowMaxPower = haLow.interpolatorPowerLimit;
            //                                        break;
            //                                    }
            //                                }
            //                                //This is pretty dope
            //                                //                                float groundMin = 0.26f;
            //                                //                                float reqGround = 0.1f;
            //                                //                                float groundMax = 0.3f;
            //                                //                                float reqSky = 0.7f;
            //                                //
            //                                //                                if (percentHeight < groundMin)
            //                                //                                {
            //                                //                                    requiredDensity = reqGround;
            //                                //                                }
            //                                //                                else if (percentHeight < groundMax)
            //                                //                                {
            //                                //                                    float miniPercent = (percentHeight - groundMin)/(groundMax - groundMin);
            //                                //
            //                                //                                    requiredDensity = reqGround*(1 - miniPercent) + reqSky*miniPercent;
            //                                //                                }
            //                                //                                else
            //                                //                                {
            //                                //                                    requiredDensity = reqSky;
            //                                //                                }
            //
            //                                int absX = chunkXAbs;
            //                                for (byte x = 0; x < 0x10; x++, absX++)
            //                                {
            //                                    int absZ = chunkZAbs;
            //                                    for (byte z = 0; z < 0x10; z++, absZ++)
            //                                    {
            //                                        final float densityHere = highMaxPower != lowMaxPower ? BinaryCubeInterpolator.getDensity(defaultInterpolaters, absX, y, absZ, miniPercent, lowMaxPower, highMaxPower)
            //                                                : BinaryCubeInterpolator.getDensity(defaultInterpolaters, absX, y, absZ, lowMaxPower);
            //                                        if (densityHere >= requiredDensity)
            //                                        {
            //                                            WorldGenerationTools.setMaterial(chunk, x, y, z, (byte)1);
            //                                        }
            //                                        //                            if (biome.getBiome(getGlobalX, getGlobalZ).equals(Biome.FOREST))
            //                                        //                            {
            //                                        //
            //                                        //                            }
            //                                    }
            //                                }
            //                            }
            //
            //                            double[][] height1s = new double[0x10][0x10];
            //                            double[][] height2s = new double[0x10][0x10];
            //                            short[][] highestBlockYs = new short[0x10][0x10];
            //                            //byte[][] highestBlockYsIn9 = new short[0x10*3][0x10*3];
            //
            //                            //                for (byte xIn9 = 0, xRelBase = (byte)-0x10; xIn9 < 0x20; xIn9++, xRelBase++)
            //                            //                {
            //                            //                    int absX = xRelBase + chunkXAbs;
            //                            //                    for (byte zIn9 = 0, zRelBase = (byte)-0x10; zIn9 < 0x20; zIn9++, zRelBase++)
            //                            //                    {
            //                            //                        int absZ = zRelBase + chunkZAbs;
            //                            //                        height1s9[xIn9][zIn9] = BinarySquareInterpolator.getHeight(heightInterpolators1, absX, absZ);
            //                            //                        height2s9[xIn9][zIn9] = BinarySquareInterpolator.getHeight(heightInterpolators2, absX, absZ);
            //                            //                        //highestBlockYsIn9[xIn9][zIn9] = (short)world.getHighestBlockYAt(absX, absZ);
            //                            //                    }
            //                            //                }
            //
            //                            for (byte x = 0; x < 0x10; x++)
            //                            {
            //                                int absX = x + chunkXAbs;
            //                                for (byte z = 0; z < 0x10; z++)
            //                                {
            //                                    int absZ = z + chunkZAbs;
            //                                    height1s[x][z] = BinarySquareInterpolator.getHeight(heightInterpolators1, absX, absZ);
            //                                    height2s[x][z] = BinarySquareInterpolator.getHeight(heightInterpolators2, absX, absZ);
            //                                    short highestBlockY;
            //                                    for (highestBlockY = (short)(maxHeight - 1); highestBlockY > 0 && WorldGenerationTools.getMaterialId(chunk, x, highestBlockY, z) == (short)0; highestBlockY--);
            //                                    highestBlockYs[x][z] = highestBlockY;
            //                                    world.setBiome(absX, absZ, Biome.PLAINS); //do not remove this
            //                                    WorldGenerationTools.setMaterial(chunk, x, (byte)0, z, (byte)7); //or this
            //                                }
            //                            }
            //
            //                            //add oceans and rivers
            //            /**/
            //                            //                    Double ratioDistanceToNearest = null;
            //                            //                    OceanPoint nearestOcean = null;
            //                            //                    Double ratioDistanceToSecondNearest = null;
            //                            //                    OceanPoint secondNearestOcean = null;
            //                            //
            //                            //                    for (byte i = 0; i < OCEAN_POINTS.length; i++)
            //                            //                    {
            //                            //                        double ratioDistance = Math.sqrt((chunkXAbs - OCEAN_POINTS[i].getGlobalX) * (chunkXAbs - OCEAN_POINTS[i].getGlobalX) + (chunkZAbs - OCEAN_POINTS[i].getGlobalZ) * (chunkZAbs - OCEAN_POINTS[i].getGlobalZ)) / OCEAN_POINTS[i].radius;
            //                            //                        if (nearestOcean == null || ratioDistance < ratioDistanceToNearest)
            //                            //                        {
            //                            //                            secondNearestOcean = nearestOcean;
            //                            //                            ratioDistanceToSecondNearest = ratioDistanceToNearest;
            //                            //
            //                            //                            nearestOcean = OCEAN_POINTS[i];
            //                            //                            ratioDistanceToNearest = ratioDistance;
            //                            //                        }
            //                            //                        else if (secondNearestOcean == null || ratioDistance < ratioDistanceToSecondNearest)
            //                            //                        {
            //                            //                            secondNearestOcean = OCEAN_POINTS[i];
            //                            //                            ratioDistanceToSecondNearest = ratioDistance;
            //                            //                        }
            //                            //                    }
            //
            //                            //add oceans, rivers, and creeks
            //                            //for (byte xRelBase = 0, xIn9 = (byte)0x10; xRelBase < 0x10; xIn9++, xRelBase++)
            //                            for (byte x = 0; x < 0x10; x++)
            //                            {
            //                                int absX = x + chunkXAbs;
            //                                //for (byte zRelBase = 0, zIn9 = (byte)0x10; zRelBase < 0x10; zIn9++, zRelBase++)
            //                                for (byte z = 0; z < 0x10; z++)
            //                                {
            //                                    int absZ = z + chunkZAbs;
            //                                    double height1 = height1s[x][z];
            //                                    double height2 = height2s[x][z];
            //
            //                                    short oceanDepth = 0;
            //                                    short riverDepth = 0;
            //                                    short creekDepth = 0;
            //                                    float sandOnBeach = SAND_PER_BEACH;
            //                                    Double beachHeightCapAlpha = null;
            //
            //                                    if (height1 < OCEAN_BELOW_HEIGHT*(1d - OCEAN_RATIO_OF_FALLOFF))
            //                                    {
            //                                        oceanDepth = OCEAN_DEPTH;
            //                                    }
            //                                    else if (height1 < OCEAN_BELOW_HEIGHT) //transition from ocean to land
            //                                    {
            //                                        final short depth = (short)((OCEAN_BELOW_HEIGHT - height1) / (OCEAN_BELOW_HEIGHT*OCEAN_RATIO_OF_FALLOFF) * OCEAN_DEPTH);
            //                                        oceanDepth = depth > 0 ? depth : 1;
            //                                    }
            //                                    else if (height1 <= OCEAN_BELOW_HEIGHT*(1d + OCEAN_RATIO_OF_FALLOFF_BEACH))
            //                                    {
            //                                        beachHeightCapAlpha = (height1 - OCEAN_BELOW_HEIGHT) / (OCEAN_BELOW_HEIGHT*OCEAN_RATIO_OF_FALLOFF_BEACH);
            //                                    }
            //
            //                                    if (oceanDepth != OCEAN_DEPTH)
            //                                    {
            //                                        //river code
            //                                        if (height2 >= RIVER_AT_HEIGHT_MIN && height2 <= RIVER_AT_HEIGHT_MAX)
            //                                        {
            //                                            final short depth;
            //                                            if (height2 <= RIVER_AT_HEIGHT_MIN + RIVER_AT_HEIGHT_FALLOFF_RANGE)
            //                                            {
            //                                                depth = (short) ((height2 - RIVER_AT_HEIGHT_MIN) / RIVER_AT_HEIGHT_FALLOFF_RANGE * RIVER_DEPTH);
            //                                            }
            //                                            else if (height2 >= RIVER_AT_HEIGHT_MAX - RIVER_AT_HEIGHT_FALLOFF_RANGE)
            //                                            {
            //                                                depth = (short) ((RIVER_AT_HEIGHT_MAX - height2) / RIVER_AT_HEIGHT_FALLOFF_RANGE * RIVER_DEPTH);
            //                                            }
            //                                            else
            //                                            {
            //                                                depth = RIVER_DEPTH;
            //                                            }
            //                                            riverDepth = depth > 0 ? depth : 1;
            //                                        }
            //                                        else if (height2 > RIVER_AT_HEIGHT_MIN - RIVER_AT_HEIGHT_FALLOFF_RANGE_BEACH && height2 < RIVER_AT_HEIGHT_MAX + RIVER_AT_HEIGHT_FALLOFF_RANGE_BEACH)
            //                                        {
            //                                            final double newAlpha;
            //                                            if (height2 < RIVER_AT_HEIGHT_MIN)
            //                                            {
            //                                                newAlpha = (RIVER_AT_HEIGHT_MIN - height2) / RIVER_AT_HEIGHT_FALLOFF_RANGE_BEACH;
            //                                            }
            //                                            else
            //                                            {
            //                                                newAlpha = (height2 - RIVER_AT_HEIGHT_MAX) / RIVER_AT_HEIGHT_FALLOFF_RANGE_BEACH;
            //                                            }
            //                                            if (beachHeightCapAlpha == null || newAlpha < beachHeightCapAlpha)
            //                                            {
            //                                                beachHeightCapAlpha = newAlpha;
            //                                            }
            //                                        }
            //
            //                                        if (riverDepth != RIVER_DEPTH)
            //                                        {//creek code
            //                                            if (height2 >= CREEK_AT_HEIGHT_MIN && height2 <= CREEK_AT_HEIGHT_MAX)
            //                                            {
            //                                                final short depth;
            //                                                if (height2 <= CREEK_AT_HEIGHT_MIN + CREEK_AT_HEIGHT_FALLOFF_RANGE)
            //                                                {
            //                                                    depth = (short)((height2 - CREEK_AT_HEIGHT_MIN) / CREEK_AT_HEIGHT_FALLOFF_RANGE * CREEK_DEPTH);
            //                                                }
            //                                                else if (height2 >= CREEK_AT_HEIGHT_MAX - CREEK_AT_HEIGHT_FALLOFF_RANGE)
            //                                                {
            //                                                    depth = (short)((CREEK_AT_HEIGHT_MAX - height2) / CREEK_AT_HEIGHT_FALLOFF_RANGE * CREEK_DEPTH);
            //                                                }
            //                                                else
            //                                                {
            //                                                    depth = CREEK_DEPTH;
            //                                                }
            //                                                creekDepth = depth > 0 ? depth : 1;
            //                                            }
            //                                            else if (height2 > CREEK_AT_HEIGHT_MIN - CREEK_AT_HEIGHT_FALLOFF_RANGE_BEACH && height2 < CREEK_AT_HEIGHT_MAX + CREEK_AT_HEIGHT_FALLOFF_RANGE_BEACH)
            //                                            {
            //                                                sandOnBeach = 0;
            //                                                final double newAlpha;
            //                                                if (height2 < CREEK_AT_HEIGHT_MIN)
            //                                                {
            //                                                    newAlpha = (CREEK_AT_HEIGHT_MIN - height2) / CREEK_AT_HEIGHT_FALLOFF_RANGE_BEACH;
            //                                                }
            //                                                else
            //                                                {
            //                                                    newAlpha = (height2 - CREEK_AT_HEIGHT_MAX) / CREEK_AT_HEIGHT_FALLOFF_RANGE_BEACH;
            //                                                }
            //                                                if (beachHeightCapAlpha == null || newAlpha < beachHeightCapAlpha)
            //                                                {
            //                                                    beachHeightCapAlpha = newAlpha;
            //                                                }
            //                                            }
            //                                        }
            //                                    }
            //
            //                                    final short totalDepth = (short)(oceanDepth + riverDepth + creekDepth);
            //
            //                                    //alpha is fine, the problem is here
            //                                    short highestBlockY = highestBlockYs[x][z];
            //
            //                                    boolean sandHere = false;
            //                                    if (beachHeightCapAlpha != null)
            //                                    {
            //                                        BinarySquareInterpolator beachInterpolator = new BinarySquareInterpolator(seed, (byte)4, absX, absZ);
            //                                        short blockThreshold = (short) (beachHeightCapAlpha * (maxHeight - SEA_LEVEL) * (1d + beachInterpolator.getHeight(absX, absZ) * 0.2));
            //
            //                                        if (highestBlockY > SEA_LEVEL + blockThreshold)
            //                                        {
            //                                            for (short y = highestBlockY; y > SEA_LEVEL + blockThreshold; y--)
            //                                            {
            //                                                WorldGenerationTools.setMaterial(chunk, x, y, z, (byte)0);
            //                                            }
            //                                        }
            //                                        else if (highestBlockY < SEA_LEVEL - blockThreshold)
            //                                        {
            //                                            for (short y = (short) (highestBlockY + 1); y <= SEA_LEVEL - blockThreshold; y++)
            //                                            {
            //                                                WorldGenerationTools.setMaterial(chunk, x, y, z, (byte)1);
            //                                            }
            //                                        }
            //
            //                                        for (highestBlockY = (short)(maxHeight - 1); highestBlockY > 0 && WorldGenerationTools.getMaterialId(chunk, x, highestBlockY, z) != (short)1; highestBlockY--);
            //
            //                                        if (beachHeightCapAlpha < sandOnBeach && highestBlockY < SEA_LEVEL + 6 && highestBlockY > SEA_LEVEL - 20)
            //                                        {
            //                                            world.setBiome(absX, absZ, Biome.BEACH);
            //                                            if (beachHeightCapAlpha < sandOnBeach * PERCENT_OF_PURE_SAND_ON_BEACH ||
            //                                                    handyRandy.nextFloat() > (float) (beachHeightCapAlpha / sandOnBeach - PERCENT_OF_PURE_SAND_ON_BEACH) / (1f - PERCENT_OF_PURE_SAND_ON_BEACH))
            //                                            {
            //                                                sandHere = true;
            //                                            }
            //                                        }
            //                                    }
            //
            //                                    if (totalDepth > 0)
            //                                    {
            //                                        if (oceanDepth > 0)
            //                                        {
            //                                            world.setBiome(absX, absZ, Biome.OCEAN);
            //                                            if (!sandHere && oceanDepth < OCEAN_SAND_MAX_DEPTH)
            //                                            {
            //                                                float percentDepth = (float)oceanDepth/OCEAN_SAND_MAX_DEPTH;
            //                                                sandHere = percentDepth < PERCENT_OF_PURE_SAND_ON_BEACH || handyRandy.nextFloat() < (1f - percentDepth)/(1f - PERCENT_OF_PURE_SAND_ON_BEACH);
            //                                            }
            //                                        }
            //                                        else if (riverDepth > 0)
            //                                        {
            //                                            world.setBiome(absX, absZ, Biome.RIVER);
            //                                            if (!sandHere && riverDepth < RIVER_SAND_MAX_DEPTH)
            //                                            {
            //                                                float percentDepth = (float)riverDepth/RIVER_SAND_MAX_DEPTH;
            //                                                sandHere = percentDepth < PERCENT_OF_PURE_SAND_ON_BEACH || handyRandy.nextFloat() < (1f - percentDepth)/(1f - PERCENT_OF_PURE_SAND_ON_BEACH);
            //                                            }
            //                                        }
            //
            //                                        for (short y = (short) (maxHeight - 1); y > SEA_LEVEL; y--)
            //                                        {
            //                                            WorldGenerationTools.setMaterial(chunk, x, y, z, (byte)0);
            //                                        }
            //
            //                                        for (byte findY = SEA_LEVEL; findY > 0; findY--)
            //                                        {
            //                                            if (WorldGenerationTools.getMaterial(chunk, x, findY, z) != Material.AIR)
            //                                            {
            //                                                for (short y = findY; y > 0 && y > findY - totalDepth; y--)
            //                                                {
            //                                                    WorldGenerationTools.setMaterial(chunk, x, y, z, (byte)0);
            //                                                }
            //                                                break;
            //                                            }
            //                                        }
            //
            //                                        for (byte y = SEA_LEVEL; y > 0; y--)
            //                                        {
            //                                            short matId = WorldGenerationTools.getMaterialId(chunk, x, y, z);
            //                                            if (matId != 8 && matId != 9 && matId != 0)
            //                                            {
            //                                                break;
            //                                            }
            //
            //                                            WorldGenerationTools.setMaterial(chunk, x, y, z, (byte) 9);
            //                                        }
            //                                    }
            //
            //                                    if (sandHere)
            //                                    {
            //                                        for (highestBlockY = (short)(maxHeight - 1); highestBlockY > 0 && WorldGenerationTools.getMaterialId(chunk, x, highestBlockY, z) != (short)1; highestBlockY--);
            //                                        for (short y = highestBlockY; y >= highestBlockY - 5 && y > 0; y--)
            //                                        {
            //                                            WorldGenerationTools.setMaterial(chunk, x, y, z, (byte)12);
            //                                        }
            //                                    }
            //
            //                                    highestBlockYs[x][z] = WorldGenerationTools.getHighestBlockYInChunk(chunk, x, z);
            //                                }
            //                            }
            //
            //                            float[][] precipitationFactors = new float[0x10][0x10];
            //                            float[][] temperatureFactors = new float[0x10][0x10];
            //
            //                            //Determine precipitation & temperature & set biome accordingly
            //                            for (byte z = 0; z < 0x10; z++)
            //                            //for (byte zRelBase = -0x10, zIn9 = 0; zRelBase < 0x20; zIn9++, zRelBase++)
            //                            {
            //                                int absZ = z + chunkZAbs;
            //
            //                                final float simpleTempFactor;
            //                                if (absZ < -HALF_WORLD_LENGTH)
            //                                {
            //                                    simpleTempFactor = 0;
            //                                }
            //                                else if (absZ > HALF_WORLD_LENGTH)
            //                                {
            //                                    simpleTempFactor = 1f;
            //                                }
            //                                else
            //                                {
            //                                    simpleTempFactor = (float)(absZ + HALF_WORLD_LENGTH) / WORLD_LENGTH;
            //                                }
            //
            //                                for (byte x = 0; x < 0x10; x++)
            //                                //for (short xRelBase = -0x10, xIn9 = 0; xRelBase < 0x20; xIn9++, xRelBase++)
            //                                {
            ////                            float precipFactor = (float)height1s9[xIn9][zIn9];
            ////                            precipitationFactorsIn9[xIn9][zIn9] = precipFactor;
            ////                            float tempFactor = 0.8f*simpleTempFactor + 0.2f*(float)height2s9[xIn9][xIn9];
            ////                            temperatureFactorsIn9[xIn9][zIn9] = tempFactor;
            //                                    precipitationFactors[x][z] = (float)height1s[x][z];
            //                                    temperatureFactors[x][z] = 0.8f*simpleTempFactor + 0.2f*(float)height2s[x][z];
            //                                }
            //                            }
            //
            //                            //grass is in a populator
            //
            //                            //ores are in a populator
            //
            //                            //trees are in a populator
            //
            //                            WorldGenerationTools.ChunkProtectionLevel level = WorldGenerationTools.getWhatProtectionLevelChunkShouldBeBasedOnItsDistanceFromSpawn(chunkX, chunkZ);
            //                            if (level != WorldGenerationTools.ChunkProtectionLevel.UNPROTECTED)
            //                            {
            //                                WorldGenerationTools.setChunkProtectionLevel(chunk, level);
            //                                if (level == WorldGenerationTools.ChunkProtectionLevel.DISABLE_EVERYTHING)
            //                                {
            //                                    if (WorldGenerationTools.getWhatProtectionLevelChunkShouldBeBasedOnItsDistanceFromSpawn(chunkX + 1, chunkZ) != WorldGenerationTools.ChunkProtectionLevel.DISABLE_EVERYTHING)
            //                                    {
            //                                        byte x = 0xF;
            //                                        for (byte z = 0; z < 0x10; z++)
            //                                        {
            //                                            short highestBlock = highestBlockYs[x][z];
            //                                            WorldGenerationTools.setMaterial(chunk, x, highestBlock, z, (short)22);
            //                                        }
            //                                    }
            //                                    if (WorldGenerationTools.getWhatProtectionLevelChunkShouldBeBasedOnItsDistanceFromSpawn(chunkX, chunkZ + 1) != WorldGenerationTools.ChunkProtectionLevel.DISABLE_EVERYTHING)
            //                                    {
            //                                        byte z = 0xF;
            //                                        for (byte x = 0; x < 0x10; x++)
            //                                        {
            //                                            short highestBlock = highestBlockYs[x][z];
            //                                            WorldGenerationTools.setMaterial(chunk, x, highestBlock, z, (short)22);
            //                                        }
            //                                    }
            //                                    if (WorldGenerationTools.getWhatProtectionLevelChunkShouldBeBasedOnItsDistanceFromSpawn(chunkX - 1, chunkZ) != WorldGenerationTools.ChunkProtectionLevel.DISABLE_EVERYTHING)
            //                                    {
            //                                        byte x = 0x0;
            //                                        for (byte z = 0; z < 0x10; z++)
            //                                        {
            //                                            short highestBlock = highestBlockYs[x][z];
            //                                            WorldGenerationTools.setMaterial(chunk, x, highestBlock, z, (short)22);
            //                                        }
            //                                    }
            //                                    if (WorldGenerationTools.getWhatProtectionLevelChunkShouldBeBasedOnItsDistanceFromSpawn(chunkX, chunkZ - 1) != WorldGenerationTools.ChunkProtectionLevel.DISABLE_EVERYTHING)
            //                                    {
            //                                        byte z = 0x0;
            //                                        for (byte x = 0; x < 0x10; x++)
            //                                        {
            //                                            short highestBlock = highestBlockYs[x][z];
            //                                            WorldGenerationTools.setMaterial(chunk, x, highestBlock, z, (short)22);
            //                                        }
            //                                    }
            //                                }
            //                            }
            //                            setChunkData(chunkX, chunkZ, new MyChunkData(precipitationFactors, temperatureFactors, highestBlockYs));
            //                        }
            //                    });


            //the one other populator
            POPULATORS.add(new BlockPopulator()
            {
                public static final int MAX_CACTUS_HEIGHT = 5;
                private static final float MAX_GRASS_PERCENTAGE = 0.75f;

                @Override
                public void populate(World world, Random random, final Chunk chunk)
                {
                    try
                    {
                        int chunkX = chunk.getX();
                        int chunkZ = chunk.getZ();
                        if (getWhatProtectionLevelChunkShouldBeBasedOnItsDistanceFromSpawn(chunkX, chunkZ) == WorldGenerationTools.ChunkProtectionLevel.DISABLE_EVERYTHING)
                        {
                            return;
                        }

                        MyChunkData data = getAndForgetChunkData(chunkX, chunkZ);
                        if (data != null)
                        {
                            int chunkXAbs = chunkX * 0x10;
                            int chunkZAbs = chunkZ * 0x10;

                            //figure out the biomes and put in grass and trees

                            int absX = chunkXAbs;
                            for (byte x = 0; x < 0x10; x++, absX++)
                            {
                                int absZ = chunkZAbs;
                                for (byte z = 0; z < 0x10; z++, absZ++)
                                {
                                    float precipitationFactor = data.precipitationFactors[x][z];
                                    float temperatureFactor = data.temperatureFactors[x][z];

                                    //figure biomes out
                                    Biome newBiome = Biome.PLAINS;
                                    if (x >= 0 && x < 0x10 && z >= 0 && z < 0x10)
                                    {
                                        int precipitationFactorInt = (int) (6f * precipitationFactor);
                                        int temperatureFactorInt = (int) (6f * temperatureFactor);

                                        final Biome oldBiome = data.biomes[x][z];
                                        newBiome = oldBiome;
                                        switch (oldBiome)
                                        {
                                            case BEACHES:
                                                if (temperatureFactorInt < 2)
                                                {
                                                    newBiome = Biome.COLD_BEACH;
                                                }
                                                break;
                                            case OCEAN:
                                                if (temperatureFactorInt < 2)
                                                {
                                                    newBiome = Biome.FROZEN_OCEAN;
                                                }
                                                break;
                                            case RIVER:
                                                if (temperatureFactorInt < 2)
                                                {
                                                    newBiome = Biome.FROZEN_RIVER;
                                                }
                                                break;
                                            default:
                                                switch (temperatureFactorInt)
                                                {
                                                    case 0:
                                                        newBiome = Biome.ICE_FLATS;
                                                        break;
                                                    case 1:
                                                        newBiome = TAIGA_COLD;
                                                        break;
                                                    case 2:
                                                        switch (precipitationFactorInt)
                                                        {
                                                            case 0:
                                                            case 1:
                                                                newBiome = Biome.PLAINS;
                                                                break;
                                                            case 2:
                                                                newBiome = Biome.FOREST;
                                                                break;
                                                            case 3:
                                                            case 4:
                                                            case 5:
                                                                newBiome = TAIGA; //boreal forest
                                                                break;
                                                        }
                                                        break;
                                                    case 3:
                                                        switch (precipitationFactorInt)
                                                        {
                                                            case 0:
                                                            case 1:
                                                                newBiome = Biome.DESERT;
                                                                break;
                                                            case 2:
                                                            case 3:
                                                                newBiome = Biome.FOREST;
                                                                break;
                                                            case 4:
                                                                newBiome = Biome.FOREST; //Seasonal Forest
                                                                break;
                                                            case 5:
                                                                newBiome = Biome.FOREST; //Temperate Forest
                                                                break;
                                                        }
                                                        break;
                                                    case 4:
                                                    case 5:
                                                        switch (precipitationFactorInt)
                                                        {
                                                            case 0:
                                                                newBiome = Biome.DESERT;
                                                                break;
                                                            case 1:
                                                                newBiome = Biome.SAVANNA;
                                                                break;
                                                            case 2:
                                                            case 3:
                                                                newBiome = Biome.FOREST;
                                                                break;
                                                            case 4:
                                                            case 5:
                                                                newBiome = Biome.JUNGLE; //Tropical Rainforest
                                                                break;
                                                        }
                                                        break;
                                                }
                                                break;
                                        }
                                        world.setBiome(absX, absZ, newBiome);
                                    }

                                    handyRandy.setSeed(seed - (absX * 777770 + absZ * 7770));
                                    short highestBlockY = data.highestBlockYs[x][z];

                                    //put in some grass
                                    GroundLayer[] groundLayers = null;
                                    int layerIndex = -1;
                                    float grassPercent = MAX_GRASS_PERCENTAGE * precipitationFactor;
                                    Material seaMat = WorldGenerationTools.getMaterial(chunk, x, SEA_LEVEL, z);
                                    if (!seaMat.equals(Material.WATER) && !seaMat.equals(Material.STATIONARY_WATER) && !WorldGenerationTools.getMaterial(chunk, x, highestBlockY, z).equals(Material.SAND))
                                    {
                                        for (short y = highestBlockY + 10 < 0x100 ? (short) (highestBlockY + 10) : highestBlockY; y > 0; y--)
                                        {
                                            Material curMat = WorldGenerationTools.getMaterial(chunk, x, y, z);

                                            if ((curMat.equals(Material.AIR) || curMat.equals(Material.WATER) || curMat.equals(Material.STATIONARY_WATER)) && (chunk.getBlock(x, y, z).getLightFromSky() == 0))
                                            {
                                                break;
                                            }
                                            if (!curMat.equals(Material.STONE) || groundLayers == null)
                                            {
                                                if (groundLayers == null || layerIndex == groundLayers.length)
                                                {
                                                    if (chunk.getBlock(x, y + 4, z).getType().equals(Material.STONE))
                                                    {
                                                        groundLayers = new GroundLayer[]{new GroundLayer(3, Material.GRAVEL)};
                                                    }
                                                    else if (newBiome == Biome.DESERT)
                                                    {
                                                        groundLayers = new GroundLayer[]{new GroundLayer(3, Material.SAND), new GroundLayer(2, Material.SANDSTONE)};
                                                    }
                                                    else
                                                    {
                                                        groundLayers = new GroundLayer[]{new GroundLayer(1, Material.GRASS), new GroundLayer(3, Material.DIRT)};
                                                    }
                                                }
                                                layerIndex = -1;
                                            }
                                            else
                                            {
                                                if (layerIndex < 0)
                                                {
                                                    layerIndex = 0;
                                                }
                                                else if (layerIndex < groundLayers.length && !groundLayers[layerIndex].hasRemainingBlocks())
                                                {
                                                    layerIndex++;
                                                }

                                                if (layerIndex < groundLayers.length) //&& chunk.getBlock(x, y, z).getLightLevel() > -1)
                                                {
                                                    Material matHere = groundLayers[layerIndex].takeBlock();
                                                    float randyFloat = handyRandy.nextFloat();
                                                    short yFlower = (short) (y + 1);
                                                    if (randyFloat < grassPercent && yFlower < 0xFF && WorldGenerationTools.getMaterial(chunk, x, yFlower, z).equals(Material.AIR)) {
                                                        randyFloat /= grassPercent;
                                                        switch (matHere)
                                                        {
                                                            case GRASS:
                                                                float deadBushSqueezer = precipitationFactor < .333f ? (.9f + .1f * (precipitationFactor / .333f)) : 1f;
                                                                if (randyFloat > deadBushSqueezer)
                                                                {
                                                                    WorldGenerationTools.setMaterial(chunk, x, yFlower, z, (short) 32); //deadbush
                                                                }
                                                                else
                                                                {
                                                                    switch (newBiome)
                                                                    {
                                                                        case TAIGA:
                                                                            if (randyFloat < 0.90f * deadBushSqueezer)
                                                                            {
                                                                                WorldGenerationTools.setMaterial(chunk, x, yFlower, z, (short) 31, (byte) 1); //tallgrass
                                                                            }
                                                                            else if (randyFloat < .98f * deadBushSqueezer)
                                                                            {
                                                                                WorldGenerationTools.setMaterial(chunk, x, yFlower, z, (short) 31, (byte) 2); //fern
                                                                            }
                                                                            else if (randyFloat < .993f * deadBushSqueezer)
                                                                            {
                                                                                WorldGenerationTools.setMaterial(chunk, x, yFlower, z, (short) 37); //dandelion
                                                                            }
                                                                            else
                                                                            {
                                                                                WorldGenerationTools.setMaterial(chunk, x, yFlower, z, (short) 38); //rose
                                                                            }
                                                                            break;
                                                                        case TAIGA_COLD:
                                                                            if (randyFloat < 0.96f * deadBushSqueezer)
                                                                            {
                                                                                me.happyman.WorldGenerationTools.setMaterial(chunk, x, yFlower, z, (short) 31, (byte) 1); //tallgrass
                                                                            }
                                                                            else if (randyFloat < 0.985f * deadBushSqueezer)
                                                                            {
                                                                                WorldGenerationTools.setMaterial(chunk, x, yFlower, z, (short) 37); //dandelion
                                                                            }
                                                                            else
                                                                            {
                                                                                WorldGenerationTools.setMaterial(chunk, x, yFlower, z, (short) 38); //rose
                                                                            }
                                                                            break;
                                                                        default:
                                                                            if (randyFloat < 0.97f * deadBushSqueezer)
                                                                            {
                                                                                WorldGenerationTools.setMaterial(chunk, x, yFlower, z, (short) 31, (byte) 1); //tallgrass
                                                                            }
                                                                            else if (randyFloat < 0.99f * deadBushSqueezer)
                                                                            {
                                                                                WorldGenerationTools.setMaterial(chunk, x, yFlower, z, (short) 37); //dandelion
                                                                            }
                                                                            else
                                                                            {
                                                                                WorldGenerationTools.setMaterial(chunk, x, yFlower, z, (short) 38); //rose
                                                                            }
                                                                            break;
                                                                    }
                                                                    break;
                                                                }
                                                            case SAND:
                                                                int cactusHeight = (int) randyFloat * MAX_CACTUS_HEIGHT + 1;

                                                                if (cactusHeight <= 1)
                                                                {
                                                                    WorldGenerationTools.setMaterial(chunk, x, yFlower, z, (short) 32); //deadbush
                                                                }
                                                                else
                                                                {
                                                                    for (short yCactus = yFlower; yCactus < yFlower + cactusHeight && yCactus < 0x100; yCactus++)
                                                                    {
                                                                        if (!WorldGenerationTools.getMaterial(chunk, x, yCactus, z).equals(Material.AIR))
                                                                        {
                                                                            break;
                                                                        }
                                                                        WorldGenerationTools.setMaterial(chunk, x, yCactus, z, (short) 81); //cactus
                                                                    }
                                                                }
                                                                break;
                                                        }
                                                    }

                                                    WorldGenerationTools.setMaterial(chunk, x, y, z, matHere);
                                                }
                                            }
                                        }
                                    }

                                    if (handyRandy.nextFloat() < Tree.getTreeDensity(precipitationFactor, temperatureFactor)) //There's a tree
                                    {
                                        //put in some trees
                                        byte height = Tree.getTreeHeight(precipitationFactor, temperatureFactor);

                                        float shrubFactor = (precipitationFactor - 0.3333f) / 0.6666f;
                                        if (shrubFactor < 0f) {
                                            shrubFactor = 0f;
                                        }

                                        new Tree(height, world, absX, highestBlockY, absZ, 0.499f/*treeHeightPrecise*.0824f*/, 0.499f/*treeHeightPrecise*.0588f*/).create();
                                    }
                                }
                            }

                            //add some ores
                            byte xInc = 2;
                            byte yInc = 2;
                            byte zInc = 2;
                            for (byte x = 1; x < 0x10; x += xInc)
                            {
                                for (byte z = 1; z < 0x10; z += zInc)
                                {
                                    int chanceMultiplier = xInc * yInc * zInc;
                                    for (short y = 1; y < 0x100; y += yInc)
                                    {
                                        float randomPercentageHere = handyRandy.nextFloat();
                                        float percentageToBeUnder = 0;

                                        for (OreVein vein : ORE_VEIN_ATTRIBUTES)
                                        {
                                            percentageToBeUnder += vein.percent * chanceMultiplier;
                                            if (randomPercentageHere <= percentageToBeUnder)
                                            {
                                                if (y < vein.upperBound && y >= vein.lowerBound && handyRandy.nextInt(vein.avgVeinSize) == 0)
                                                {
                                                    vein.create(seed, chunk, x, y, z);
                                                }
                                                break;
                                            }
                                        }
                                    }
                                }
                            }

                            //possibly add a hermit
                            short heightInMiddle = data.highestBlockYs[HERMIT_HUT_DOOR_X][HERMIT_HUT_DOOR_Z];
                            if (isVillagerChunk(chunk, heightInMiddle))
                            {
                                short bottomBound = heightInMiddle;
                                short ceilingTopBound = (short) (bottomBound + HERMIT_HUT_TOTAL_HEIGHT / 2);
                                short roofTopBound = (short) (bottomBound + HERMIT_HUT_TOTAL_HEIGHT);

                                //protect the chunk
                                WorldGenerationTools.setChunkProtectionLevel(chunk, WorldGenerationTools.ChunkProtectionLevel.DISABLE_EVERYTHING);

                                //Bukkit.broadcastMessage("" + chunkXAbs + ", " + chunkZAbs);

                                if (HERMIT_HUT_NORTH_BOUND >= HERMIT_HUT_BLOCK_BUFFER && HERMIT_HUT_SOUTH_BOUND < 0x10 - HERMIT_HUT_BLOCK_BUFFER &&
                                        HERMIT_HUT_WEST_BOUND >= HERMIT_HUT_BLOCK_BUFFER && HERMIT_HUT_EAST_BOUND < 0x10 - HERMIT_HUT_BLOCK_BUFFER)
                                {
                                    //outside
                                    for (short y = ceilingTopBound; y > bottomBound; y--)
                                    {
                                        boolean goingPositiveDirection = false;
                                        for (byte z = (byte) (HERMIT_HUT_NORTH_BOUND - 1); z <= HERMIT_HUT_SOUTH_BOUND + 1; z += HERMIT_HUT_SOUTH_BOUND - HERMIT_HUT_NORTH_BOUND + 2, goingPositiveDirection = true)
                                        {
                                            for (byte zOffset = 0; zOffset < HERMIT_HUT_BLOCK_BUFFER; zOffset++)//go through so that there is "more than 1" space for the hut
                                            {
                                                byte actualZ = (byte) (goingPositiveDirection ? z + zOffset : z - zOffset);
                                                for (byte x = (byte) (HERMIT_HUT_WEST_BOUND - 1); x <= HERMIT_HUT_EAST_BOUND + 1; x++)
                                                {
                                                    WorldGenerationTools.setMaterial(chunk, x, y, actualZ, Material.AIR);
                                                }
                                            }
                                        }

                                        goingPositiveDirection = false;
                                        for (byte x = (byte) (HERMIT_HUT_WEST_BOUND - 1); x <= HERMIT_HUT_EAST_BOUND + 1; x += HERMIT_HUT_EAST_BOUND - HERMIT_HUT_WEST_BOUND + 2, goingPositiveDirection = true)
                                        {
                                            for (byte xOffset = 0; xOffset < HERMIT_HUT_BLOCK_BUFFER; xOffset++)
                                            {
                                                byte actualX = (byte) (goingPositiveDirection ? x + xOffset : x - xOffset);
                                                for (byte z = (byte) (HERMIT_HUT_NORTH_BOUND - 1); z <= HERMIT_HUT_SOUTH_BOUND + 1; z++)
                                                {
                                                    WorldGenerationTools.setMaterial(chunk, actualX, y, z, Material.AIR);
                                                }
                                            }
                                        }
                                    }

                                    //floor, inner roof, and inside
                                    for (byte x = HERMIT_HUT_WEST_BOUND; x <= HERMIT_HUT_EAST_BOUND; x++)
                                    {
                                        for (byte z = HERMIT_HUT_NORTH_BOUND; z <= HERMIT_HUT_SOUTH_BOUND; z++)
                                        {
                                            //inside
                                            for (short y = (short) (ceilingTopBound - 1); y > bottomBound; y--)
                                            {
                                                WorldGenerationTools.setMaterial(chunk, x, y, z, Material.AIR);
                                            }

                                            //floor & inner roof
                                            WorldGenerationTools.setMaterial(chunk, x, bottomBound, z, HERMIT_HUT_FLOOR_MATERIAL, HERMIT_HUT_WOOD_SUBTYPE);
                                            WorldGenerationTools.setMaterial(chunk, x, ceilingTopBound, z, HERMIT_HUT_ROOF_HIDDEN_MATERIAL, HERMIT_HUT_WOOD_SUBTYPE);

                                        }
                                    }

                                    //walls
                                    for (short y = (short) (bottomBound + 1); y <= ceilingTopBound; y++)
                                    {
                                        //north and south walls
                                        for (byte x = (byte) (HERMIT_HUT_WEST_BOUND + 1); x < HERMIT_HUT_EAST_BOUND; x++)
                                        {
                                            for (byte z = HERMIT_HUT_NORTH_BOUND; z <= HERMIT_HUT_SOUTH_BOUND; z += HERMIT_HUT_SOUTH_BOUND - HERMIT_HUT_NORTH_BOUND)
                                            {
                                                WorldGenerationTools.setMaterial(chunk, x, y, z, HERMIT_HUT_WALL_MATERIAL);
                                            }
                                        }

                                        //east and west walls
                                        for (byte z = (byte) (HERMIT_HUT_NORTH_BOUND + 1); z < HERMIT_HUT_SOUTH_BOUND; z++)
                                        {
                                            for (byte x = HERMIT_HUT_WEST_BOUND; x <= HERMIT_HUT_EAST_BOUND; x += HERMIT_HUT_EAST_BOUND - HERMIT_HUT_WEST_BOUND)
                                            {
                                                WorldGenerationTools.setMaterial(chunk, x, y, z, HERMIT_HUT_WALL_MATERIAL);
                                            }
                                        }

                                    }

                                    //corner pillars
                                    for (byte x = HERMIT_HUT_WEST_BOUND; x <= HERMIT_HUT_EAST_BOUND; x += HERMIT_HUT_EAST_BOUND - HERMIT_HUT_WEST_BOUND)
                                    {
                                        for (byte z = HERMIT_HUT_NORTH_BOUND; z <= HERMIT_HUT_SOUTH_BOUND; z += HERMIT_HUT_SOUTH_BOUND - HERMIT_HUT_NORTH_BOUND)
                                        {
                                            short curHighestY = data.highestBlockYs[x][z];
                                            for (short y = ceilingTopBound; y >= bottomBound || y >= curHighestY; y--)
                                            {
                                                WorldGenerationTools.setMaterial(chunk, x, y, z, HERMIT_HUT_PILLAR_MATERIAL, HERMIT_HUT_WOOD_SUBTYPE);
                                            }
                                        }
                                    }


                                    //roof
                                    short y = (short) (ceilingTopBound - 1);
                                    for (byte north = (byte) (HERMIT_HUT_NORTH_BOUND - 1), south = (byte) (HERMIT_HUT_SOUTH_BOUND + 1); y <= roofTopBound && north <= south; y++, north++, south--)
                                    {
                                        for (byte x = HERMIT_HUT_WEST_BOUND; x <= HERMIT_HUT_EAST_BOUND; x++)
                                        {
                                            for (short smallY = (short) (y - 1); smallY > ceilingTopBound; smallY--)
                                            {
                                                if (WorldGenerationTools.getMaterial(chunk, x, smallY, north).equals(Material.AIR))
                                                {
                                                    WorldGenerationTools.setMaterial(chunk, x, smallY, north, HERMIT_HUT_ROOF_HIDDEN_MATERIAL, HERMIT_HUT_WOOD_SUBTYPE);
                                                }
                                                if (WorldGenerationTools.getMaterial(chunk, x, smallY, south).equals(Material.AIR))
                                                {
                                                    WorldGenerationTools.setMaterial(chunk, x, smallY, south, HERMIT_HUT_ROOF_HIDDEN_MATERIAL, HERMIT_HUT_WOOD_SUBTYPE);
                                                }
                                            }
                                            WorldGenerationTools.setMaterial(chunk, x, y, north, HERMIT_HUT_STAIRS_MATERIAL, (byte) 2);
                                            WorldGenerationTools.setMaterial(chunk, x, y, south, HERMIT_HUT_STAIRS_MATERIAL, (byte) 3);
                                        }
                                    }

                                    //door
                                    short topOfDoor = (short)(bottomBound + 2);
                                    short bottomOfDoor = (short)(topOfDoor - 1);
                                    short baseOfDoor = (short)(bottomOfDoor - 1);

                                    WorldGenerationTools.setMaterial(chunk, HERMIT_HUT_WEST_BOUND_OF_DOOR, topOfDoor, HERMIT_HUT_SOUTH_BOUND, Material.WOOD_DOOR);
                                    WorldGenerationTools.setMaterial(chunk, HERMIT_HUT_WEST_BOUND_OF_DOOR, bottomOfDoor, HERMIT_HUT_SOUTH_BOUND, Material.WOOD_DOOR);
                                    WorldGenerationTools.setMaterial(chunk, HERMIT_HUT_WEST_BOUND_OF_DOOR, baseOfDoor, HERMIT_HUT_SOUTH_BOUND, HERMIT_HUT_FLOOR_MATERIAL, HERMIT_HUT_WOOD_SUBTYPE);
                                    if (!WorldGenerationTools.getMaterial(chunk, HERMIT_HUT_WEST_BOUND_OF_DOOR, baseOfDoor, (byte) (HERMIT_HUT_SOUTH_BOUND + 1)).isSolid())
                                    {
                                        WorldGenerationTools.setMaterial(chunk, HERMIT_HUT_WEST_BOUND_OF_DOOR, baseOfDoor, (byte) (HERMIT_HUT_SOUTH_BOUND + 1), HERMIT_HUT_STAIRS_MATERIAL, HERMIT_HUT_ENTRANCE_STAIRS_SUBTYPE);
                                    }
                                    if (HERMIT_HUT_EAST_BOUND_OF_DOOR != HERMIT_HUT_WEST_BOUND_OF_DOOR) //even (cause difference is odd)
                                    {
                                        WorldGenerationTools.setMaterial(chunk, HERMIT_HUT_EAST_BOUND_OF_DOOR, topOfDoor, HERMIT_HUT_SOUTH_BOUND, Material.WOOD_DOOR);
                                        WorldGenerationTools.setMaterial(chunk, HERMIT_HUT_EAST_BOUND_OF_DOOR, bottomOfDoor, HERMIT_HUT_SOUTH_BOUND, Material.WOOD_DOOR);
                                        WorldGenerationTools.setMaterial(chunk, HERMIT_HUT_EAST_BOUND_OF_DOOR, baseOfDoor, HERMIT_HUT_SOUTH_BOUND, HERMIT_HUT_FLOOR_MATERIAL, HERMIT_HUT_WOOD_SUBTYPE);
                                        if (!WorldGenerationTools.getMaterial(chunk, HERMIT_HUT_EAST_BOUND_OF_DOOR, baseOfDoor, (byte) (HERMIT_HUT_SOUTH_BOUND + 1)).isSolid())
                                        {
                                            WorldGenerationTools.setMaterial(chunk, HERMIT_HUT_EAST_BOUND_OF_DOOR, baseOfDoor, (byte) (HERMIT_HUT_SOUTH_BOUND + 1), HERMIT_HUT_STAIRS_MATERIAL, HERMIT_HUT_ENTRANCE_STAIRS_SUBTYPE);
                                        }
                                    }

                                    //windows
                                    byte northSideOfWindow = (byte) (HERMIT_HUT_NORTH_BOUND + (HERMIT_HUT_SOUTH_BOUND - HERMIT_HUT_NORTH_BOUND) / 2);
                                    short topOfWindow = (short) (topOfDoor + 1);
                                    short bottomOfWindow = (short) (bottomOfDoor + 1);
                                    WorldGenerationTools.setMaterial(chunk, HERMIT_HUT_WEST_BOUND, bottomOfWindow, northSideOfWindow, Material.THIN_GLASS);
                                    WorldGenerationTools.setMaterial(chunk, HERMIT_HUT_WEST_BOUND, topOfWindow, northSideOfWindow, Material.THIN_GLASS);
                                    WorldGenerationTools.setMaterial(chunk, HERMIT_HUT_EAST_BOUND, bottomOfWindow, northSideOfWindow, Material.THIN_GLASS);
                                    WorldGenerationTools.setMaterial(chunk, HERMIT_HUT_EAST_BOUND, topOfWindow, northSideOfWindow, Material.THIN_GLASS);
                                    if ((HERMIT_HUT_SOUTH_BOUND - HERMIT_HUT_NORTH_BOUND) % 2 == 1)
                                    {
                                        byte southSideOfWindow = (byte) (northSideOfWindow + 1);

                                        WorldGenerationTools.setMaterial(chunk, HERMIT_HUT_WEST_BOUND, bottomOfWindow, southSideOfWindow, Material.THIN_GLASS);
                                        WorldGenerationTools.setMaterial(chunk, HERMIT_HUT_WEST_BOUND, topOfWindow, southSideOfWindow, Material.THIN_GLASS);
                                        WorldGenerationTools.setMaterial(chunk, HERMIT_HUT_EAST_BOUND, bottomOfWindow, southSideOfWindow, Material.THIN_GLASS);
                                        WorldGenerationTools.setMaterial(chunk, HERMIT_HUT_EAST_BOUND, topOfWindow, southSideOfWindow, Material.THIN_GLASS);
                                    }

                                    //shop area
                                    for (byte x = (byte) (HERMIT_HUT_WEST_BOUND + 1); x <= HERMIT_HUT_EAST_BOUND - 1; x++)
                                    {
                                        WorldGenerationTools.setMaterial(chunk, x, (short) (bottomBound + 1), (byte) ((HERMIT_HUT_NORTH_BOUND + HERMIT_HUT_SOUTH_BOUND) / 2), (short) 85);
                                    }

                                    //random mystery sign
                                    handyRandy.setSeed(chunkXAbs + chunkZAbs*10);
                                    String[] hexValues = WorldMysteries.getMysteryEntryHexValues();
                                    final int keyOnSign = handyRandy.nextInt(hexValues.length);
                                    final String valueOnSign = hexValues[keyOnSign];
                                    final byte signX = (byte)(HERMIT_HUT_EAST_BOUND - 1);
                                    final short signY = (short)(bottomBound + 1);
                                    final byte signZ = (byte)(HERMIT_HUT_NORTH_BOUND - 1);
                                    chunk.getBlock(signX, signY, signZ).setType(Material.WALL_SIGN);
                                    Bukkit.getScheduler().callSyncMethod(MetaWorldGeneratorMain.getInstance(), new Callable()
                                    {
                                        public String call()
                                        {
                                            Sign sign = (Sign)chunk.getBlock(signX, signY, signZ).getState();
                                            sign.setLine(1, "§1§l" + keyOnSign + " " + valueOnSign);
                                            sign.update();
                                            return "";
                                        }
                                    });

                                    //shop keeper
                                    Villager villager = (Villager) world.spawnEntity(new Location(world, 0.5f + chunkXAbs + (float) (HERMIT_HUT_EAST_BOUND + HERMIT_HUT_WEST_BOUND) / 2, bottomBound + 1, 0.5f + chunkZAbs + (HERMIT_HUT_NORTH_BOUND + HERMIT_HUT_SOUTH_BOUND) / 2 - 1), EntityType.VILLAGER);
                                    villager.setCustomName(" " + KIT_SELLER_HERMIT_NAME);
                                    villager.setProfession(KIT_SELLER_HERMIT_PROFESSION);
                                }
                            }
                        }
                    }
                    catch (IllegalAccessError error)
                    {
                        error.printStackTrace();
                    }
                }
            });
            //                    float howFarWeCanGoOutside = 1.2f;
            //                    float minDiameter = 0.2f;
            //                    float maxDiameter = 0.45f;
            //                    for (byte i = 0; i < OCEAN_POINTS.length; i++)
            //                    {
            //                        OCEAN_POINTS[i] = new OceanPoint(handyRandy.nextFloat()*howFarWeCanGoOutside, handyRandy.nextFloat()*howFarWeCanGoOutside,
            //                                handyRandy.nextFloat()*(maxDiameter - minDiameter) + minDiameter);
            //                    }
        }
    }

    //            @Override
    //            public byte[][] generateBlockSections(World world, Random randNotForUse, int chunkX, int chunkZ, BiomeGrid biome)
    //            {
    //                return new byte[world.getMaxHeight()/0x10][];
    //            }

    @Override
    public byte[][] generateBlockSections(World world, Random randNotForUse, int chunkX, int chunkZ, BiomeGrid biome)
    {
        //3817667513105396968L;
        int maxHeight = world.getMaxHeight();
        byte[][] chunk = new byte[maxHeight/0x10][];

        try
        {
            int chunkXAbs = chunkX * 0x10;
            int chunkZAbs = chunkZ * 0x10;

            handyRandy.setSeed(seed + chunkX * 240 + chunkZ * 240402);

            BinarySquareInterpolator[] heightInterpolators1 = new BinarySquareInterpolator[]
                    {
                            new BinarySquareInterpolator(seed, (byte) 11, chunkXAbs, chunkZAbs),
                            new BinarySquareInterpolator(seed, (byte)10, chunkXAbs, chunkZAbs),
                            new BinarySquareInterpolator(seed, (byte) 9, chunkXAbs, chunkZAbs),
                            //new BinarySquareInterpolator(seed, (byte)8, chunkXAbs, chunkZAbs),
                            new BinarySquareInterpolator(seed, (byte) 7, chunkXAbs, chunkZAbs),
                            //new BinarySquareInterpolator(seed, (byte)6, chunkXAbs, chunkZAbs),
                            new BinarySquareInterpolator(seed, (byte) 5, chunkXAbs, chunkZAbs),
                            //new BinarySquareInterpolator(seed, (byte)4, chunkXAbs, chunkZAbs),
                            //new BinarySquareInterpolator(seed, (byte)3, chunkXAbs, chunkZAbs),
                            //new BinarySquareInterpolator(seed, (byte)2, chunkXAbs, chunkZAbs)
                    };

            BinarySquareInterpolator[] heightInterpolators2 = new BinarySquareInterpolator[]
                    {
                            //new BinarySquareInterpolator(seed2, (byte)12, chunkXAbs, chunkZAbs),
                            new BinarySquareInterpolator(seed2, (byte)11, chunkXAbs, chunkZAbs),
                            new BinarySquareInterpolator(seed2, (byte)10, chunkXAbs, chunkZAbs),
                            new BinarySquareInterpolator(seed2, (byte)9, chunkXAbs, chunkZAbs),
                            //new BinarySquareInterpolator(seed, (byte)8, chunkXAbs, chunkZAbs),
                            new BinarySquareInterpolator(seed2, (byte)7, chunkXAbs, chunkZAbs),
                            //new BinarySquareInterpolator(seed, (byte)6, chunkXAbs, chunkZAbs),
                            new BinarySquareInterpolator(seed2, (byte)5, chunkXAbs, chunkZAbs)
                            //new BinarySquareInterpolator(seed, (byte)4, chunkXAbs, chunkZAbs),
                            //new BinarySquareInterpolator(seed, (byte)2, chunkXAbs, chunkZAbs)
                    };

            //build the terrain
            BinaryCubeInterpolator[] defaultInterpolaters = BinaryCubeInterpolator.getInterpolators(seed, TERRAIN_INTERPOLATOR_POWERS, chunkXAbs, 0, chunkZAbs);

            for (short y = 0; y < maxHeight; y++)
            {
                float requiredDensity = -1;
                float percentHeight = (float) y / maxHeight;

                byte highMaxPower = 0;
                byte lowMaxPower = 0;
                float miniPercent = 0f;
                for (byte i = 0; i < DEFAULT_HEIGHT_ATTRIBUTES.length; i++)
                {
                    HeightAttribute haLow = DEFAULT_HEIGHT_ATTRIBUTES[i];
                    if (percentHeight >= haLow.height)
                    {
                        HeightAttribute haHigh = DEFAULT_HEIGHT_ATTRIBUTES[i - 1];

                        miniPercent = (percentHeight - haLow.height) / (haHigh.height - haLow.height);
                        requiredDensity = miniPercent * haHigh.densityRequirement + (1f - miniPercent) * haLow.densityRequirement;
                        highMaxPower = haHigh.interpolatorPowerLimit;
                        lowMaxPower = haLow.interpolatorPowerLimit;
                        break;
                    }
                }
                //This is pretty dope
                //                                float groundMin = 0.26f;
                //                                float reqGround = 0.1f;
                //                                float groundMax = 0.3f;
                //                                float reqSky = 0.7f;
                //
                //                                if (percentHeight < groundMin)
                //                                {
                //                                    requiredDensity = reqGround;
                //                                }
                //                                else if (percentHeight < groundMax)
                //                                {
                //                                    float miniPercent = (percentHeight - groundMin)/(groundMax - groundMin);
                //
                //                                    requiredDensity = reqGround*(1 - miniPercent) + reqSky*miniPercent;
                //                                }
                //                                else
                //                                {
                //                                    requiredDensity = reqSky;
                //                                }

                int absX = chunkXAbs;
                for (byte x = 0; x < 0x10; x++, absX++)
                {
                    int absZ = chunkZAbs;
                    for (byte z = 0; z < 0x10; z++, absZ++)
                    {
                        final float densityHere = highMaxPower != lowMaxPower ? BinaryCubeInterpolator.getDensity(defaultInterpolaters, absX, y, absZ, miniPercent, lowMaxPower, highMaxPower)
                                : BinaryCubeInterpolator.getDensity(defaultInterpolaters, absX, y, absZ, lowMaxPower);
                        if (densityHere >= requiredDensity)
                        {
                            WorldGenerationTools.setMaterial(chunk, x, y, z, (byte)1);
                        }
                        //                            if (biome.getBiome(getGlobalX, getGlobalZ).equals(Biome.FOREST))
                        //                            {
                        //
                        //                            }
                    }
                }
            }

            double[][] height1s = new double[0x10][0x10];
            double[][] height2s = new double[0x10][0x10];
            short[][] highestBlockYs = new short[0x10][0x10];
            //byte[][] highestBlockYsIn9 = new short[0x10*3][0x10*3];

            //                for (byte xIn9 = 0, xRelBase = (byte)-0x10; xIn9 < 0x20; xIn9++, xRelBase++)
            //                {
            //                    int absX = xRelBase + chunkXAbs;
            //                    for (byte zIn9 = 0, zRelBase = (byte)-0x10; zIn9 < 0x20; zIn9++, zRelBase++)
            //                    {
            //                        int absZ = zRelBase + chunkZAbs;
            //                        height1s9[xIn9][zIn9] = BinarySquareInterpolator.getHeight(heightInterpolators1, absX, absZ);
            //                        height2s9[xIn9][zIn9] = BinarySquareInterpolator.getHeight(heightInterpolators2, absX, absZ);
            //                        //highestBlockYsIn9[xIn9][zIn9] = (short)world.getHighestBlockYAt(absX, absZ);
            //                    }
            //                }

            for (byte x = 0; x < 0x10; x++)
            {
                int absX = x + chunkXAbs;
                for (byte z = 0; z < 0x10; z++)
                {
                    int absZ = z + chunkZAbs;
                    height1s[x][z] = BinarySquareInterpolator.getHeight(heightInterpolators1, absX, absZ);
                    height2s[x][z] = BinarySquareInterpolator.getHeight(heightInterpolators2, absX, absZ);
                    short highestBlockY;
                    for (highestBlockY = (short)(maxHeight - 1); highestBlockY > 0 && WorldGenerationTools.getMaterialId(chunk, x, highestBlockY, z) == (short)0; highestBlockY--);
                    highestBlockYs[x][z] = highestBlockY;
                    biome.setBiome(x, z, Biome.PLAINS); //do not remove this
                    WorldGenerationTools.setMaterial(chunk, x, (byte)0, z, (byte)7); //or this
                }
            }

            //add oceans and rivers
                /**/
            //                    Double ratioDistanceToNearest = null;
            //                    OceanPoint nearestOcean = null;
            //                    Double ratioDistanceToSecondNearest = null;
            //                    OceanPoint secondNearestOcean = null;
            //
            //                    for (byte i = 0; i < OCEAN_POINTS.length; i++)
            //                    {
            //                        double ratioDistance = Math.sqrt((chunkXAbs - OCEAN_POINTS[i].getGlobalX) * (chunkXAbs - OCEAN_POINTS[i].getGlobalX) + (chunkZAbs - OCEAN_POINTS[i].getGlobalZ) * (chunkZAbs - OCEAN_POINTS[i].getGlobalZ)) / OCEAN_POINTS[i].radius;
            //                        if (nearestOcean == null || ratioDistance < ratioDistanceToNearest)
            //                        {
            //                            secondNearestOcean = nearestOcean;
            //                            ratioDistanceToSecondNearest = ratioDistanceToNearest;
            //
            //                            nearestOcean = OCEAN_POINTS[i];
            //                            ratioDistanceToNearest = ratioDistance;
            //                        }
            //                        else if (secondNearestOcean == null || ratioDistance < ratioDistanceToSecondNearest)
            //                        {
            //                            secondNearestOcean = OCEAN_POINTS[i];
            //                            ratioDistanceToSecondNearest = ratioDistance;
            //                        }
            //                    }

            //add oceans, rivers, and creeks
            //for (byte xRelBase = 0, xIn9 = (byte)0x10; xRelBase < 0x10; xIn9++, xRelBase++)
            for (byte x = 0; x < 0x10; x++)
            {
                int absX = x + chunkXAbs;
                //for (byte zRelBase = 0, zIn9 = (byte)0x10; zRelBase < 0x10; zIn9++, zRelBase++)
                for (byte z = 0; z < 0x10; z++)
                {
                    int absZ = z + chunkZAbs;
                    double height1 = height1s[x][z];
                    double height2 = height2s[x][z];

                    short oceanDepth = 0;
                    short riverDepth = 0;
                    short creekDepth = 0;
                    float sandOnBeach = SAND_PER_BEACH;
                    Double beachHeightCapAlpha = null;

                    if (height1 < OCEAN_BELOW_HEIGHT*(1d - OCEAN_RATIO_OF_FALLOFF))
                    {
                        oceanDepth = OCEAN_DEPTH;
                    }
                    else if (height1 < OCEAN_BELOW_HEIGHT) //transition from ocean to land
                    {
                        final short depth = (short)((OCEAN_BELOW_HEIGHT - height1) / (OCEAN_BELOW_HEIGHT*OCEAN_RATIO_OF_FALLOFF) * OCEAN_DEPTH);
                        oceanDepth = depth > 0 ? depth : 1;
                    }
                    else if (height1 <= OCEAN_BELOW_HEIGHT*(1d + OCEAN_RATIO_OF_FALLOFF_BEACH))
                    {
                        beachHeightCapAlpha = (height1 - OCEAN_BELOW_HEIGHT) / (OCEAN_BELOW_HEIGHT*OCEAN_RATIO_OF_FALLOFF_BEACH);
                    }

                    if (oceanDepth != OCEAN_DEPTH)
                    {
                        //river code
                        if (height2 >= RIVER_AT_HEIGHT_MIN && height2 <= RIVER_AT_HEIGHT_MAX)
                        {
                            final short depth;
                            if (height2 <= RIVER_AT_HEIGHT_MIN + RIVER_AT_HEIGHT_FALLOFF_RANGE)
                            {
                                depth = (short) ((height2 - RIVER_AT_HEIGHT_MIN) / RIVER_AT_HEIGHT_FALLOFF_RANGE * RIVER_DEPTH);
                            }
                            else if (height2 >= RIVER_AT_HEIGHT_MAX - RIVER_AT_HEIGHT_FALLOFF_RANGE)
                            {
                                depth = (short) ((RIVER_AT_HEIGHT_MAX - height2) / RIVER_AT_HEIGHT_FALLOFF_RANGE * RIVER_DEPTH);
                            }
                            else
                            {
                                depth = RIVER_DEPTH;
                            }
                            riverDepth = depth > 0 ? depth : 1;
                        }
                        else if (height2 > RIVER_AT_HEIGHT_MIN - RIVER_AT_HEIGHT_FALLOFF_RANGE_BEACH && height2 < RIVER_AT_HEIGHT_MAX + RIVER_AT_HEIGHT_FALLOFF_RANGE_BEACH)
                        {
                            final double newAlpha;
                            if (height2 < RIVER_AT_HEIGHT_MIN)
                            {
                                newAlpha = (RIVER_AT_HEIGHT_MIN - height2) / RIVER_AT_HEIGHT_FALLOFF_RANGE_BEACH;
                            }
                            else
                            {
                                newAlpha = (height2 - RIVER_AT_HEIGHT_MAX) / RIVER_AT_HEIGHT_FALLOFF_RANGE_BEACH;
                            }
                            if (beachHeightCapAlpha == null || newAlpha < beachHeightCapAlpha)
                            {
                                beachHeightCapAlpha = newAlpha;
                            }
                        }

                        if (riverDepth != RIVER_DEPTH)
                        {//creek code
                            if (height2 >= CREEK_AT_HEIGHT_MIN && height2 <= CREEK_AT_HEIGHT_MAX)
                            {
                                final short depth;
                                if (height2 <= CREEK_AT_HEIGHT_MIN + CREEK_AT_HEIGHT_FALLOFF_RANGE)
                                {
                                    depth = (short)((height2 - CREEK_AT_HEIGHT_MIN) / CREEK_AT_HEIGHT_FALLOFF_RANGE * CREEK_DEPTH);
                                }
                                else if (height2 >= CREEK_AT_HEIGHT_MAX - CREEK_AT_HEIGHT_FALLOFF_RANGE)
                                {
                                    depth = (short)((CREEK_AT_HEIGHT_MAX - height2) / CREEK_AT_HEIGHT_FALLOFF_RANGE * CREEK_DEPTH);
                                }
                                else
                                {
                                    depth = CREEK_DEPTH;
                                }
                                creekDepth = depth > 0 ? depth : 1;
                            }
                            else if (height2 > CREEK_AT_HEIGHT_MIN - CREEK_AT_HEIGHT_FALLOFF_RANGE_BEACH && height2 < CREEK_AT_HEIGHT_MAX + CREEK_AT_HEIGHT_FALLOFF_RANGE_BEACH)
                            {
                                sandOnBeach = 0;
                                final double newAlpha;
                                if (height2 < CREEK_AT_HEIGHT_MIN)
                                {
                                    newAlpha = (CREEK_AT_HEIGHT_MIN - height2) / CREEK_AT_HEIGHT_FALLOFF_RANGE_BEACH;
                                }
                                else
                                {
                                    newAlpha = (height2 - CREEK_AT_HEIGHT_MAX) / CREEK_AT_HEIGHT_FALLOFF_RANGE_BEACH;
                                }
                                if (beachHeightCapAlpha == null || newAlpha < beachHeightCapAlpha)
                                {
                                    beachHeightCapAlpha = newAlpha;
                                }
                            }
                        }
                    }

                    final short totalDepth = (short)(oceanDepth + riverDepth + creekDepth);

                    //alpha is fine, the problem is here
                    short highestBlockY = highestBlockYs[x][z];

                    boolean sandHere = false;
                    if (beachHeightCapAlpha != null)
                    {
                        BinarySquareInterpolator beachInterpolator = new BinarySquareInterpolator(seed, (byte)4, absX, absZ);
                        short blockThreshold = (short) (beachHeightCapAlpha * (maxHeight - SEA_LEVEL) * (1d + beachInterpolator.getHeight(absX, absZ) * 0.2));

                        if (highestBlockY > SEA_LEVEL + blockThreshold)
                        {
                            for (short y = highestBlockY; y > SEA_LEVEL + blockThreshold; y--)
                            {
                                WorldGenerationTools.setMaterial(chunk, x, y, z, (byte)0);
                            }
                        }
                        else if (highestBlockY < SEA_LEVEL - blockThreshold)
                        {
                            for (short y = (short) (highestBlockY + 1); y <= SEA_LEVEL - blockThreshold; y++)
                            {
                                WorldGenerationTools.setMaterial(chunk, x, y, z, (byte)1);
                            }
                        }

                        for (highestBlockY = (short)(maxHeight - 1); highestBlockY > 0 && WorldGenerationTools.getMaterialId(chunk, x, highestBlockY, z) != (short)1; highestBlockY--);

                        if (beachHeightCapAlpha < sandOnBeach && highestBlockY < SEA_LEVEL + 6 && highestBlockY > SEA_LEVEL - 20)
                        {
                            biome.setBiome(x, z, Biome.BEACHES);
                            if (beachHeightCapAlpha < sandOnBeach * PERCENT_OF_PURE_SAND_ON_BEACH ||
                                    handyRandy.nextFloat() > (float) (beachHeightCapAlpha / sandOnBeach - PERCENT_OF_PURE_SAND_ON_BEACH) / (1f - PERCENT_OF_PURE_SAND_ON_BEACH))
                            {
                                sandHere = true;
                            }
                        }
                    }

                    if (totalDepth > 0)
                    {
                        if (oceanDepth > 0)
                        {
                            biome.setBiome(x, z, Biome.OCEAN);
                            if (!sandHere && oceanDepth < OCEAN_SAND_MAX_DEPTH)
                            {
                                float percentDepth = (float)oceanDepth/OCEAN_SAND_MAX_DEPTH;
                                sandHere = percentDepth < PERCENT_OF_PURE_SAND_ON_BEACH || handyRandy.nextFloat() < (1f - percentDepth)/(1f - PERCENT_OF_PURE_SAND_ON_BEACH);
                            }
                        }
                        else if (riverDepth > 0)
                        {
                            biome.setBiome(x, z, Biome.RIVER);
                            if (!sandHere && riverDepth < RIVER_SAND_MAX_DEPTH)
                            {
                                float percentDepth = (float)riverDepth/RIVER_SAND_MAX_DEPTH;
                                sandHere = percentDepth < PERCENT_OF_PURE_SAND_ON_BEACH || handyRandy.nextFloat() < (1f - percentDepth)/(1f - PERCENT_OF_PURE_SAND_ON_BEACH);
                            }
                        }

                        for (short y = (short) (maxHeight - 1); y > SEA_LEVEL; y--)
                        {
                            WorldGenerationTools.setMaterial(chunk, x, y, z, (byte)0);
                        }

                        for (byte findY = SEA_LEVEL; findY > 0; findY--)
                        {
                            if (WorldGenerationTools.getMaterialId(chunk, x, findY, z) != 0)
                            {
                                for (short y = findY; y > 0 && y > findY - totalDepth; y--)
                                {
                                    WorldGenerationTools.setMaterial(chunk, x, y, z, (byte)0);
                                }
                                break;
                            }
                        }

                        for (byte y = SEA_LEVEL; y > 0; y--)
                        {
                            short matId = WorldGenerationTools.getMaterialId(chunk, x, y, z);
                            if (matId != 8 && matId != 9 && matId != 0)
                            {
                                break;
                            }

                            WorldGenerationTools.setMaterial(chunk, x, y, z, (byte) 9);
                        }
                    }

                    if (sandHere)
                    {
                        for (highestBlockY = (short)(maxHeight - 1); highestBlockY > 0 && WorldGenerationTools.getMaterialId(chunk, x, highestBlockY, z) != (short)1; highestBlockY--);
                        for (short y = highestBlockY; y >= highestBlockY - 5 && y > 0; y--)
                        {
                            WorldGenerationTools.setMaterial(chunk, x, y, z, (byte)12);
                        }
                    }

                    highestBlockYs[x][z] = WorldGenerationTools.getHighestBlockYInChunk(chunk, x, z);
                }
            }

            float[][] precipitationFactors = new float[0x10][0x10];
            float[][] temperatureFactors = new float[0x10][0x10];
            Biome[][] biomes = new Biome[0x10][0x10];

            //Determine precipitation & temperature & set biome accordingly
            for (byte z = 0; z < 0x10; z++)
            //for (byte zRelBase = -0x10, zIn9 = 0; zRelBase < 0x20; zIn9++, zRelBase++)
            {
                int absZ = z + chunkZAbs;

                final float simpleTempFactor;
                if (absZ < -HALF_WORLD_LENGTH)
                {
                    simpleTempFactor = 0;
                }
                else if (absZ > HALF_WORLD_LENGTH)
                {
                    simpleTempFactor = 1f;
                }
                else
                {
                    simpleTempFactor = (float)(absZ + HALF_WORLD_LENGTH) / WORLD_LENGTH;
                }

                for (byte x = 0; x < 0x10; x++)
                //for (short xRelBase = -0x10, xIn9 = 0; xRelBase < 0x20; xIn9++, xRelBase++)
                {
                    //                            float precipFactor = (float)height1s9[xIn9][zIn9];
                    //                            precipitationFactorsIn9[xIn9][zIn9] = precipFactor;
                    //                            float tempFactor = 0.8f*simpleTempFactor + 0.2f*(float)height2s9[xIn9][xIn9];
                    //                            temperatureFactorsIn9[xIn9][zIn9] = tempFactor;
                    precipitationFactors[x][z] = (float)height1s[x][z];
                    temperatureFactors[x][z] = 0.8f*simpleTempFactor + 0.2f*(float)height2s[x][z];
                    biomes[x][z] = biome.getBiome(x, z);
                }
            }

            //grass is in a populator

            //ores are in a populator

            //trees are in a populator

            WorldGenerationTools.ChunkProtectionLevel level = getWhatProtectionLevelChunkShouldBeBasedOnItsDistanceFromSpawn(chunkX, chunkZ);
            if (level != WorldGenerationTools.ChunkProtectionLevel.UNPROTECTED)
            {
                WorldGenerationTools.setChunkProtectionLevel(chunk, level);
                if (level == WorldGenerationTools.ChunkProtectionLevel.DISABLE_EVERYTHING)
                {
                    if (getWhatProtectionLevelChunkShouldBeBasedOnItsDistanceFromSpawn(chunkX + 1, chunkZ) != WorldGenerationTools.ChunkProtectionLevel.DISABLE_EVERYTHING)
                    {
                        byte x = 0xF;
                        for (byte z = 0; z < 0x10; z++)
                        {
                            short highestBlock = highestBlockYs[x][z];
                            WorldGenerationTools.setMaterial(chunk, x, highestBlock, z, (short)22);
                        }
                    }
                    if (getWhatProtectionLevelChunkShouldBeBasedOnItsDistanceFromSpawn(chunkX, chunkZ + 1) != WorldGenerationTools.ChunkProtectionLevel.DISABLE_EVERYTHING)
                    {
                        byte z = 0xF;
                        for (byte x = 0; x < 0x10; x++)
                        {
                            short highestBlock = highestBlockYs[x][z];
                            WorldGenerationTools.setMaterial(chunk, x, highestBlock, z, (short)22);
                        }
                    }
                    if (getWhatProtectionLevelChunkShouldBeBasedOnItsDistanceFromSpawn(chunkX - 1, chunkZ) != WorldGenerationTools.ChunkProtectionLevel.DISABLE_EVERYTHING)
                    {
                        byte x = 0x0;
                        for (byte z = 0; z < 0x10; z++)
                        {
                            short highestBlock = highestBlockYs[x][z];
                            WorldGenerationTools.setMaterial(chunk, x, highestBlock, z, (short)22);
                        }
                    }
                    if (getWhatProtectionLevelChunkShouldBeBasedOnItsDistanceFromSpawn(chunkX, chunkZ - 1) != WorldGenerationTools.ChunkProtectionLevel.DISABLE_EVERYTHING)
                    {
                        byte z = 0x0;
                        for (byte x = 0; x < 0x10; x++)
                        {
                            short highestBlock = highestBlockYs[x][z];
                            WorldGenerationTools.setMaterial(chunk, x, highestBlock, z, (short)22);
                        }
                    }
                }
            }

            setChunkData(chunkX, chunkZ, new MyChunkData(precipitationFactors, temperatureFactors, highestBlockYs, biomes));
        }
        catch (IllegalAccessError ex)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Illegal access error at " + chunkX*16 + ", " + chunkZ*16);
            //                    ex.setStackTrace(new StackTraceElement[] {});
            ex.printStackTrace();
        }
        return chunk;
    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(final World w)
    {
        return POPULATORS;
    }

    private static class Tree
    {
        private static final float MAX_TREE_DENSITY = 0.07f;
        private static final float TREE_TAKEUP_MOD = 15f;
        private static final float AVG_TREE_HEIGHT = 6.5f;
        private static final float TRUNK_LOWBRANCH_HEIGHT_PERCENT = 0.4f;
        private static final float RADIUS_OF_LEAF_PER_RADIUS_OF_TRUNK = 6f;
        private static final float PERCENT_OF_NO_TREES = 0.33f;

        private final byte height;
        private final World world;
        private final int globalX;
        private final short globalY;
        private final int globalZ;
        private final float maxTrunkRadius;
        private final float minTrunkRadius;
        private ArrayList<Tree.TreeBlock> usableTreeBlocks;
        private boolean canBeGrown;
        private int createdBlocks;

        private static float highestPrecip = 0;
        private static float lowestPrecip = 0;
        private static float hl = 0;
        private static float ll = 0;

        private static float getTreeDensity(float precipitationFactor, float temperatureFactor)
        {
            float lushness = (0.8f*precipitationFactor + 0.2f*(
                    precipitationFactor*temperatureFactor + (1f - precipitationFactor)*(temperatureFactor < 0.5f ? 0.5f - temperatureFactor : temperatureFactor - 0.5f)*2f)
            );
            lushness -= PERCENT_OF_NO_TREES;

            return (lushness < 0 ? 0 : lushness)* MAX_TREE_DENSITY;
        }

        private static byte getTreeHeight(float precipitationFactor, float temperatureFactor)
        {
            float treeHeightFactor = (precipitationFactor - 0.3333f)*(temperatureFactor - 0.3333f)/0.44444f;
            if (treeHeightFactor < 0f)
            {
                treeHeightFactor = 0f;
            }
            return (byte)((handyRandy.nextFloat()*0.1f + treeHeightFactor*0.7f + 0.6f) * AVG_TREE_HEIGHT);
        }

        public Tree(byte height, World world, int globalX, int grassY, int globalZ, float maxTrunkRadius, float minTrunkRadius)
        {
            this.height = height;
            this.world = world;
            this.globalX = globalX;
            this.globalZ = globalZ;
            this.maxTrunkRadius = maxTrunkRadius;
            this.minTrunkRadius = minTrunkRadius;

            this.globalY = (short)(this.maxTrunkRadius >= 0.5f ? grassY : grassY + 1);
            canBeGrown = this.globalY < 0x100 && this.globalY > 0 && world.getBlockAt(this.globalX, grassY, this.globalZ).getType().equals(Material.GRASS) /*&& c.getBlock(xInChunk, grassY + 1, zInChunk).getLightLevel() >= 8*/;
        }

        private void create()
        {
            if (canBeGrown)
            {
                canBeGrown = false;
                //root =
                createdBlocks = 0;
                usableTreeBlocks = new ArrayList<Tree.TreeBlock>();
                world.getBlockAt(globalX, globalY - 1, globalZ).setType(Material.DIRT);
                usableTreeBlocks.add(new Tree.TreeBlock(null, this, (byte)0, (byte)0, (byte)0, Tree.TreeBlock.TreeBlockType.TRUNK));

                int i = 0;
                int numberOfBlocks = (int)(TREE_TAKEUP_MOD*height);
                boolean timedOut = false;
                while (createdBlocks < numberOfBlocks)
                {
                    i++;
                    if (!expandTree())
                    {
                        break;
                    }
                    if (i > numberOfBlocks)
                    {
                        timedOut= true;
                        break;
                    }
                    //Bukkit.broadcastMessage("" + i);
                }
                if (timedOut)
                {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Took too freaking long to make a tree. Only " + createdBlocks + " created.");
                }
                //                        else if (i == 0)
                //                        {
                //                            //Bukkit.broadcastMessage("height: " + height + "#Blocks: " + numberOfBlocks);
                //                        }
                //                        else
                //                        {
                //                           //Bukkit.broadcastMessage("efficiency: " + ((float)i/numberOfBlocks));
                //                        }

                usableTreeBlocks = null;
                //                        needyBlocks = new ArrayList<TreeBlock>();
                //                        needyBlocks.add(new TreeBlock(null, this, (byte)0, (byte)0, (byte)0, TreeBlock.TreeBlockType.TRUNK));
                //                        allBlocks.add(root);
                //                        usableTreeBlocks.add(root);
                //                        int it = 0;
                //                        int grownBlocks = 0;
                //                        for (int numberOfBlocks = 30/*(int)(TREE_TAKEUP_MOD*height*height*height)*/; grownBlocks < numberOfBlocks;)
                //                        {
                //                            it++;
                //                            if (expandTree())
                //                            {
                //                                grownBlocks++;
                //                            }
                //                        }
                //Bukkit.broadcastMessage("efficiency: " + ((float)((int)(TREE_TAKEUP_MOD*height*height*height))/it));
            }
        }

        //                private ArrayList<TreeBlock> needyBlocks;
        //                private static final float FULL_NOURISHMENT = 1f;

        private boolean expandTree() //@TODO: Maybe change to algorithm that goes up from the root; a tracker will keep track of the direction of parent (has been tried, but not thoroughly)
        {
            if (usableTreeBlocks.size() == 0)
            {
                return false;
            }
            Tree.TreeBlock treeBlockToBranch = usableTreeBlocks.get(handyRandy.nextInt(usableTreeBlocks.size()));
            //Bukkit.broadcastMessage("" + treeBlockToBranch.type);

            float randomFloat = handyRandy.nextFloat();

            final byte xRelBase = treeBlockToBranch.xRelBase;
            final byte yRelBase = treeBlockToBranch.yRelBase;
            final byte zRelBase = treeBlockToBranch.zRelBase;

            float heightPercent = (float)yRelBase/height;
            float radiusOfTrunkHere = (1f - heightPercent)*maxTrunkRadius + heightPercent*minTrunkRadius;
            float sqRadiusOfTrunkHere = radiusOfTrunkHere*radiusOfTrunkHere;
            //                    final byte xCurRelBaseMag = xCurRelBase < 0 ? (byte)-xCurRelBase : xCurRelBase;
            //                    //final byte yCurRelBaseMag = yCurRelBase;
            //                    final byte zCurRelBaseMag = zCurRelBase < 0 ? (byte)-zCurRelBase : zCurRelBase;
            //                    final byte xRelParent = treeBlockToBranch.parent == null ? 0 : (byte)(xRelBase - treeBlockToBranch.parent.xRelBase);
            //                    final byte yRelParent = treeBlockToBranch.parent == null ? 1 : (byte)(yRelBase - treeBlockToBranch.parent.yRelBase);
            //                    final byte zRelParent = treeBlockToBranch.parent == null ? 0 : (byte)(zRelBase - treeBlockToBranch.parent.zRelBase);
            final short rSqRelBase = (short)(xRelBase*xRelBase + zRelBase*zRelBase);
            //final float xPortionOfXZSlope = (float)xSqRelBase/rSqRelBase;
            //                    int globalX = treeBlockToBranch.getGlobalX();
            //                    int globalY = treeBlockToBranch.getGlobalY();
            //                    int globalZ = treeBlockToBranch.getGlobalZ();
            Tree.TreeBlock.TreeBlockType typeEast = null;
            Tree.TreeBlock.TreeBlockType typeWest = null;
            Tree.TreeBlock.TreeBlockType typeUp = null;
            Tree.TreeBlock.TreeBlockType typeDown = null;
            Tree.TreeBlock.TreeBlockType typeSouth = null;
            Tree.TreeBlock.TreeBlockType typeNorth = null;
            //                    if (randomValue1 < 0.9f || percentHeight < 0.2f) //keep doing what you're doing
            //                    {
            //                        newType = treeBlockToGrow.type;
            //                        if (yCurRelParent == 0) //we're going horizontal or we're close to ground, so go horizontal (following the angle)
            //                        {
            //                            if (randomValue2 < (float)xCurRelBaseMag/zCurRelBaseMag)
            //                            {
            //                                xOffset = xCurRelBase < 0 ? (byte)-1 : 1;
            //                            }
            //                            else
            //                            {
            //                                zOffset = zCurRelBase < 0 ? (byte)-1 : 1;
            //                            }
            //                        }
            //                        else //we're going vertically, so keep doing that
            //                        {
            //                            yOffset = yCurRelParent < 0 ? (byte)-1 : 1;
            //                        }
            //                    }
            //                    else //branch off
            //                    {
            //                        newType = treeBlockToGrow.type.getSmallerType(); //ROOT -> BRANCH -> LEAF
            //                        if (yCurRelParent == 0) //since we're going horizontal, go vertical
            //                        {
            //                            yOffset = randomValue2 < 0.5f && percentHeight > 0.2f ? (byte)-1 : 1;
            //                        }
            //                        else //since we're going up, go horizontal
            //                        {
            //                            if (randomValue2 < 0.5f)
            //                            {
            //                                xOffset = randomValue3 < 0.5f ? (byte)-1 : 1;
            //                            }
            //                            else
            //                            {
            //                                zOffset = randomValue3 < 0.5f ? (byte)-1 : 1;
            //                            }
            //                        }
            //                    }

            switch (treeBlockToBranch.type)
            {
                //                        case ROOT:
                //                            if (treeBlockToBranch.isInGround)
                //                            {
                //                                typeUp = TreeBlock.TreeBlockType.ROOT;
                //                            }
                //                            else
                //                            {
                //                                typeUp = TreeBlock.TreeBlockType.TRUNK;
                //                                if (1f + rSqRelBase < sqRadiusOfTrunkHere)
                //                                {
                //                                    typeEast = typeWest = typeDown = typeSouth = typeNorth = TreeBlock.TreeBlockType.TRUNK;
                //                                }
                //                            }
                //                            break;
                case TRUNK:
                    typeUp = heightPercent >= 1f ? null : Tree.TreeBlock.TreeBlockType.TRUNK;
                    if (1f + rSqRelBase >= sqRadiusOfTrunkHere)
                    {
                        typeEast = typeWest = typeSouth = typeNorth = null;
                    }
                    else
                    {
                        typeEast = typeWest = typeSouth = typeNorth = Tree.TreeBlock.TreeBlockType.TRUNK;
                    }
                    if (heightPercent > TRUNK_LOWBRANCH_HEIGHT_PERCENT && 10f*(heightPercent % 0.1f) < 1.3f*(0.5f + 0.75f*((heightPercent - TRUNK_LOWBRANCH_HEIGHT_PERCENT)/(1f - TRUNK_LOWBRANCH_HEIGHT_PERCENT)))) //make branch
                    {
                        Tree.TreeBlock.TreeBlockType branchType = handyRandy.nextFloat() < 0.2f && world.getBiome(globalX, globalZ).equals(Biome.JUNGLE) ?
                                Tree.TreeBlock.TreeBlockType.COCOA_BEAN : Tree.TreeBlock.TreeBlockType.BRANCH;
                        switch ((int)(4f*(heightPercent % 0.03f)/0.03f))
                        {
                            case 0:
                                typeSouth = branchType;
                                break;
                            case 1:
                                typeWest = branchType;
                                break;
                            case 2:
                                typeNorth = branchType;
                                break;
                            case 3:
                                typeEast = branchType;
                                break;
                        }
                    }
                    break;
                case BRANCH:
                    typeEast = typeWest = typeUp = typeDown = typeSouth = typeNorth = Tree.TreeBlock.TreeBlockType.LEAF;
                    if (RADIUS_OF_LEAF_PER_RADIUS_OF_TRUNK/2*(heightPercent % 0.1f) > rSqRelBase/radiusOfTrunkHere) //continue branch in same direction?
                    {
                        if (xRelBase != 0)
                        {
                            if (xRelBase < 0)
                            {
                                typeWest = Tree.TreeBlock.TreeBlockType.BRANCH;
                            }
                            else
                            {
                                typeEast = Tree.TreeBlock.TreeBlockType.BRANCH;
                            }
                        }
                        else if (zRelBase != 0)
                        {
                            if (zRelBase < 0)
                            {
                                typeNorth = Tree.TreeBlock.TreeBlockType.BRANCH;
                            }
                            else
                            {
                                typeSouth = Tree.TreeBlock.TreeBlockType.BRANCH;
                            }
                        }
                    }
                    break;
                case LEAF: //it's a leaf
                    typeEast = typeWest = typeUp = typeSouth = typeNorth = Tree.TreeBlock.TreeBlockType.LEAF;
                    typeDown = heightPercent > TRUNK_LOWBRANCH_HEIGHT_PERCENT*0.77f/* || heightPercent > TRUNK_LOWBRANCH_HEIGHT_PERCENT /2 && randomFloat < 1f - (TRUNK_LOWBRANCH_HEIGHT_PERCENT - heightPercent)/(TRUNK_LOWBRANCH_HEIGHT_PERCENT /2)*/ ?
                            Tree.TreeBlock.TreeBlockType.LEAF : null;
                    break;
            }

            boolean addToUsable = rSqRelBase + 1 < radiusOfTrunkHere*RADIUS_OF_LEAF_PER_RADIUS_OF_TRUNK;
            treeBlockToBranch.createChild((byte)1, (byte)0, (byte)0, typeEast, addToUsable);
            treeBlockToBranch.createChild((byte)-1, (byte)0, (byte)0, typeWest, addToUsable);
            treeBlockToBranch.createChild((byte)0, (byte)1, (byte)0, typeUp, addToUsable);
            treeBlockToBranch.createChild((byte)0, (byte)-1, (byte)0, typeDown, addToUsable);
            treeBlockToBranch.createChild((byte)0, (byte)0, (byte)1, typeSouth, addToUsable);
            treeBlockToBranch.createChild((byte)0, (byte)0, (byte)-1, typeNorth, addToUsable);
            usableTreeBlocks.remove(treeBlockToBranch);
            return true;
            //                    TreeBlock treeBlockToGrow = usableTreeBlocks.get(handyRandy.nextInt(usableTreeBlocks.size()));
            ////
            ////                    while (treeBlockToGrow.getNourishment() > FULL_NOURISHMENT)
            ////                    {
            ////                        needyBlocks.remove(treeBlockToGrow);
            ////                        treeBlockToGrow = needyBlocks.get(handyRandy.nextInt(needyBlocks.size()));
            ////                    }
            ////                    while (treeBlockToGrow.getChildren().size() > 0)
            ////                    {
            ////                        treeBlockToGrow = treeBlockToGrow.getMostNeedyChild();
            ////                        if (treeBlockToGrow.getNourishment() < 1.25f)
            ////                        {
            ////                            branchOff = true;
            ////                        }
            ////                    }
            //                    float randomValue1 = handyRandy.nextFloat();
            //                    float randomValue2 = 2f*(randomValue1 % 0.5f);
            //                    float randomValue3 = 2f*(randomValue2 % 0.5f);
            //                    float randomValue4 = 2f*(randomValue3 % 0.5f);
            //                    float randomValue5 = 2f*(randomValue4 % 0.5f);
            //                    float randomValue6 = 2f*(randomValue5 % 0.5f);
            //
            //                    final TreeBlock.TreeBlockType newType;
            //
            //                    byte xOffset = 0;
            //                    byte yOffset = 0;
            //                    byte zOffset = 0;
            //                    final byte xCurRelBase = treeBlockToGrow.xRelBase;
            //                    final byte yCurRelBase = treeBlockToGrow.yRelBase;
            //                    final byte zCurRelBase = treeBlockToGrow.zRelBase;
            //                    float heightPercent = (float)yCurRelBase/height;
            //                    final byte xCurRelBaseMag = xCurRelBase < 0 ? (byte)-xCurRelBase : xCurRelBase;
            //                    //final byte yCurRelBaseMag = yCurRelBase;
            //                    final byte zCurRelBaseMag = zCurRelBase < 0 ? (byte)-zCurRelBase : zCurRelBase;
            //                    final byte yCurRelParent = treeBlockToGrow.parent == null ? 1 : (byte)(yCurRelBase - treeBlockToGrow.parent.yRelBase);
            //
            ////                    if (randomValue1 < 0.9f || percentHeight < 0.2f) //keep doing what you're doing
            ////                    {
            ////                        newType = treeBlockToGrow.type;
            ////                        if (yCurRelParent == 0) //we're going horizontal or we're close to ground, so go horizontal (following the angle)
            ////                        {
            ////                            if (randomValue2 < (float)xCurRelBaseMag/zCurRelBaseMag)
            ////                            {
            ////                                xOffset = xCurRelBase < 0 ? (byte)-1 : 1;
            ////                            }
            ////                            else
            ////                            {
            ////                                zOffset = zCurRelBase < 0 ? (byte)-1 : 1;
            ////                            }
            ////                        }
            ////                        else //we're going vertically, so keep doing that
            ////                        {
            ////                            yOffset = yCurRelParent < 0 ? (byte)-1 : 1;
            ////                        }
            ////                    }
            ////                    else //branch off
            ////                    {
            ////                        newType = treeBlockToGrow.type.getSmallerType(); //ROOT -> BRANCH -> LEAF
            ////                        if (yCurRelParent == 0) //since we're going horizontal, go vertical
            ////                        {
            ////                            yOffset = randomValue2 < 0.5f && percentHeight > 0.2f ? (byte)-1 : 1;
            ////                        }
            ////                        else //since we're going up, go horizontal
            ////                        {
            ////                            if (randomValue2 < 0.5f)
            ////                            {
            ////                                xOffset = randomValue3 < 0.5f ? (byte)-1 : 1;
            ////                            }
            ////                            else
            ////                            {
            ////                                zOffset = randomValue3 < 0.5f ? (byte)-1 : 1;
            ////                            }
            ////                        }
            ////                    }
            //
            //                    switch (treeBlockToGrow.type)
            //                    {
            //                        case TRUNK:
            //                            if (heightPercent > TRUNK_LOWBRANCH_HEIGHT_PERCENT && randomValue1 < 0.23f*(1f + (heightPercent - TRUNK_LOWBRANCH_HEIGHT_PERCENT)/(1f - TRUNK_LOWBRANCH_HEIGHT_PERCENT))) //make branch
            //                            {
            //                                newType = TreeBlock.TreeBlockType.BRANCH;
            //                                int directionValue = (int)(4f*randomValue2);
            //                                switch (directionValue)
            //                                {
            //                                    case 0:
            //                                        zOffset = 1;
            //                                        break;
            //                                    case 1:
            //                                        xOffset = -1;
            //                                        break;
            //                                    case 2:
            //                                        zOffset = -1;
            //                                        break;
            //                                    default:
            //                                        xOffset = 1;
            //                                        break;
            //                                }
            //                            }
            //                            else //make more trunk
            //                            {
            //                                newType = TreeBlock.TreeBlockType.TRUNK;
            //                                if (randomValue1 < 0.4f)
            //                                {
            //                                    //if we can go farther to the side, make horizontal not toward center
            //                                    float radiusOfTrunkHere = (1f - heightPercent)*maxTrunkRadius + heightPercent*minTrunkRadius;
            //                                    if (xCurRelBase*xCurRelBase + zCurRelBase*zCurRelBase < radiusOfTrunkHere*radiusOfTrunkHere)
            //                                    {
            //                                        if (randomValue2 < 0.5f)
            //                                        {
            //                                            xOffset = xCurRelBase < 0 ? (byte)-1 : 1;
            //                                        }
            //                                        else
            //                                        {
            //                                            zOffset = zCurRelBase < 0 ? (byte)-1 : 1;
            //                                        }
            //                                    }
            //                                }
            //                                else
            //                                {
            //                                    //Bukkit.broadcastMessage("" + treeHeightPrecise);
            //                                    //make vertical
            //                                    yOffset = 1;
            //                                }
            //                            }
            //                            break;
            //                        case BRANCH:
            //                            if (randomValue1 < 0.2f)
            //                            {
            //                                newType = TreeBlock.TreeBlockType.BRANCH;
            //                                if (randomValue2 < 0.43333f)
            //                                {
            //                                    xOffset = xCurRelBase < 0 ? (byte)-1 : 1;
            //                                }
            //                                else if (randomValue2 < 0.43333f)
            //                                {
            //                                    zOffset = zCurRelBase < 0 ? (byte)-1 : 1;
            //                                }
            //                                else
            //                                {
            //                                    yOffset = randomValue2 < 0.5f ? (byte)-1 : 1;
            //                                }
            //                            }
            //                            else
            //                            {
            //                                newType = TreeBlock.TreeBlockType.LEAF;
            //                                if (randomValue1 < 0.5f)
            //                                {
            //                                    xOffset = randomValue2 < 0.5f ? (byte)-1 : 1;
            //                                }
            //                                if (randomValue3 < 0.5f)
            //                                {
            //                                    zOffset = randomValue4 < 0.5f ? (byte)-1 : 1;
            //                                }
            //                                if (randomValue5 < 0.5f)
            //                                {
            //                                    yOffset = randomValue6 < 0.5f ? (byte)-1 : 1;
            //                                }
            //                            }
            //                            break;
            //                        default:
            //                            newType = TreeBlock.TreeBlockType.LEAF;
            //                            if (randomValue1 < 0.5f)
            //                            {
            //                                xOffset = randomValue2 < 0.5f ? (byte)-1 : 1;
            //                            }
            //                            if (randomValue2 < 0.5f)
            //                            {
            //                                zOffset = randomValue4 < 0.5f ? (byte)-1 : 1;
            //                            }
            //                            if (randomValue5 < 0.5f)
            //                            {
            //                                yOffset = randomValue6 < 0.5f ? (byte)-1 : 1;
            //                            }
            //                            break;
            //                    }
            ////                    if (newRelY > height)
            ////                    {
            ////                        return;
            ////                    }
            //                    TreeBlock newBlock = treeBlockToGrow.grow(xOffset, yOffset, zOffset, newType);
            //                    if (newBlock == null)
            //                    {
            //                        return false;
            //                    }
            //                    needyBlocks.add(newBlock);
            //                    needyBlocks.remove(treeBlockToGrow);
            //                    return true;
            ////
            ////                        if (yOffset > 0)
            ////                        {
            ////                            usableTreeBlocks.remove(treeBlockToBranch);
            ////                        }
            ////                        allBlocks.add(newBlock);
            ////                        usableTreeBlocks.add(newBlock);
            ////                        newBlock.appendChild(new TreeBlock(this, newRelX, newRelY, newRelZ, newType));
        }

        private static class TreeBlock
        {
            public enum TreeBlockType
            {
                TRUNK()
                        {
                            @Override
                            public Material getBaseMaterial(Biome biome)
                            {
                                if (biome.equals(Biome.SAVANNA))
                                {
                                    return Material.LOG_2;
                                }
                                return Material.LOG;
                            }
                        },
                BRANCH()
                        {
                            @Override
                            public Material getBaseMaterial(Biome biome)
                            {
                                if (biome.equals(Biome.SAVANNA))
                                {
                                    return Material.LOG_2;
                                }
                                return Material.LOG;
                            }
                        },
                COCOA_BEAN()
                        {
                            @Override
                            public Material getBaseMaterial(Biome biome)
                            {
                                return Material.COCOA;
                            }
                        },
                LEAF()
                        {
                            @Override
                            public Material getBaseMaterial(Biome biome)
                            {
                                if (biome.equals(Biome.SAVANNA))
                                {
                                    return Material.LEAVES_2;
                                }
                                return Material.LEAVES;
                            }
                        };

                public abstract Material getBaseMaterial(Biome biome);
                //                        private TreeBlockType getSmallerType()
                //                        {
                //                            int i;
                //                            for (i = 0; values()[i] != this; i++);
                //                            i++;
                //                            if (i == values().length)
                //                            {
                //                                i--;
                //                            }
                //                            return values()[i];
                //                        }
            }

            private final byte xRelBase;
            private final byte yRelBase;
            private final byte zRelBase;
            private final Tree.TreeBlock.TreeBlockType type;
            //private final ArrayList<TreeBlock> children;
            private final Tree owner;
            //private final TreeBlock parent;
            //private float nourishment;
            //private final boolean isInGround;

            private TreeBlock(Tree.TreeBlock parent, Tree owner/*has to be world to allow placement outside single chunk. like if you scratch head every tim*/, byte xRelBase, byte yRelBase, byte zRelBase, Tree.TreeBlock.TreeBlockType type)
            {
                this.owner = owner;
                //this.parent = parent;
                //children = new ArrayList<TreeBlock>();

                this.xRelBase = xRelBase;
                this.yRelBase = yRelBase;
                this.zRelBase = zRelBase;
                this.type = type;

                int globalY = yRelBase + owner.globalY;
                if (globalY >= 0 && globalY < 0x100)
                {
                    int globalX = xRelBase + owner.globalX;
                    int globalZ = zRelBase + owner.globalZ;
                    World w = owner.world;
                    Block b = w.getBlockAt(globalX, globalY, globalZ);

                    //                            boolean stuck = false;
                    //                            if (parent == null /*|| parent.isInGround*/)
                    //                            {
                    //                                byte blockedBlocks = 0;
                    //                                for (byte x = (byte)-1; x <= (byte)1; x += (byte)2)
                    //                                {
                    //                                    for (byte z = (byte)-1; z <= (byte)1; z += (byte)2)
                    //                                    {
                    //                                        short item = (short)b.getRelative(x, 0, z).getTypeId();
                    //                                        if (item != 0 && item != 17 && item != 18)
                    //                                        {
                    //                                            blockedBlocks++;
                    //                                            if (blockedBlocks > 2)
                    //                                            {
                    //                                                stuck = true;
                    //                                                break;
                    //                                            }
                    //                                        }
                    //                                    }
                    //                                }
                    //                            }
                    //                            //Bukkit.broadcastMessage("block at (" + xRelBase + ", "+ yRelBase + ", " + zRelBase + ") is stuck = " + stuck);
                    //                            isInGround = stuck;
                    Biome biomeHere = w.getBiome(globalX, globalZ);
                    b.setType(type.getBaseMaterial(biomeHere));

                    switch (biomeHere)
                    {
                        case TAIGA:
                        case TAIGA_COLD:
                            b.setData((byte)1);
                            break;
                        case JUNGLE:
                            b.setData((byte)3);
                            break;
                    }
                }
                //                        else
                //                        {
                //                            isInGround = false;
                //                        }

                //this.nourishment = 0;
            }
            //                    private TreeBlock(TreeBlock parent, Tree owner,/*has to be world to allow placement outside single chunk. like if you scratch head every tim*/Block b, byte xRelBase, byte yRelBase, byte zRelBase, TreeBlockType type)
            //                    {
            //                        this.owner = owner;
            //                        this.parent = parent;
            //                        //children = new ArrayList<TreeBlock>();
            //
            //                        this.xRelBase = xRelBase;
            //                        this.yRelBase = yRelBase;
            //                        this.zRelBase = zRelBase;
            //                        this.type = type;
            //
            //                        if (b != null)
            //                        {
            //                            b.setType(type.baseMaterial);
            //                            switch (b.getBiome())
            //                            {
            //                                case TAIGA:
            //                                case COLD_TAIGA:
            //                                    b.setData((byte)1);
            //                                    break;
            //                                case JUNGLE:
            //                                    b.setData((byte)3);
            //                                    break;
            //                            }
            //                        }
            //                        else
            //                        {
            //                            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Error! Tried to create a null tree block!");
            //                        }
            //
            //                        this.nourishment = 0;
            //                    }

            private void createChild(byte xOffset, byte yOffset, byte zOffset, Tree.TreeBlock.TreeBlockType newBlockType, boolean branchable)
            {
                final byte xRelBase = (byte)(this.xRelBase + xOffset);
                final byte yRelBase = (byte)(this.yRelBase + yOffset);
                final byte zRelBase = (byte)(this.zRelBase + zOffset);
                //                        if (parent != null && parent.xRelBase == xRelBase && parent.yRelBase == yRelBase && parent.zRelBase == zRelBase)
                //                        {
                //                            return null;
                //                        }
                //                        for (TreeBlock treeBlock : getChildren())
                //                        {
                //                            if (treeBlock.xRelBase == xRelBase && treeBlock.yRelBase == yRelBase && treeBlock.zRelBase == zRelBase)
                //                            {
                //                                //Bukkit.broadcastMessage("failed at (" + xRelBase + ", " + yRelBase + ", " + zRelBase + ")");
                //                                return null;
                //                            }
                //                        }
                //                        final byte xRelParent = parent == null ? 0 : (byte)(xRelBase - parent.xRelBase);
                //                        final byte yRelParent = parent == null ? 1 : (byte)(yRelBase - parent.yRelBase);
                //                        final byte zRelParent = parent == null ? 0 : (byte)(zRelBase - parent.zRelBase);
                short existingMat = (short)owner.world.getBlockAt( xRelBase + owner.globalX, yRelBase + owner.globalY, zRelBase + owner.globalZ).getTypeId();
                int globalY = yRelBase + owner.globalY;
                if (!(newBlockType == null ||
                        //parent != null && xRelBase == parent.xRelBase && yRelBase == parent.yRelBase && zRelBase == parent.zRelBase ||
                        existingMat != 0 && existingMat != 31 && existingMat != 32 && existingMat != 37 && existingMat != 38 ||
                        globalY < 0 || globalY > 0x100))
                {
                    Tree.TreeBlock result = new Tree.TreeBlock(this, owner, xRelBase, yRelBase, zRelBase, newBlockType);
                    if (branchable)
                    {
                        owner.usableTreeBlocks.add(result);
                    }
                    owner.createdBlocks++;
                    //Bukkit.broadcastMessage("Fail at (" + xRelBase + ", " + yRelBase + ", " + zRelBase + ")");
                    //Bukkit.broadcastMessage("" + newBlockType);
                    //Bukkit.broadcastMessage("" + (parent != null && xRelBase == parent.xRelBase && yRelBase == parent.yRelBase && zRelBase == parent.zRelBase));
                    //Bukkit.broadcastMessage("" + (!type.equals(TreeBlockType.ROOT)));
                    //Bukkit.broadcastMessage("" + (!owner.world.getBlockAt( xRelBase + owner.globalX, yRelBase + owner.globalY, zRelBase + owner.globalZ).getType().equals(Material.AIR)));
                    //Bukkit.broadcastMessage("" + (globalY < 0 || globalY > 0x100));
                }
                //                        short id = (short)block.getTypeId();
                //                        if (id != 0 && (yRelBase > 2 || (id != 2 && id != 3)))
                //                        {
                //                            //Bukkit.broadcastMessage("air fail at (" + xRelBase + ", " + yRelBase + ", " + zRelBase + ") - block was " + id);
                //                            return null;
                //                        }
                //children.add(newOne);
                //                        float nourishmentAdd = 1f;
                //                        TreeBlock ancestor = this;
                //                        while (ancestor != null && ancestor.nourishment < FULL_NOURISHMENT)
                //                        {
                //                            ancestor.nourishment += nourishmentAdd;
                //                            nourishmentAdd = nourishmentAdd / (nourishmentAdd + 1f);
                //                            ancestor = ancestor.parent;
                //                        }
                //Bukkit.broadcastMessage("success at (" + xRelBase + ", " + yRelBase + ", " + zRelBase + ") " + newBlockType);
            }
            //                    private ArrayList<TreeBlock> getChildren()
            //                    {
            //                        return children;
            //                    }
            //
            //                    private TreeBlock getMostNeedyChild()
            //                    {
            //                        TreeBlock mostNeedy = null;
            //                        for (TreeBlock child : getChildren())
            //                        {
            //                            if (mostNeedy == null || child.getNourishment() < mostNeedy.getNourishment())
            //                            {
            //                                mostNeedy = child;
            //                            }
            //                        }
            //                        return mostNeedy;
            //                    }
            //
            //                    private float getNourishment()
            //                    {
            //                        return nourishment;
            //                    }
        }
    }

    private static boolean isVillagerChunk(Chunk chunk, short heightInMiddle)
    {
        if (heightInMiddle > HERMIT_HEIGHT_MIN && heightInMiddle < HERMIT_HEIGHT_MAX)
        {
            handyRandy.setSeed(seed - (chunk.getX()*0x77770 + chunk.getZ()*0x777770));
            return handyRandy.nextFloat() < HERMITS_PER_HIGH_CHUNK;
        }
        return false;
    }

    private static boolean isVillagerChunk(Chunk chunk)
    {
        return isVillagerChunk(chunk, WorldGenerationTools.getHighestBlockYInChunk(chunk, HERMIT_HUT_DOOR_X, HERMIT_HUT_DOOR_Z));
    }

    public enum ShopEntranceState
    {
        ENTERING, EXITING, NOT_IN_DOOR;
    }

    public static ShopEntranceState getShopEntranceState(Location oldLocation, Location newLocation)
    {
        Chunk chunk = newLocation.getChunk();
        if (isVillagerChunk(chunk))
        {
            int chunkAbsX = chunk.getX()*0x10;
            if (newLocation.getBlock().getRelative(0, -1, 0).getType().equals(HERMIT_HUT_FLOOR_MATERIAL) || newLocation.getBlock().getRelative(0, -2, 0).getType().equals(HERMIT_HUT_FLOOR_MATERIAL))
            {
                if (newLocation.getX() < chunkAbsX + HERMIT_HUT_EAST_BOUND_OF_DOOR + 1 &&
                        newLocation.getX() > chunkAbsX + HERMIT_HUT_WEST_BOUND_OF_DOOR)
                {
                    int chunkAbsZ = chunk.getZ()*0x10;

                    if (newLocation.getZ() - oldLocation.getZ() < 0)
                    {
                        if (newLocation.getZ() < chunkAbsZ + HERMIT_HUT_SOUTH_BOUND + 1 &&
                                newLocation.getZ() > chunkAbsZ + HERMIT_HUT_SOUTH_BOUND + 0.5)
                        {
                            return ShopEntranceState.ENTERING;
                        }
                    }
                    else
                    {
                        if (newLocation.getZ() < chunkAbsZ + HERMIT_HUT_SOUTH_BOUND + 0.5 &&
                                newLocation.getZ() > chunkAbsZ + HERMIT_HUT_SOUTH_BOUND)
                        {
                            return ShopEntranceState.EXITING;
                        }
                    }
                }
            }
        }
        return ShopEntranceState.NOT_IN_DOOR;
    }
}
