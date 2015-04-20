package net.awesomebox.flowMeterReader;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;



public class SignalVisualizer
{
	// ===================================================================
	// Constants
	//
	// ===================================================================
	
	private static final Color FM1_COLOR       = new Color(255, 0,   0);
	private static final Color FM1_LIGHT_COLOR = new Color(255, 128, 128);
	private static final Color FM2_COLOR       = new Color(0,   0,   255);
	private static final Color FM2_LIGHT_COLOR = new Color(128, 128, 255);
	
	
	
	// ===================================================================
	// Variables
	//
	// ===================================================================
	
	// size of the visualizer
	int visualizationWidth;
	int visualizationHeight;
	
	// the sample rate of all samples given
	private final int sampleRate;
	
	// how long to keep samples and pulses for in nanoseconds
	private final long dataStoreDurationNS;
	private final int dataStoreNumSamples;
	
	// if the view should update to show the latest sample when one is added
	private boolean tailing;
	
	// time of the newest sample when refresh was last called
	private AudioSample lastSampleTailed;
	
	
	// -------------------------------------------------------------------
	// visualization drawing params
	
	// pixel position of the Y origin (y=0) in the visualization
	private int visualizationYOrigin;
	
	// the number of pixels above and below the Y origin in the visualization
	private int visualizationYRange;
	
	// visualization drawing scales
	private double visualizationXScale = 1.0d/200000; // pixels per nanosecond. 1 pixel = 200000 nanoseconds
	private double visualizationYScale;
	
	// what decorations we should show
	private boolean showFM1PulseBoxes        = false;
	private boolean showFM2PulseBoxes        = false;
	private boolean showPulseAmplitudeDeltas = false;
	private boolean showGridLines            = false;
	
	
	// -------------------------------------------------------------------
	// visualization view variables
	
	// span of time that can be seen by the visualizer view
	private long visualizationViewTimeSpanNS;
	
	// the X position of the visualization view in regards to the data
	// meaning, data at this time appears at the far left side of the visualizer
	private long visualizationViewTimePositionNS = 0;
	
	private int visualizationViewFirstVisibleSampleIndex;
	private int visualizationViewLastVisibleSampleIndex;
	
	
	// -------------------------------------------------------------------
	// visualization data
	
	private ArrayList<AudioSample> samples = new ArrayList<AudioSample>();
	private ArrayList<Pulse>       pulses  = new ArrayList<Pulse>();
	
	private AudioSample oldestSample = null;
	private AudioSample newestSample = null;
	
	// keep track of some stats
	private long totalNumSamples   = 0;
	private long totalNumPulsesFM1 = 0;
	private long totalNumPulsesFM2 = 0;
	
	
	
	// ===================================================================
	// Methods
	//
	// ===================================================================
	
	/**
	 * Creates a utility for visualizing audio samples and pulses.
	 * 
	 * @param sampleRate - Sample rate. It is assumed that all samples given are <code>1/sampleRate</code>
	 *                     seconds apart.
	 */
	public SignalVisualizer(int sampleRate)
	{
		this(sampleRate, -1, false);
	}
	
	/**
	 * Creates a utility for visualizing audio samples and pulses.
	 * 
	 * @param sampleRate          - Sample rate. It is assumed that all samples given are <code>1/sampleRate</code>
	 *                              seconds apart.
	 * @param dataStoreDurationNS - How long samples and pulses are stored for in nanoseconds. Use <= 0 to keep forever.
	 * @param tailing             - If the view should update to show the latest samples as they are added.
	 */
	public SignalVisualizer(int sampleRate, long dataStoreDurationNS, boolean tailing)
	{
		this.sampleRate = sampleRate;
		this.dataStoreDurationNS = dataStoreDurationNS;
		this.tailing = tailing;
		
		// calculate the number of samples to store
		if (this.dataStoreDurationNS > 0)
			dataStoreNumSamples = (int)((sampleRate * dataStoreDurationNS) / (double)FlowMeterReader.NS_IN_S + 0.5d);
		else
			dataStoreNumSamples = 0;
		
		setVisualizationSize(100, 100);
	}
	
	
	
	
	
	
	public void setVisualizationSize(int width, int height)
	{
		this.visualizationWidth = width;
		this.visualizationHeight = height;
		
		// make y=0 the center of the visualization
		visualizationYOrigin = visualizationHeight / 2;
		
		// use the full range of the visualization with some padding
		visualizationYRange = (visualizationHeight / 2);
		
		// scale so the largest amplitude values use the Y full range
		// this way, the largest amplitude value will be at the very top of the visualization
		visualizationYScale = (double)visualizationYRange / AudioSample.AMPLITUDE_MAX_VALUE;
		
		// calculate the time span that can be seen by the visualizer
		recalculateVisualizationViewTimeSpanNS();
	}
	
