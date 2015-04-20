package net.awesomebox.flowMeterReader.signalVisualizer;

import java.awt.Graphics;
import java.util.ArrayList;

import net.awesomebox.flowMeterReader.AudioSample;
import net.awesomebox.flowMeterReader.FlowMeterReader;
import net.awesomebox.flowMeterReader.Pulse;



public class SignalVisualizer
{
	// ===================================================================
	// Constants
	//
	// ===================================================================
	
	public static final int VISUALIZATION_PADDING_TOP = 15;
	public static final int VISUALIZATION_PADDING_BOTTOM = 15;
	
	
	// ===================================================================
	// Variables
	//
	// ===================================================================
	
	// size of the visualizer
	private int visualizationWidth;
	private int visualizationHeight;
	
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
	
	private final SignalVisualizerDrawer drawer = new SignalVisualizerDrawer();
	
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
	
	// the index of the first and last samples visible in the view
	private int visualizationViewFirstVisibleSampleIndex;
	private int visualizationViewLastVisibleSampleIndex;
	
	
	// -------------------------------------------------------------------
	// visualization data
	
	private ArrayList<AudioSample> samples = new ArrayList<AudioSample>();
	private ArrayList<Pulse>       pulses  = new ArrayList<Pulse>();
	
	// the newest and oldest samples
	private AudioSample oldestSample = null;
	private AudioSample newestSample = null;
	
	// keep track of some stats
	private long totalNumSamples   = 0;
	private long totalNumPulsesFM1 = 0;
	private long totalNumPulsesFM2 = 0;
	
	
	
	// ===================================================================
	// Constructor
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
	
	
	
	// ===================================================================
	// Getters & Setters
	//
	// ===================================================================
	
	// -------------------------------------------------------------------
	// visualization size
	
	public void setVisualizationSize(int width, int height)
	{
		this.visualizationWidth = width;
		this.visualizationHeight = height;
		
		// the height to draw the samples in
		int visualizationRangeHeight = height - (VISUALIZATION_PADDING_TOP + VISUALIZATION_PADDING_BOTTOM);
		
		// make y=0 the center of the visualization
		visualizationYOrigin = VISUALIZATION_PADDING_TOP + visualizationRangeHeight / 2;
		
		// use the full range of the visualization with some padding
		visualizationYRange = (visualizationRangeHeight / 2);
		
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
	
	
	// -------------------------------------------------------------------
	// visualization x scale
	
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
	
	
	// -------------------------------------------------------------------
	// visualization view
	
	public void setVisualizationViewTimePositionNS(long visualizationViewTimePositionNS)
	{
		this.visualizationViewTimePositionNS = visualizationViewTimePositionNS;
		
		clampVisualizationViewTimePositionNS();
		
		// calculate what samples are visible
		recalculateVisualizationViewVisibleSamples();
	}
	
	public long getVisualizationViewTimePositionNS()
	{
		return visualizationViewTimePositionNS;
	}
	
	public long getVisualizationViewTimeSpanNS()
	{
		return visualizationViewTimeSpanNS;
	}
	
	
	// -------------------------------------------------------------------
	// samples
	
	public AudioSample getNewestSample()
	{
		return newestSample;
	}
	
	public AudioSample getOldestSample()
	{
		return oldestSample;
	}
	
	public long getTotalNumSamples()
	{
		return totalNumSamples;
	}
	
	
	// -------------------------------------------------------------------
	// pulses
	
	public long getTotalNumPulsesFM1()
	{
		return totalNumPulsesFM1;
	}
	
	public long getTotalNumPulsesFM2()
	{
		return totalNumPulsesFM2;
	}
	
	
	// -------------------------------------------------------------------
	// drawing decorations
	
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
	
	
	
	// ===================================================================
	// Public Interface
	//
	// ===================================================================
	
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
	
	
	
	// ===================================================================
	// Private Methods
	//
	// ===================================================================
	
	// -------------------------------------------------------------------
	// removing old data
	
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
	
	
	// -------------------------------------------------------------------
	// recalculating visualization variables
	
	/**
	 * Calculates the time span that can be seen by the visualizer view
	 * and updates effected variables.
	 */
	private void recalculateVisualizationViewTimeSpanNS()
	{
		// calculate the time span that can be seen by the visualizer view
		visualizationViewTimeSpanNS = (long)(visualizationWidth / visualizationXScale + 0.5d);
		
		// make sure our view stays within our samples
		clampVisualizationViewTimePositionNS();
		
		recalculateVisualizationViewVisibleSamples();
	}
	
	/**
	 * Calculates the samples that can be seen by the visualizer view
	 * and updates effected variables.
	 */
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
	 * Ensures the view is not positioned outside of the samples.
	 */
	private void clampVisualizationViewTimePositionNS()
	{
		// make sure our view stays within our samples
		if (oldestSample == null)
			return;
		
		if (visualizationViewTimePositionNS + visualizationViewTimeSpanNS > newestSample.timeNS)
			visualizationViewTimePositionNS = newestSample.timeNS - visualizationViewTimeSpanNS;
		
		if (visualizationViewTimePositionNS < oldestSample.timeNS)
			visualizationViewTimePositionNS = oldestSample.timeNS;
	}
	
	
	// -------------------------------------------------------------------
	// other
	
	/**
	 * Updates the view's position so it shows the latest sample when one
	 * is added.
	 */
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
	
	
	
	// ===================================================================
	// Drawing
	//
	// ===================================================================
	
	/**
	 * Draws the visualization.
	 * 
	 * @param g - Graphics to draw with.
	 */
	public void drawVisualization(Graphics g)
	{
		// start fresh
		g.clearRect(0, 0, visualizationWidth, visualizationHeight);
		
		// update the drawer
		drawer.updateVisualizationSize(
			visualizationWidth,
			visualizationHeight
		);
		drawer.updateVisualizationXAxis(visualizationXScale);
		drawer.updateVisualizationYAxis(
			visualizationYOrigin,
			visualizationYRange,
			visualizationYScale
		);
		drawer.updateVisualizationView(
			visualizationViewTimePositionNS,
			visualizationViewTimeSpanNS,
			visualizationViewFirstVisibleSampleIndex,
			visualizationViewLastVisibleSampleIndex
		);
		
		// draw
		if (showGridLines)
			drawer.drawGridLines(g);
		
		drawer.drawBoundaryLines(g);
		drawer.drawPulses(pulses, showFM1PulseBoxes, showFM2PulseBoxes, showPulseAmplitudeDeltas, g);
		drawer.drawSamples(samples, g);
		
		if (showGridLines)
			drawer.drawGridLineLables(g);
	}
}
