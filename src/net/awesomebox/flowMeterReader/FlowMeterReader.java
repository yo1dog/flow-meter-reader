package net.awesomebox.flowMeterReader;

import java.util.ArrayList;


public class FlowMeterReader
{
	// ===================================================================
	// Constants
	//
	// ===================================================================
	
	// number of nanoseconds in a second
	public static final long NS_IN_S  = 1000000000l;
	public static final long NS_IN_MS = 1000000l;
	
	// max time in nanoseconds a pulse can exist for
	// meaning, this is the max time the amplitude can rise/fall for it to be considered caused by a pulse
	private static final long MAX_PULSE_DURATION_NS = 625000; // 10 samples at 16000Hz
	
	// the min amplitude delta for it to be considered a pulse
	public static final double FM1_PULSE_AMPLITUDE_DELTA_THRESHOLD_RATIO = 0.15d;
	public static final double FM2_PULSE_AMPLITUDE_DELTA_THRESHOLD_RATIO = 0.75d;
	public static final int FM1_PULSE_AMPLITUDE_DELTA_THRESHOLD = (int)(FM1_PULSE_AMPLITUDE_DELTA_THRESHOLD_RATIO * AudioSample.AMPLITUDE_MAX_VALUE);
	public static final int FM2_PULSE_AMPLITUDE_DELTA_THRESHOLD = (int)(FM2_PULSE_AMPLITUDE_DELTA_THRESHOLD_RATIO * AudioSample.AMPLITUDE_MAX_VALUE);
	
	
	// ===================================================================
	// Variables
	//
	// ===================================================================
	
	// number of audio samples processed
	private long numSamplesProcessed = 0;
	
	// sample rate of the audio stream we are reading
	private final int sampleRate;
	
	
	// -------------------------------------------------------------------
	// derived constants
	
	// max number of samples a pulse can exist for
	private final int maxPulseDurationNumSamples;
	
	
	// -------------------------------------------------------------------
	// intermediate processAudioData variables
	
	// the leftover byte in the event that an odd number of bytes were given to processAudioData
	// this is necessary because two bytes are required to create a short
	private byte    leftoverAudioDataByte;
	private boolean useLeftoverAudioDataByte = false;
	
	// at the end of a processAudioData call, some of then end samples are stored
	// this way, during the next execution of processAudioData, it can look back
	// at the samples from the last run in order to calculate the amplitude delta.
	// the number of samples needed to be back-filled is equal to the number of
	// samples required to calculate the amplitude delta.
	private final AudioSample[] backfilledSamples;
	private int numBackfilledSamples = 0;
	
	
	
	// ===================================================================
	// Constructor
	//
	// ===================================================================
	
	public FlowMeterReader(int sampleRate)
	{
		this.sampleRate = sampleRate;
		
		// calculate derived constants
		// D = AMPLITUDE_DELTA_DURATION_NS
		// R = sampleRate
		// D ns * (R samples/s) * (1s/1000000000ns) = (D * R samples)/1000000000
		maxPulseDurationNumSamples = (int)Math.max(1, (MAX_PULSE_DURATION_NS * sampleRate) / NS_IN_S);
		
		// setup intermediate processAudioData variables
		backfilledSamples = new AudioSample[maxPulseDurationNumSamples];
	}
	
	
	
	// ===================================================================
	// Methods
	//
	// ===================================================================
	
	/**
	 * @see #readFlowMeterAudioData(byte[], int, int, boolean)
	 */
	public FlowMeterReading readFlowMeterAudioData(byte[] data, int dataOffset, int dataLength)
	{
		return readFlowMeterAudioData(data, dataOffset, dataLength, true);
	}
	
	/**
	 * Processes the given audio data to find pulses from the flow meters.<br />
	 * <br />
	 * Data passed to this method over multiple executions is treated as one data set. Meaning, data from
	 * previous executions of this function may be used during subsequent executions. This is intended for
	 * uses such as streaming where small data sets can be passed to this function without having to worry
	 * about pulses being cut off or not detected. Only necessary data is kept in memory so excessive
	 * memory build up will not occur with multiple executions of this function.
	 * 
	 * @param data       - Audio data.
	 * @param dataOffset - Offset to start from in bytes.
	 * @param dataLength - Number of bytes to read.
	 * @param bigEndian  - If the data is big-endian (true) or little-endian (false).
	 */
	public int maxAmplitude = 0;
	public int minAmplitude = 0;
	public FlowMeterReading readFlowMeterAudioData(byte[] data, int dataOffset, int dataLength, boolean bigEndian)
	{
		// create audio samples from the audio data
		AudioSample[] samples = createSamplesFromAudioData(data, dataOffset, dataLength, bigEndian);
		
		// detect pulses
		Pulse[] pulses = detectPulses(samples);
		
		// done
		return new FlowMeterReading(samples, pulses);
	}
	
