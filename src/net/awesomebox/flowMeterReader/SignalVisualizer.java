package net.awesomebox.flowMeterReader;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;


public class SignalVisualizer
{
	// ===================================================================
	// Constants
	//
	// ===================================================================
	
	// starting screen size
	private static final int STARTING_VISUALIZER_WIDTH  = 1200;
	private static final int STARTING_VISUALIZER_HEIGHT = 500;
	
	private static final int VISUALIZATION_TOP_PADDING    = 120;
	private static final int VISUALIZATION_BOTTOM_PADDING = 40;
	private static final int VISUALIZATION_MIN_HEIGHT     = 100;
	
	static final double MIN_VISUALIZATION_X_SCALE = 1.0d/20000000; // pixels per nanosecond. 1 pixel = 20000000 nanoseconds
	static final double MAX_VISUALIZATION_X_SCALE = 1.0d/50;       // pixels per nanosecond. 1 pixel = 50 nanoseconds
	
	// The scroll bar uses ints and our times are in nanoseconds.
	// This means we will only be able to span 2 seconds. Scaling
	// the values down before converting to ints will allow use to
	// operate longer before overflowing
	private static final double SCROLL_BAR_SCALE = 0.0005d;
	
	public static final double ZOOM_RATE = 1.5d;
	public static final int PAN_SPEED = 100;
	
	public static final Color FM1_COLOR       = new Color(255, 0,   0);
	public static final Color FM1_LIGHT_COLOR = new Color(255, 128, 128);
	public static final Color FM2_COLOR       = new Color(0,   0,   255);
	public static final Color FM2_LIGHT_COLOR = new Color(128, 128, 255);
		
	
	// ===================================================================
	// Variables
	//
	// ===================================================================
	
	// if the streamer should pause
	boolean pauseStreamer = false;
	public boolean shouldPause() { return pauseStreamer; }
	
	// the sample rate of all samples given
	private final int sampleRate;
	
	// if only the latest samples should be shown (true) or if all samples
	// should be shown in a scroll view (false).
	private final boolean onlyShowLatest;
	
	
	// -------------------------------------------------------------------
	// window elements
	
	SingalVisualizerPanel visualizerPanel;
	JScrollBar scrollBar;
	
	
	// -------------------------------------------------------------------
	// visualization drawing params
	
	// pixel position of the Y origin (y=0) in the visualization
	private int visualizationYOrigin;
	
	// the number of pixels above and below the Y origin in the visualization
	private int visualizationYRange;
	
	// visualization drawing scales
	double visualizationXScale = 1.0d/200000; // pixels per nanosecond. 1 pixel = 200000 nanoseconds
	double visualizationYScale;
	
	// what decorations we should show
	boolean showFM1PulseBoxes        = false;
	boolean showFM2PulseBoxes        = false;
	boolean showPulseAmplitudeDeltas = false;
	boolean showGridLines            = false;
	
	
	// -------------------------------------------------------------------
	// visualization window variables
	
	// span of time that can be seen by the visualizer window
	private long visualizationWindowTimeSpanNS;
	
	// the X position of the visualization window in regards to the data
	// meaning, data at this time appears at the far left side of the visualizer
	long visualizationWindowTimePositionNS = 0;
	
	private int visualizationWindowFirstVisibleSampleIndex;
	private int visualizationWindowLastVisibleSampleIndex;
	
	
	// -------------------------------------------------------------------
	// visualization data
	
	private ArrayList<AudioSample> samples = new ArrayList<AudioSample>();
	private ArrayList<Pulse>       pulses  = new ArrayList<Pulse>();
	
	// keep track of some stats
	private long totalNumSamples   = 0;
	private long totalNumPulsesFM1 = 0;
	private long totalNumPulsesFM2 = 0;
	
	
	
	// ===================================================================
	// Private Classes
	//
	// ===================================================================
	
