package com.hsrg.bp.demo;

import android.util.Log;

import java.util.Arrays;

/**
 * 数据处理类
 * @author zbf
 *
 */
public class ParserHelper
{
	public static byte[] setBpmWriteData(int paramInt1, int paramInt2)
	{
		byte[] arrayOfByte = new byte[6];
		arrayOfByte[0] = 2;
		arrayOfByte[1] = 64;
		arrayOfByte[2] = -36;
		arrayOfByte[3] = 1;
		arrayOfByte[4] = ((byte)paramInt1);
		arrayOfByte[5] = ((byte)paramInt2);
		Log.i("[ParserHelper]", "set bpm write data:" + Arrays.toString(arrayOfByte));
		return arrayOfByte;
	}

}