	/**
	 * Creates {@link AudioSample}s for the given audio data.
	 * 
	 * @param data       - Audio data.
	 * @param dataOffset - Offset to start from in bytes
	 * @param dataLength - Number of bytes to read.
	 * @param bigEndian  - If the data is big-endian (true) or little-endian (false).
	 * 
	 * @return List of {@link AudioSample}s created from the given audio data.
	 */
	private AudioSample[] createSamplesFromAudioData(byte[] data, int dataOffset, int dataLength, boolean bigEndian)
	{
		// because we need two bytes for every sample, the data length must be kept even
		int evenDataLength = dataLength;
		
		if (useLeftoverAudioDataByte) // account for the offset of using the leftover byte
			++evenDataLength;
		
		if (evenDataLength % 2 == 1)
			--evenDataLength;
		
		
		// calculate ahead how many samples will be created
		int numSamplesWillBeCreated = evenDataLength / 2; // two bytes are used for each sample
		
		// create an array to hold the samples
		int numSamples = 0;
		AudioSample[] samples = new AudioSample[numSamplesWillBeCreated];
		
		
		// iterate through the data bytes and create the raw samples
		for (int i = 0; i < evenDataLength; i += 2)
		{
			// select the bytes to use
			byte byte1;
			byte byte2;
			
			// check if we should use the leftover byte
			if (useLeftoverAudioDataByte)
			{
				// use the leftover byte and the first byte
				byte1 = leftoverAudioDataByte;
				byte2 = data[dataOffset];
				
				// don't use the leftover byte anymore
				useLeftoverAudioDataByte = false;
				
				// decrement the index to offset using the leftover byte
				--i;
			}
			
			// use the normal bytes based on the index
			else
			{
				byte1 = data[i + dataOffset];
				byte2 = data[i + dataOffset + 1];
			}
			
			
			// combine bytes to a single short to get the amplitude
			short amplitude = ByteCombiner.toShort(byte1, byte2, bigEndian);
			
			// calculate time based on how many samples have been processed so far
			// N = numSamplesProcessed
			// R = sampleRate
			// N samples * (1/(R samples/s)) * (1000000000ns/1s) = (N * 1000000000ns)/R
			long time = (numSamplesProcessed * NS_IN_S) / sampleRate;
			
			
			// create an audio sample
			AudioSample sample = new AudioSample(time, amplitude);
			
			// increment the number of samples processed
			++numSamplesProcessed;
			
			// add it to the list of samples
			samples[numSamples] = sample;
			++numSamples;
		}
		
		// check if we have a leftover byte
		if (evenDataLength < dataLength)
		{
			leftoverAudioDataByte = data[dataOffset + dataLength - 1];
			useLeftoverAudioDataByte = true;
		}
		
		return samples;
	}
	
	
	