	/**
	 * Custom panel for drawing our audio visualization.
	 */
	class SingalVisualizerPanel extends JPanel
	{
		private static final long serialVersionUID = 1l;
		
		public SingalVisualizerPanel()
		{
			super();
		}
		
		@Override
		public void paint(Graphics g)
		{
			drawVisualization(g, this.getWidth(), this.getHeight());
		}
	}
	
	
	
	// ===================================================================
	// Methods
	//
	// ===================================================================
	
	/**
	 * Creates a window for visualizing audio samples and pulses.
	 * 
	 * @param sampleRate     - Sample rate. It is assumed that all samples given are <code>1/sampleRate</code>
	 *                         seconds apart.
	 * @param onlyShowLatest - if only the latest samples should be shown (<code>true</code>) or
	 *                         if all samples should be shown in a scroll view (<code>false</code>).
	 */
	public SignalVisualizer(int sampleRate, boolean onlyShowLatest)
	{
		this.sampleRate = sampleRate;
		this.onlyShowLatest = onlyShowLatest;
		
		// I know very little about Java windows, so the bellow code is probably terrible...
		// please let me know how I can improve it.
		
		// create the frame
		final JFrame frame = new JFrame("AudioVisualizer");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		
		// add a listener to know when the size changes
		frame.addComponentListener(new ComponentListener()
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				// resize the visualizer
				resizeVisualizer();
			}
			
			@Override public void componentHidden(ComponentEvent e) {}
			@Override public void componentMoved(ComponentEvent e) {}
			@Override public void componentShown(ComponentEvent e) {}
		});
		
		
		// create the panel
		visualizerPanel = new SingalVisualizerPanel();
		
		// add key bindings
		SignalVisualizerKeyBindings.attachKeyBindings(this);
		
		// set the preferred size to the max possible visualization size
		visualizerPanel.setPreferredSize(new Dimension(
			STARTING_VISUALIZER_WIDTH,
			STARTING_VISUALIZER_HEIGHT
		));
		frame.add(visualizerPanel, java.awt.BorderLayout.CENTER);
		
		
		// create scroll bar
		if (!onlyShowLatest)
		{
			scrollBar = new JScrollBar(JScrollBar.HORIZONTAL);
			
			// allow extent to be set before max is
			scrollBar.getModel().setMaximum(Integer.MAX_VALUE);
			
			// attach scroll listener
			scrollBar.addAdjustmentListener(new AdjustmentListener()
			{
				@Override
				public void adjustmentValueChanged(AdjustmentEvent e)
				{
					// set visualization window position
					visualizationWindowTimePositionNS = (long)(scrollBar.getModel().getValue() / SCROLL_BAR_SCALE);
					recalculateVisualizationWindowVisibleSamples();
					
					visualizerPanel.repaint();
				}
			});
			
			frame.add(scrollBar, java.awt.BorderLayout.SOUTH);
		}
		
		// finalize and display
		frame.pack();
		frame.setVisible(true);
		
		// resize the visualizer
		resizeVisualizer();
	}
	
	
	/**
	 * Updates the visualizer params to fit the frame.
	 * 
	 * @param frame - Frame to fit to.
	 */
	synchronized void resizeVisualizer()
	{
		int visualizationHeight = Math.max(
			VISUALIZATION_MIN_HEIGHT, 
			visualizerPanel.getHeight() - (VISUALIZATION_TOP_PADDING + VISUALIZATION_BOTTOM_PADDING) // fill the panel
		);
		
		// make y=0 the center of the visualization
		visualizationYOrigin = VISUALIZATION_TOP_PADDING + visualizationHeight / 2;
		
		// use the full range of the visualization with some padding
		visualizationYRange = (visualizationHeight / 2);
		
		// scale so the largest amplitude values use the Y full range
		// this way, the largest amplitude value will be at the very top of the visualization
		visualizationYScale = (double)visualizationYRange / AudioSample.AMPLITUDE_MAX_VALUE;
		
		// calculate the time span that can be seen by the visualizer
		recalculateVisualizationWindowTimeSpanNS(false);
	}
	
	synchronized void recalculateVisualizationWindowTimeSpanNS(boolean keepScrollBarCentered)
	{
		// calculate the time span that can be seen by the visualizer
		visualizationWindowTimeSpanNS = (long)(visualizerPanel.getWidth() / visualizationXScale + 0.5d);
		
		recalculateVisualizationWindowVisibleSamples();
		
		// update the scroll bar if we have one
		if (scrollBar != null)
			setScrollBarWindowTimeSpan(visualizationWindowTimeSpanNS, keepScrollBarCentered);
	}
	
	synchronized void recalculateVisualizationWindowVisibleSamples()
	{
		if (samples.size() == 0)
		{
			visualizationWindowFirstVisibleSampleIndex = -1;
			visualizationWindowLastVisibleSampleIndex  = -1;
			return;
		}
		
		// get oldest sample
		AudioSample oldestSample = samples.get(0);
		
		
		// find the first visible sample
		// get time from oldest sample to visualization window's left side
		long timeTillWindowLeftNS = visualizationWindowTimePositionNS - oldestSample.timeNS;
		
		// get the number of samples that would make up this time (rounded down)
		int numSamplesTillWindowLeft = (int)((double)(timeTillWindowLeftNS * sampleRate) / FlowMeterReader.NS_IN_S);
		
		// if it is negative, this means the oldest sample is past the left side of the visualization window
		if (numSamplesTillWindowLeft < 0)
			numSamplesTillWindowLeft = 0;
		
		// if this greater than the number of samples, then there are no samples that are past the left side of the visualization window
		if (numSamplesTillWindowLeft > samples.size())
		{
			// if the there are no samples past the left side, there are no visible samples
			visualizationWindowFirstVisibleSampleIndex = -1;
			visualizationWindowLastVisibleSampleIndex  = -1;
			return;
		}
		
		int firstVisibleIndex = numSamplesTillWindowLeft - 1;
		
		// if this is negative, that means it took 0 samples to get to the window's left side
		if (firstVisibleIndex < 0)
		{
			// the first visible sample is the first sample
			firstVisibleIndex = 0;
		}
		
		visualizationWindowFirstVisibleSampleIndex = firstVisibleIndex;
		
		
		// find the last visible sample
		// get time from oldest sample to visualization window's right side
		long timeTillWindowRightNS = (visualizationWindowTimePositionNS + visualizationWindowTimeSpanNS) - oldestSample.timeNS;
		
		// get the number of samples that would make up this time (rounded up)
		int numSamplesTillWindowRight = (int)(((double)(timeTillWindowRightNS * sampleRate) / FlowMeterReader.NS_IN_S) + 0.5d);
		
		// if it is negative, this means the oldest sample is past the right side of the visualization window
		if (numSamplesTillWindowRight < 0)
		{
			// if the oldest sample is past the right side, there are no visible samples
			visualizationWindowFirstVisibleSampleIndex = -1;
			visualizationWindowLastVisibleSampleIndex  = -1;
			return;
		}
		
		// +1 to account for rounding errors
		++numSamplesTillWindowRight;
		
		// if this greater than the number of samples, then their are no samples past the right side of the visualization window
		if (numSamplesTillWindowRight > samples.size() - 1)
		{
			// use the last sample
			numSamplesTillWindowRight = samples.size() - 1;
		}
		
		visualizationWindowLastVisibleSampleIndex = numSamplesTillWindowRight;
	}
	
	void updateScrollBar(int value, int extent, int min, int max)
	{
		if (extent > max - min)
			extent = max - min;
		
		if (value < min)
			value = min;
		
		if (value + extent > max)
			value = max - extent;
		
		scrollBar.getModel().setRangeProperties(value, extent, min, max, scrollBar.getModel().getValueIsAdjusting());
	}
	
	void setScrollBarTimeSpan(long startTimeSpan, long endTimeSpan)
	{
		updateScrollBar(
			scrollBar.getModel().getValue(),
			scrollBar.getModel().getExtent(),
			(int)(startTimeSpan * SCROLL_BAR_SCALE),
			(int)(endTimeSpan   * SCROLL_BAR_SCALE)
		);
	}
	
	void setScrollBarWindowTimeSpan(long windowTimeSpanNS, boolean keepCentered)
	{
		int value  = scrollBar.getModel().getValue();
		int extent = (int)(windowTimeSpanNS * SCROLL_BAR_SCALE);
		
		if (keepCentered)
		{
			int oldExtent = scrollBar.getModel().getExtent();
			int scrollBarCenter = value + oldExtent / 2;
			
			value = scrollBarCenter - extent / 2;
		}
		
		updateScrollBar(
			value,
			extent,
			scrollBar.getModel().getMinimum(),
			scrollBar.getModel().getMaximum()
		);
		
		scrollBar.setBlockIncrement(extent / 2);
		scrollBar.setUnitIncrement(extent / 10);
	}
	
	
	/**
	 * Adds samples to the list of samples to be displayed.<br />
	 * <br />
	 * Make sure to call {@link #refresh} after adding samples.
	 * @see #refresh
	 * 
	 * @param samples - Samples to add.
	 */
	public synchronized void addSamples(AudioSample[] samples)
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
	public synchronized void addPulses(Pulse[] pulses)
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
	 * Resets the display and removes old data.<br />
	 * <br />
	 * This should be called after one or more samples are added via {@link #addRawSample} or {@link #addNormalizedSample}.
	 */
	public synchronized void refresh()
	{
		if (samples.size() > 0)
		{
			if (onlyShowLatest)
			{
				// get the oldest time that can be seen based on the newest sample and the visualization window
				AudioSample newestSample = samples.get(samples.size() - 1);
				long oldestVisibleTimeNS = newestSample.timeNS - visualizationWindowTimeSpanNS;
				
				// remove old data
				removeOldSamples(oldestVisibleTimeNS);
				removeOldPulses(oldestVisibleTimeNS);
				
				// set the visualizer window's time position to the oldest sample's time
				AudioSample oldestSample = samples.get(0);
				visualizationWindowTimePositionNS = oldestSample.timeNS;
			}
			else
			{
				// update the scroll bar
				AudioSample newestSample = samples.get(samples.size() - 1);
				AudioSample oldestSample = samples.get(0);
				
				setScrollBarTimeSpan(oldestSample.timeNS, newestSample.timeNS);
			}
		}
		
		// redraw
		visualizerPanel.repaint();
	}
	
	
	/**
	 * Removes all samples that can no longer be seen.
	 */
	private void removeOldSamples(long oldestVisibleTimeNS)
	{
		// find the oldest visible sample
		int oldestVisibleSampleIndex = -1;
		for (int i = 0; i < samples.size(); ++i)
		{
			// check if the sample is new enough to be visible
			if (samples.get(i).timeNS >= oldestVisibleTimeNS)
			{
				oldestVisibleSampleIndex = i;
				break;
			}
		}
		
		// check if we found a visible sample
		if (oldestVisibleSampleIndex == -1)
		{
			// we didn't, they are all too old
			// remove them all
			samples.clear();
			return;
		}
		
		// don't remove the sample before the oldest visible sample so
		// we can still draw a line between them
		int removeUpToIndex = oldestVisibleSampleIndex - 1;
		
		// check if we have anything to remove
		if (removeUpToIndex < 1)
			return;
		
		// remove old samples
		samples.subList(0, removeUpToIndex).clear();
	}
	
	/**
	 * Removes all pulses that can no longer be seen.
	 */
	private void removeOldPulses(long oldestVisibleTimeNS)
	{
		// find the oldest visible pulse
		int oldestVisiblePulseIndex = -1;
		for (int i = 0; i < pulses.size(); ++i)
		{
			// check if the end of the pulse is new enough to be visible
			if (pulses.get(i).endSample.timeNS >= oldestVisibleTimeNS)
			{
				oldestVisiblePulseIndex = i;
				break;
			}
		}
		
		// check if we found a visible pulse
		if (oldestVisiblePulseIndex == -1)
		{
			// we didn't, they are all too old
			// remove them all
			pulses.clear();
			return;
		}
		
		int removeUpToIndex = oldestVisiblePulseIndex;
		
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
	public synchronized void drawVisualization(Graphics g, int width, int height)
	{
		// start fresh
		g.clearRect(0, 0, width, height);
		
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
				g.drawLine(0, y, width, y);
			}
			
			
			// draw time lines
			// the optimal number of grid lines should fill the screen with 100 pixels between each
			int optimalNumLines = width / 100;
			
			// get the spacing between each gridline for the optimal 
			long lineTimeSpacingNS = visualizationWindowTimeSpanNS / optimalNumLines;
			
			// round the spacing using the most significant digit
			int mul = 10;
			while (lineTimeSpacingNS/mul >= 10) // continue until we have only 1 digit
				mul *= 10;
			
			lineTimeSpacingNS = (int)Math.round((double)lineTimeSpacingNS / mul) * mul;
			
			// find the starting time using the spacing (rounded down)
			long firstLineTimesNS = ((long)((double)visualizationWindowTimePositionNS / lineTimeSpacingNS)) * lineTimeSpacingNS;
			
			// get the number of lines to draw (rounded up) and an extra
			int numLines = (int)(((double)visualizationWindowTimeSpanNS / lineTimeSpacingNS) + 0.5d) + 1;
			
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
				g.drawLine(x, VISUALIZATION_TOP_PADDING, x, height);
				
				// draw label
				g.setColor(Color.BLACK);
				
				// get displayTime
				String displayTime;
				if (unit == null)
					displayTime = time + "ns";
				else
					displayTime = ((time / (unitMul / 100)) / 100.0d) + unit; // round to two decimal places
				
				g.drawString(displayTime, x + 1, height - VISUALIZATION_BOTTOM_PADDING + 15);
			}
		}
		
		// draw a line at the Y origin
		g.setColor(Color.GRAY);
		g.drawLine(
			0,     visualizationYOrigin,
			width, visualizationYOrigin);
		
		// draw lines to show the amplitude range
		g.drawLine(
			0,     visualizationYOrigin - visualizationYRange,
			width, visualizationYOrigin - visualizationYRange);
		g.drawLine(
			0,     visualizationYOrigin + visualizationYRange,
			width, visualizationYOrigin + visualizationYRange);
		
		
		// -------------------------------------------------------------------
		// pulses
		
		for (int i = 0; i < pulses.size(); ++i)
		{
			Pulse pulse = pulses.get(i);
			
			// check if the pulse is visible
			if (pulse.endSample.timeNS < visualizationWindowTimePositionNS)
				continue;
			if (pulse.startSample.timeNS > visualizationWindowTimePositionNS + visualizationWindowTimeSpanNS)
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
				
				g.drawString(
					Integer.toString(pulse.endSample.amplitude - pulse.startSample.amplitude),
					startX - 5,
					bottomY + 20);
			}
		}
		
		
		// -------------------------------------------------------------------
		// samples
		
		if (visualizationWindowFirstVisibleSampleIndex > -1)
		{
			g.setColor(Color.BLACK);
			for (int i = visualizationWindowFirstVisibleSampleIndex + 1; i <= visualizationWindowLastVisibleSampleIndex; ++i)
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
		g.drawString("Current Time: "      + visualizationWindowTimePositionNS, 20, 45);
		
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
		return (int)((timeNS - visualizationWindowTimePositionNS) * visualizationXScale);
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
