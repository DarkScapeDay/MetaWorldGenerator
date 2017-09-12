package me.happyman;

import java.util.Random;

public class WorldMysteries
{
    private static int[] mysteryEntryValues = new int[9*3];
    static
    {
        Random handyRandy = new Random(101230123402341434L);
        for (int i = 0; i < mysteryEntryValues.length; i++)
        {
            int amount = handyRandy.nextInt(64) + 1;
            mysteryEntryValues[i] = amount;
        }
    }

    public static int[] getMysteryEntryValues()
    {
        int[] copy = new int[mysteryEntryValues.length];
        for (int i = 0; i < mysteryEntryValues.length; i++)
        {
            copy[i] = mysteryEntryValues[i];
        }
        return copy;
    }

    public static String[] getMysteryEntryHexValues()
    {
        String[] result = new String[mysteryEntryValues.length];
        for (int i = 0; i < mysteryEntryValues.length; i++)
        {
            result[i] = HexConverter.convertToHex(mysteryEntryValues[i]);
        }
        return result;
    }
}
