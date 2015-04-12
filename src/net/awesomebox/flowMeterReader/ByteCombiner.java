package net.awesomebox.flowMeterReader;

/**
 * Provides functions for combining bytes together.
 */
public class ByteCombiner
{
	/**
	 * Combines two bytes into a short by appending the bytes on the binary level.
	 * Example:<br />
	 * 
	 * <code>byte1</code> bits = <code>11010010</code><br />
	 * <code>byte2</code> bits = <code>10110101</code><br />
	 * returned short bits = <code>11010010 10110101</code>
	 * 
	 * @param byte1     - Most significant byte
	 * @param byte2     - Least significant byte
	 * @param bigEndian - Endianness should be big-endian (<code>true</code>) or
	 *                    little-endian (<code>false</code>). If big-endian,
	 *                    <code>byte1</code> will be first and <code>byte2</code>
	 *                    will be appended after. Vice-versa for little-endian.
	 * 
	 * @return The short value of the combined bytes.
	 */
	public static short toShort(byte byte1, byte byte2, boolean bigEndian) {
		// Remember! The data types are signed! (and no way to use unsigned... grumble grumble)
		// We assume the data being read is signed.
		
		// For the examples bellow I will be using big-endianness with the following data:
		// 11010010 10110101 (-11595)
		//
		// we have:
		// signed byte1: 11010010 (-46)
		// signed byte2: 10110101 (-75)
		
		
		// ---------------------------------------------------------------------
		// first we convert the signed bytes, to signed shorts
		short short1;
		short short2;
		
		if (bigEndian)
		{
			short1 = byte1;
			short2 = byte2;
		}
		else
		{
			// if bytes are little-endian, the most significant bit is last and we must reverse the order
			short1 = byte2;
			short2 = byte1;
		}
		
		// now we have:
		// signed short1: 11111111 11010010 (-46)
		// signed short2: 11111111 10110101 (-75)
		//
		// the new bits are filled with 1's if the signed byte was negative to maintain the negative sign
		
		
		// ---------------------------------------------------------------------
		// shift the most significant byte over 8 bits
		short1 <<= 8;
		//    11111111 11010010
		// << 8
		//    -----------------
		//    11010010 00000000
		
		// now we have:
		// signed short1: 11010010 00000000 (-11776)
		// signed short2: 11111111 10110101 (-75)
		//
		// the bits added on the end during the shift are always 0
		// we don't have to worry about this short being signed as the first 8 bits will be pushed off by this shift
		
		
		// ---------------------------------------------------------------------
		// mask the least significant byte so all bits are 0 except the last 8. This is necessary if the signed byte was negative.
		short2 &= 0b00000000_11111111;
		//   11111111 10110101
		// & 00000000 11111111
		//   -----------------
		//   00000000 10110101
		
		// now we have:
		// signed short1: 11010010 00000000 (-11776)
		// signed short2: 00000000 10110101 (181)
		
		
		// ---------------------------------------------------------------------
		// finally, combine the two shorts with a bitwise OR
		short shortVal = (short)(short1 | short2);
		//   11010010 00000000
		// | 00000000 10110101
		//   -----------------
		//   11010010 10110101
		
		// and we have our original value:
		// 11010010 10110101 (-11595)
		return shortVal;
	}
}