	/**
	 * Detects pulses in audio samples. The given audio samples are used along with
	 * any back-filled samples from previous executions.
	 * 
	 * @param newSamples - New audio samples to check for pulses in.
	 * 
	 * @return The number of pulses found. Index 0 contains pulses for flow meter 1,
	 * index 1 contains pulses for flow meter 2.
	 */
	private Pulse[] detectPulses(AudioSample[] newSamples)
	{
		ArrayList<Pulse> pulsesList = new ArrayList<Pulse>();
		
		// if we don't have any new samples, there is nothing to do
		if (newSamples.length == 0)
			return new Pulse[0];
		
		
		// combine the back-filled samples and the new samples
		AudioSample[] samples = new AudioSample[numBackfilledSamples + newSamples.length];
		
		for (int i = 0; i < numBackfilledSamples; ++i)
			samples[i] = backfilledSamples[i];
		
		for (int i = 0; i < newSamples.length; ++i)
			samples[numBackfilledSamples + i] = newSamples[i];
		
		
		int lastFoundEdgeEndIndex = -1;
		
		// follow all the edges in the samples and check if they are pulses
		// make sure we always have enough samples to make the longest possible pulse
		for (int i = 0; i < samples.length - maxPulseDurationNumSamples; ++i)
		{
			// follow the edge
			int edgeStartIndex = i;
			int edgeEndIndex = followEdge(samples, edgeStartIndex, maxPulseDurationNumSamples);
			
			// check if we followed the edge to the end
			if (edgeEndIndex == -1)
			{
				// we did not. This means the edge was longer than the max pulse duration.
				// We can only infer that there is no pulse that starts at i, but there may
				// be one that starts at i+1.
				continue;
			}
			
			// at this point, we know this edge absolutely is or is not a pulse. Either way,
			// we should start our next search from the end of this edge
			i = edgeEndIndex - 1; // -1 to compensate for the ++i in the for loop
			
			// we should also not back-fill any samples before the end of the edge
			// record the end index of this edge
			lastFoundEdgeEndIndex = edgeEndIndex;
			
			
			AudioSample startSample = samples[edgeStartIndex];
			AudioSample endSample   = samples[edgeEndIndex];
			
			// get the amplitude delta for the edge
			int edgeAmplitudeDelta = endSample.amplitude - startSample.amplitude;
			int absEdgeAmplitudeDelta = Math.abs(edgeAmplitudeDelta);
			
			// check if this edge was a pulse from flow meter 2
			if (absEdgeAmplitudeDelta >= FM2_PULSE_AMPLITUDE_DELTA_THRESHOLD)
			{
				// it was! create a pulse
				Pulse pulse = new Pulse(startSample, endSample, 2);
				pulsesList.add(pulse);
				
				continue;
			}
						
			// check if this edge was a pulse from flow meter 1
			if (absEdgeAmplitudeDelta >= FM1_PULSE_AMPLITUDE_DELTA_THRESHOLD)
			{
				// it was! create a pulse
				Pulse pulse = new Pulse(startSample, endSample, 1);
				pulsesList.add(pulse);
				
				continue;
			}
			
			// this edge was not a pulse
		}
		
		
		
		// back-fill samples for the next iteration
		// try to fill the entire array
		int backFillFromIndex = samples.length - backfilledSamples.length;
		
		// clamp to the number of samples available
		if (backFillFromIndex < 0)
			backFillFromIndex = 0;
		
		// don't back-fill samples for edges we have already checked
		if (backFillFromIndex < lastFoundEdgeEndIndex)
			backFillFromIndex = lastFoundEdgeEndIndex;
		
		int numToBackfill = samples.length - backFillFromIndex;
		
		// copy samples into the back-filled array
		for (int i = 0; i < numToBackfill; ++i)
			backfilledSamples[i] = samples[backFillFromIndex + i];
		
		numBackfilledSamples = numToBackfill;
		
		
		// return pulses
		return pulsesList.toArray(new Pulse[pulsesList.size()]);
	}
	
	/**
	 * Follows an edge starting at the given sample and returns where it ends.
	 * 
	 * @param samples                   - Audio samples to use.
	 * @param startIndex                - Index of the sample to start the edge from.
	 * @param maxEdgeDurationNumSamples - The maximum number of samples to follow an edge for.
	 * 
	 * @return The index of the last sample of the edge or -1 if no end was found. An end will
	 *         not be found if the duration of the edge exceeds <code>maxEdgeDurationNumSamples</code>
	 *         or if the end of the samples array is reached.
	 */
	private static int followEdge(AudioSample[] samples, int startIndex, int maxEdgeDurationNumSamples)
	{
		// follow an edge
		// an edge is a series of samples that all rise/fall in the same direction.
		// as soon as the direction changes, the edge ends, and a new one begins
		// example:
		
		// ---|-----|----|--|-|---|---
		//
		//         / \
		//        |   \
		// \     /     |
		//  \   /      |         / \
		//   \ /       |        /   |
		//             \   / \ /    \
		//              \ /          \
		//
		
		
		// direction of the edge: +1 = up, -1 = down, 0 = not yet set
		int edgeDirection = 0;
		
		for (int i = 1; i < maxEdgeDurationNumSamples; ++i)
		{
			int curentIndex = startIndex + i;
			
			// make sure we don't go outside the samples array
			if (curentIndex >= samples.length)
			{
				// we have reached the end of the samples array
				// we were unable to find an end to the edge
				return -1;
			}
			
			AudioSample currentSample  = samples[curentIndex];
			AudioSample previousSample = samples[curentIndex - 1];
			
			// get change in amplitude and direction between this sample and the last
			int imediateAmplitudeDelta = currentSample.amplitude - previousSample.amplitude;
			
			// if we did not move at all, we can't infer the direction
			if (imediateAmplitudeDelta == 0) {
				continue;
			}
			
			int imediateDirection = imediateAmplitudeDelta < 0? -1 : 1;
			
			// if the edge does not have a direction yet, set one
			if (edgeDirection == 0)
			{
				edgeDirection = imediateDirection;
				continue;
			}
			
			// check to see if we are still moving in the same direction
			if (imediateDirection != edgeDirection)
			{
				// we have found the edge's end
				return curentIndex - 1;
			}
		}
		
		// the duration of the edge has exceeded the max
		// we were unable to find an end to the edge
		return -1;
	}
}