	public int getVisualizationWidth()
	{
		return visualizationWidth;
	}
	public int getVisualizationHeight()
	{
		return visualizationHeight;
	}
	
	
	/**
	 * Sets the X-axis scale of the visualization.
	 * 
	 * @param visualizationXScale - Scale in pixels per nanosecond. EX:
	 *                              <code>1.0d/200000</code> means 1 pixel = 200000 nanoseconds
	 */
	public void setVisualizationXScale(double visualizationXScale)
	{
		this.visualizationXScale = visualizationXScale;
		
		recalculateVisualizationViewTimeSpanNS();
	}
	
	public double getVisualizationXScale()
	{
		return visualizationXScale;
	}
	
	public void toggleShowFM1PulseBoxes()
	{
		showFM1PulseBoxes = !showFM1PulseBoxes;
	}
	
	public void toggleShowFM2PulseBoxes()
	{
		showFM2PulseBoxes = !showFM2PulseBoxes;
	}
	
	public void toggleShowPulseAmplitudeDeltas()
	{
		showPulseAmplitudeDeltas = !showPulseAmplitudeDeltas;
	}
	
	public void toggleShowGridLines()
	{
		showGridLines = !showGridLines;
	}
	
	
	public void setVisualizationViewTimePositionNS(long visualizationViewTimePositionNS)
	{
		this.visualizationViewTimePositionNS = visualizationViewTimePositionNS;
		
		clampVisualizationViewTimePositionNS();
		
		// calculate what samples are visible
		recalculateVisualizationViewVisibleSamples();
	}
	
	public void clampVisualizationViewTimePositionNS()
	{
		// make sure our view stays within our samples
		if (oldestSample == null)
			return;
		
		if (visualizationViewTimePositionNS + visualizationViewTimeSpanNS > newestSample.timeNS)
			visualizationViewTimePositionNS = newestSample.timeNS - visualizationViewTimeSpanNS;
		
		if (visualizationViewTimePositionNS < oldestSample.timeNS)
			visualizationViewTimePositionNS = oldestSample.timeNS;
	}
	
	public AudioSample getNewestSample()
	{
		return newestSample;
	}
	
	public AudioSample getOldestSample()
	{
		return oldestSample;
	}
	
	public long getVisualizationViewTimeSpanNS()
	{
		return visualizationViewTimeSpanNS;
	}
	
	public long getVisualizationViewTimePositionNS()
	{
		return visualizationViewTimePositionNS;
	}
	

	/**
	 * Adds samples to the list of samples to be displayed.<br />
	 * <br />
	 * Make sure to call {@link #refresh} after adding samples.
	 * @see #refresh
	 * 
	 * @param samples - Samples to add.
	 */
	public void addSamples(AudioSample[] samples)
	{
		for (int i = 0; i < samples.length; ++i)
			this.samples.add(samples[i]);
		
		totalNumSamples += samples.length;
	}
	
	/**
	 * Adds pulses to the list of pulses to be displayed.
	 * 
	 * @param pulses - Pulses to add.
	 */
	public void addPulses(Pulse[] pulses)
	{
		for (int i = 0; i < pulses.length; ++i)
		{
			this.pulses.add(pulses[i]);
			
			if (pulses[i].flowMeterID == 1)
				++totalNumPulsesFM1;
			else
				++totalNumPulsesFM2;
		}
	}
	
