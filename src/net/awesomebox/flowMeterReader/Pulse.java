package net.awesomebox.flowMeterReader;

/**
 * Represents a detected pulse.
 */
public class Pulse
{
	public final AudioSample startSample;
	public final AudioSample endSample;
	public final int         flowMeterID;
	
	public Pulse(AudioSample startSample, AudioSample endSample, int flowMeterID)
	{
		this.startSample = startSample;
		this.endSample   = endSample;
		this.flowMeterID = flowMeterID;
	}
}