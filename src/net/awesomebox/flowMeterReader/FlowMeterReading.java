package net.awesomebox.flowMeterReader;

/**
 * Contains the results of a flow meter audio data reading.
 */
public final class FlowMeterReading
{
	public final AudioSample[] samples;
	public final Pulse[]       pulses;
	
	public FlowMeterReading(AudioSample[] samples, Pulse[] pulses)
	{
		this.samples = samples;
		this.pulses  = pulses;
	}
}