	/**
	 * Updates the visualization and removes old data.<br />
	 * <br />
	 * This should be called after one or more samples are added via {@link #addRawSample} or {@link #addNormalizedSample}.
	 */
	public void refresh()
	{
		if (samples.size() == 0)
			return;
		
		// remove old samples
		removeOldSamples();
		
		// recalculate oldest and newest samples
		if (samples.size() == 0)
		{
			oldestSample = null;
			newestSample = null;
		}
		else
		{
			oldestSample = samples.get(0);
			newestSample = samples.get(samples.size() - 1);
		}
		
		// remove old pulses
		removeOldPulses();
		
		
		// check if we have any samples
		if (oldestSample == null)
		{
			recalculateVisualizationViewVisibleSamples();
			return;
		}
		
		
		if (tailing)
		{
			// tail samples
			tailSamples();
			// NOTE: we do not need to call recalculateVisualizationViewVisibleSamples because tailSamples calls it internally
		}
		else
		{
			// make sure our view does not go outside of our samples
			clampVisualizationViewTimePositionNS();
			
			// re-calculate what samples are visible
			recalculateVisualizationViewVisibleSamples();
		}
	}
	
	private void tailSamples()
	{
		if (newestSample == null)
			return;
		
		// check if we have already tailed this sample
		if (newestSample == lastSampleTailed)
			return;
		
		lastSampleTailed = newestSample;
		
		// update the view to show the newest sample
		visualizationViewTimePositionNS = newestSample.timeNS - visualizationViewTimeSpanNS;
		
		// re-calculate what samples are visible
		recalculateVisualizationViewVisibleSamples();
	}
	
	
	
	
	
	
	
	
	
	private void recalculateVisualizationViewTimeSpanNS()
	{
		// calculate the time span that can be seen by the visualizer
		visualizationViewTimeSpanNS = (long)(visualizationWidth / visualizationXScale + 0.5d);
		
		// make sure our view stays within our samples
		clampVisualizationViewTimePositionNS();
		
		recalculateVisualizationViewVisibleSamples();
	}
	
	private void recalculateVisualizationViewVisibleSamples()
	{
		if (samples.size() == 0)
		{
			visualizationViewFirstVisibleSampleIndex = -1;
			visualizationViewLastVisibleSampleIndex  = -1;
			return;
		}
		
		// find the first visible sample
		// get time from oldest sample to visualization view's left side
		long timeTillViewLeftNS = visualizationViewTimePositionNS - oldestSample.timeNS;
		
		// get the number of samples that would make up this time (rounded down)
		int numSamplesTillViewLeft = (int)((double)(timeTillViewLeftNS * sampleRate) / FlowMeterReader.NS_IN_S);
		
		// if it is negative, this means the oldest sample is past the left side of the visualization view
		if (numSamplesTillViewLeft < 0)
			numSamplesTillViewLeft = 0;
		
		// if this greater than the number of samples, then there are no samples that are past the left side of the visualization view
		if (numSamplesTillViewLeft > samples.size())
		{
			// if the there are no samples past the left side, there are no visible samples
			visualizationViewFirstVisibleSampleIndex = -1;
			visualizationViewLastVisibleSampleIndex  = -1;
			return;
		}
		
		int firstVisibleIndex = numSamplesTillViewLeft - 1;
		
		// if this is negative, that means it took 0 samples to get to the view's left side
		if (firstVisibleIndex < 0)
		{
			// the first visible sample is the first sample
			firstVisibleIndex = 0;
		}
		
		visualizationViewFirstVisibleSampleIndex = firstVisibleIndex;
		
		
		// find the last visible sample
		// get time from oldest sample to visualization view's right side
		long timeTillViewRightNS = (visualizationViewTimePositionNS + visualizationViewTimeSpanNS) - oldestSample.timeNS;
		
		// get the number of samples that would make up this time (rounded up)
		int numSamplesTillViewRight = (int)(((double)(timeTillViewRightNS * sampleRate) / FlowMeterReader.NS_IN_S) + 0.5d);
		
		// if it is negative, this means the oldest sample is past the right side of the visualization view
		if (numSamplesTillViewRight < 0)
		{
			// if the oldest sample is past the right side, there are no visible samples
			visualizationViewFirstVisibleSampleIndex = -1;
			visualizationViewLastVisibleSampleIndex  = -1;
			return;
		}
		
		// +1 to account for rounding errors
		++numSamplesTillViewRight;
		
		// if this greater than the number of samples, then their are no samples past the right side of the visualization view
		if (numSamplesTillViewRight > samples.size() - 1)
		{
			// use the last sample
			numSamplesTillViewRight = samples.size() - 1;
		}
		
		visualizationViewLastVisibleSampleIndex = numSamplesTillViewRight;
	}
	
	
	
