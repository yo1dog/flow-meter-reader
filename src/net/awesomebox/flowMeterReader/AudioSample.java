package net.awesomebox.flowMeterReader;

/**
 * Represents an audio sample.<br />
 * <br />
 * An audio sample represents the amplitude of an audio signal at a given time.
 * When an audio signal is graphed as a wave, time is the X-axis and amplitude is the Y-axis.
 */
public class AudioSample
{
	public static final long TIME_MIN_VALUE = 0;
	public static final long TIME_MAX_VALUE = Long.MAX_VALUE;
	
	public static final short AMPLITUDE_MIN_VALUE = Short.MIN_VALUE;
	public static final short AMPLITUDE_MAX_VALUE = Short.MAX_VALUE;
	
	
	// I used long for time so I don't have to worry about overflow. Starting from 0, long
	// can store 2^63-1 nanoseconds, which means we can run for 292+ years! An int (2^31-1)
	// would only last 2.1 seconds.
	public final long timeNS;
	
	// I used short for amplitude because my audio sample size is 16 bits (2 bytes)
	public final short amplitude;
	
	
	public AudioSample(long timeNS, short amplitude)
	{
		this.timeNS    = timeNS;
		this.amplitude = amplitude;
	}
}
