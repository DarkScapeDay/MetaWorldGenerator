package me.happyman;

import java.util.ArrayList;

public class HexConverter
{
    public static int convertToDecimal(String hexString)
    {
        int result = 0;
        for (int i = hexString.length() - 1, powerOf16 = 1; i >= 0; powerOf16 *= 0x10, i--)
        {
            int charHere = hexString.charAt(i);
            int valueHere = charHere + (charHere > (int)'9' ? -(int)'A' + 10 : -(int)'0');
            result += valueHere*powerOf16;
        }
        return result;
    }

    public static String convertToHex(int decimalValue)
    {
        ArrayList<Boolean> remainders = new ArrayList<Boolean>();
        while (decimalValue > 0)
        {
            remainders.add(decimalValue % 2 == 1);
            decimalValue /= 2;
        }

        final int numberOfHexDigits = remainders.size()/4 + (remainders.size()%4 == 0 ? 0 : 1);
        char[] resultString = new char[numberOfHexDigits];

        for (int i = 0; i < numberOfHexDigits; i++)
        {
            int hexHere = 0;
            for (int j = i*4, powerOf2 = 1; j < i*4 + 4 && j < remainders.size(); powerOf2 *= 2, j++)
            {
                if (remainders.get(j))
                {
                    hexHere += powerOf2;
                }
            }

            resultString[resultString.length-1-i] = (char)(hexHere + (hexHere > 9 ? ((int)'A' - 10) : (int)'0'));
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < numberOfHexDigits; i++)
        {
            result.append(resultString[i]);
        }

        return result.toString();
    }
}