	/**
	 * Removes all samples that can no longer be seen.
	 */
	private void removeOldSamples()
	{
		if (dataStoreNumSamples <= 0)
			return;
		
		// find the oldest sample to keep
		int oldestSampleToKeepIndex = samples.size() - dataStoreNumSamples;
		
		// check if we have anything to remove
		if (oldestSampleToKeepIndex < 1)
			return;
		
		// remove old samples
		samples.subList(0, oldestSampleToKeepIndex).clear();
	}
	
	/**
	 * Removes all pulses that can no longer be seen.
	 */
	private void removeOldPulses()
	{
		if (dataStoreNumSamples <= 0)
			return;
		
		if (samples.size() == 0)
			return;
		
		// a pulse can not be older than the oldest sample
		long oldestTimeToKeepNS = oldestSample.timeNS;
		
		// find the oldest pulse to keep
		int oldestPulseToKeepIndex = -1;
		for (int i = 0; i < pulses.size(); ++i)
		{
			// check if the end of the pulse is new enough to be kept
			if (pulses.get(i).endSample.timeNS >= oldestTimeToKeepNS)
			{
				oldestPulseToKeepIndex = i;
				break;
			}
		}
		
		// check if we found a pulse to keep
		if (oldestPulseToKeepIndex == -1)
		{
			// we didn't, they are all too old
			// remove them all
			pulses.clear();
			return;
		}
		
		int removeUpToIndex = oldestPulseToKeepIndex;
		
		// check if we have anything to remove
		if (removeUpToIndex < 1)
			return;
		
		// remove old pulses
		pulses.subList(0, removeUpToIndex).clear();
	}
	
	
	
	/**
	 * Draws the visualization.
	 * 
	 * @param g - Graphics to draw with.
	 */
	public void drawVisualization(Graphics g)
	{
		// start fresh
		g.clearRect(0, 0, visualizationWidth, visualizationHeight);
		
		// -------------------------------------------------------------------
		// guide lines
		
		if (showGridLines)
		{
			// draw amplitude lines at 10% intervals
			g.setColor(Color.LIGHT_GRAY);
			for (int i = 9; i > -10; --i)
			{
				if (i == 0)
					continue;
				
				int y = visualizationYOrigin - (int)((i / 10.0d) * visualizationYRange);
				g.drawLine(0, y, visualizationWidth, y);
			}
			
			
			// draw time lines
			// the optimal number of grid lines should fill the screen with 100 pixels between each
			int optimalNumLines = visualizationWidth / 100;
			
			// get the spacing between each gridline for the optimal 
			long lineTimeSpacingNS = visualizationViewTimeSpanNS / optimalNumLines;
			
			// round the spacing using the most significant digit
			int mul = 10;
			while (lineTimeSpacingNS/mul >= 10) // continue until we have only 1 digit
				mul *= 10;
			
			lineTimeSpacingNS = (int)Math.round((double)lineTimeSpacingNS / mul) * mul;
			
			// find the starting time using the spacing (rounded down)
			long firstLineTimesNS = ((long)((double)visualizationViewTimePositionNS / lineTimeSpacingNS)) * lineTimeSpacingNS;
			
			// get the number of lines to draw (rounded up) and an extra
			int numLines = (int)(((double)visualizationViewTimeSpanNS / lineTimeSpacingNS) + 0.5d) + 1;
			
			// get the unit to use
			String unit = null;
			long unitMul = 1l;
			
			if (lineTimeSpacingNS > FlowMeterReader.NS_IN_S / 10) // seconds
			{
				unit = "s";
				unitMul = FlowMeterReader.NS_IN_S;
			}
			else if (lineTimeSpacingNS > FlowMeterReader.NS_IN_MS / 100) // milliseconds
			{
				unit = "ms";
				unitMul = FlowMeterReader.NS_IN_MS;
			}
			
			for (int i = 0; i < numLines; ++i)
			{
				long time = firstLineTimesNS + i * lineTimeSpacingNS;
				int x = getXForTime(time);
				
				// draw lines
				g.setColor(Color.LIGHT_GRAY);
				g.drawLine(x, 0, x, visualizationHeight);
				
				// draw label
				g.setColor(Color.BLACK);
				
				// get displayTime
				String displayTime;
				if (unit == null)
					displayTime = time + "ns";
				else
					displayTime = ((time / (unitMul / 100)) / 100.0d) + unit; // round to two decimal places
				
				g.drawString(displayTime, x + 1, visualizationHeight + 15);
			}
		}
		
		// draw a line at the Y origin
		g.setColor(Color.GRAY);
		g.drawLine(
			0,                  visualizationYOrigin,
			visualizationWidth, visualizationYOrigin);
		
		// draw lines to show the amplitude range
		g.drawLine(
			0,                  visualizationYOrigin - visualizationYRange,
			visualizationWidth, visualizationYOrigin - visualizationYRange);
		g.drawLine(
			0,                  visualizationYOrigin + visualizationYRange,
			visualizationWidth, visualizationYOrigin + visualizationYRange);
		
		
		// -------------------------------------------------------------------
		// pulses
		
		for (int i = 0; i < pulses.size(); ++i)
		{
			Pulse pulse = pulses.get(i);
			
			// check if the pulse is visible
			if (pulse.endSample.timeNS < visualizationViewTimePositionNS)
				continue;
			if (pulse.startSample.timeNS > visualizationViewTimePositionNS + visualizationViewTimeSpanNS)
				break;
			
			g.setColor(pulse.flowMeterID == 1? FM1_LIGHT_COLOR : FM2_LIGHT_COLOR);
			
			int startX = getXForTime(pulse.startSample.timeNS);
			int startY = getYForAmplitude(pulse.startSample.amplitude);
			int endX   = getXForTime(pulse.endSample.timeNS);
			int endY   = getYForAmplitude(pulse.endSample.amplitude);
			
			int topY;
			int bottomY;
			
			if (startY < endY)
			{
				topY = startY;
				bottomY = endY;
			}
			else
			{
				topY = endY;
				bottomY = startY;
			}
			
			
			// draw boxes if enabled
			if ((pulse.flowMeterID == 1 && showFM1PulseBoxes) ||
				(pulse.flowMeterID == 2 && showFM2PulseBoxes))
			{
				// draw a box from the pulse's start time to end time
				g.fillRect(
					startX,
					topY,
					(endX - startX) + 1,
					(bottomY - topY) + 1);
				
				// draw box around pulses
				g.drawRect(
					startX - 5,
					topY - 5,
					(endX - startX) + 11,
					(bottomY - topY) + 11);
			}
			
			
			// draw amplitude delta if enabled
			if (showPulseAmplitudeDeltas)
			{
				g.setColor(pulse.flowMeterID == 1? FM1_COLOR : FM2_COLOR);
				
				double amplitudeDelta = (pulse.endSample.amplitude - pulse.startSample.amplitude) / (double)AudioSample.AMPLITUDE_MAX_VALUE;
				
				// round amplitude delta to 3 decimal places
				amplitudeDelta = (int)(amplitudeDelta * 1000) / 1000.0d;
				
				g.drawString(
					Double.toString(amplitudeDelta),
					startX - 5,
					bottomY + 20);
			}
		}
		
		
		// -------------------------------------------------------------------
		// samples
		
		if (visualizationViewFirstVisibleSampleIndex > -1)
		{
			g.setColor(Color.BLACK);
			for (int i = visualizationViewFirstVisibleSampleIndex + 1; i <= visualizationViewLastVisibleSampleIndex; ++i)
			{
				AudioSample sample         = samples.get(i);
				AudioSample previousSample = samples.get(i - 1);
				
				// draw a line from the previous sample to this one
				g.drawLine(
					getXForTime(previousSample.timeNS), getYForAmplitude(previousSample.amplitude),
					getXForTime(sample        .timeNS), getYForAmplitude(sample        .amplitude));
			}
		}
		
		
		// -------------------------------------------------------------------
		// guide line labels
		
		if (showGridLines)
		{
			int topY    = visualizationYOrigin - visualizationYRange - 10;
			int bottomY = visualizationYOrigin + visualizationYRange + 10;
			
			g.setColor(Color.LIGHT_GRAY);
			g.fillRect(0, topY, 30, bottomY - topY);
			
			g.setColor(Color.BLACK);
			for (int i = 10; i > -11; --i)
			{
				double ratio = i / 10.0d;
				int y = visualizationYOrigin - (int)(ratio * visualizationYRange);
				
				g.drawString(Double.toString(ratio), 3, y + 5);
			}
		}
		
		
		// -------------------------------------------------------------------
		// info
		
		// draw the scroll position
		g.setColor(Color.BLACK);
		g.drawString("Samples Processed: " + totalNumSamples,                   20, 30);
		g.drawString("Current Time: "      + visualizationViewTimePositionNS, 20, 45);
		
		g.setColor(FM1_COLOR);
		g.drawString("FM1 Pulses: "        + totalNumPulsesFM1,                 20, 60);
		
		g.setColor(FM2_COLOR);
		g.drawString("FM2 Pulses: "        + totalNumPulsesFM2,                 20, 75);
		
		
		g.setColor(Color.BLACK);
		g.drawString("W, S: Zoom",                       250, 30);
		g.drawString("A, D: Pan",                        250, 45);
		g.drawString("1, 2: Toggle Pulse Boxes",         250, 60);
		g.drawString("3: Toggle Pulse Amplitude Deltas", 250, 75);
		g.drawString("4: Toggle Grid Lines",             250, 90);
		
		
		g.setColor(FM1_COLOR);
		g.drawString("FM1 Pulse Amplitude Delta Threshold: " + FlowMeterReader.FM1_PULSE_AMPLITUDE_DELTA_THRESHOLD_RATIO + " (" + FlowMeterReader.FM1_PULSE_AMPLITUDE_DELTA_THRESHOLD + ")", 500, 30);
		
		g.setColor(FM2_COLOR);
		g.drawString("FM2 Pulse Amplitude Delta Threshold: " + FlowMeterReader.FM2_PULSE_AMPLITUDE_DELTA_THRESHOLD_RATIO + " (" + FlowMeterReader.FM2_PULSE_AMPLITUDE_DELTA_THRESHOLD + ")", 500, 45);
	}
	
	/**
	 * Translates the given time into an X position on the visualization using the
	 * current time position and X scale.
	 * 
	 * @param time - Time to translate.
	 * 
	 * @return X position on the visualization.
	 */
	private int getXForTime(long timeNS)
	{
		return (int)((timeNS - visualizationViewTimePositionNS) * visualizationXScale);
	}
	
	/**
	 * Translates the given amplitude into a Y position on the visualization using the
	 * Y origin and Y scale.
	 * 
	 * @param amplitude - Amplitude to translate.
	 * 
	 * @return Y position on the visualization.
	 */
	private int getYForAmplitude(short amplitude)
	{
		return (int)(visualizationYOrigin - amplitude * visualizationYScale);
	}
}
