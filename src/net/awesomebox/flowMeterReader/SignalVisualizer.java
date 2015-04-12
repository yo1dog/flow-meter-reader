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
import javax.swing.JScrollPane;


public class SignalVisualizer
{
	// ===================================================================
	// Constants
	//
	// ===================================================================
	
	// starting screen size
	private static final int STARTING_VISUALIZER_WIDTH  = 1200;
	private static final int STARTING_VISUALIZER_HEIGHT = 500;
	
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
	public boolean shouldPause() { return pauseStreamer;}
	
	boolean showFM1PulseBoxes = false;
	boolean showFM2PulseBoxes = false;
	boolean showPulseAmplitudeDeltas = false;
	boolean showGridLines = false;
	
	
	// -------------------------------------------------------------------
	// visualization params
	
	// pixel width of the visualizer
	private int visualizerWidth;
	
	// pixel position of the Y origin (y=0) in the visualization
	private int visualizationYOrigin;
	
	// the number of pixels above and below the Y origin in the visualization
	private int visualizationYRange;
	
	// visualization drawing scales
	double visualizationXScale = 1.0d/200000; // pixels per nanosecond. 1 pixel = 200000 nanoseconds
	private double visualizationYScale;
	
	// span of time that can be seen by the visualizer
	private long visualizationTimeSpanNS;
	
	// how many pixels wide all the stored samples are when displayed in the visualization
	int dataDisplayedWidth;
	
	
	// the X position of the visualization in regards to the data
	// meaning, data at this time appears at the far left side of the visualizer
	private long visualizationTimePositionNS = 0;
	
	// if only the latest samples should be shown (true) or if all samples
	// should be shown in a scroll view (false).
	private final boolean onlyShowLatest;
	
	
	// -------------------------------------------------------------------
	// window elements
	
	SingalVisualizerPanel panel;
	JScrollPane scroller;
	
	int scrollPosX = 0;
	
	
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
			drawVisualization(this, g);
		}
	}
	
	
	
	// ===================================================================
	// Methods
	//
	// ===================================================================
	
	/**
	 * Creates a window for visualizing audio samples and pulses.
	 * 
	 * @param onlyShowLatest - if only the latest samples should be shown (<code>true</code>) or
	 *                         if all samples should be shown in a scroll view (<code>false</code>).
	 */
	public SignalVisualizer(boolean onlyShowLatest)
	{
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
				// resize the visualizer to match the frame
				resizeVisualizer(frame);
			}
			
			@Override public void componentHidden(ComponentEvent e) {}
			@Override public void componentMoved(ComponentEvent e) {}
			@Override public void componentShown(ComponentEvent e) {}
		});
		
		
		// create the panel
		panel = new SingalVisualizerPanel();
		
		// add key bindings
		SignalVisualizerKeyBindings.attachKeyBindings(this);
		
		// set the preferred size to the max possible visualization size
		panel.setPreferredSize(new Dimension(
			STARTING_VISUALIZER_WIDTH,
			STARTING_VISUALIZER_HEIGHT
		));
		frame.add(panel, java.awt.BorderLayout.CENTER);
		
		// create the scroll pane
		if (!onlyShowLatest)
		{
			scroller = new JScrollPane(panel);
			scroller.getHorizontalScrollBar().addAdjustmentListener(new AdjustmentListener()
			{
				// on scroll, record the x position
				@Override
				public void adjustmentValueChanged(AdjustmentEvent e)
				{
					scrollPosX = e.getValue();
				}
			});
			frame.add(scroller, java.awt.BorderLayout.CENTER);
		}
		
		// finalize and display
		frame.pack();
		frame.setVisible(true);
		
		// resize the visualizer to match the frame
		resizeVisualizer(frame);
	}
	
	
	/**
	 * Updates the visualizer params to fit the frame.
	 * 
	 * @param frame - Frame to fit to.
	 */
	synchronized void resizeVisualizer(JFrame frame)
	{
		// make the visualizer the same width as the frame
		visualizerWidth = frame.getWidth();
		
		// make y=0 the center of the frame
		visualizationYOrigin = frame.getHeight() / 2;
		
		// use the full frame height with some padding
		visualizationYRange = frame.getHeight() / 2 - 100;
		
		// scale so the largest amplitude values use the Y full range
		// this way, the largest amplitude value will be at the very top of the visualization
		visualizationYScale = (double)visualizationYRange / AudioSample.AMPLITUDE_MAX_VALUE;
		
		// calculate the time span that can be seen by the visualizer
		recalculateVisualizationTimeSpanNS();
	}
	
	void recalculateVisualizationTimeSpanNS()
	{
		// calculate the time span that can be seen by the visualizer
		visualizationTimeSpanNS = (long)(visualizerWidth / visualizationXScale + 0.5d);
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
			AudioSample newestSample = samples.get(samples.size() - 1);
			
			if (onlyShowLatest)
			{
				// get the oldest time that can be seen based on the newest sample
				long oldestVisibleTimeNS = newestSample.timeNS - visualizationTimeSpanNS;
				
				// remove old data
				removeOldSamples(oldestVisibleTimeNS);
				removeOldPulses(oldestVisibleTimeNS);
			}
			
			AudioSample oldestSample = samples.get(0);
			
			// set the visualizer's time position to the oldest sample's time
			visualizationTimePositionNS = oldestSample.timeNS;
			
			// set the visualization width based on the current time span
			long sampleTimeSpan = newestSample.timeNS - oldestSample.timeNS;
			dataDisplayedWidth = (int)(sampleTimeSpan * visualizationXScale + 0.5d);
			
			if (!onlyShowLatest)
			{
				panel.setPreferredSize(new Dimension(
					dataDisplayedWidth,
					panel.getHeight()
				));
			}
		}
		
		// redraw
		if (scroller != null)
			scroller.repaint();
		else
			panel.repaint();
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
	public synchronized void drawVisualization(JPanel panel, Graphics g)
	{
		// start fresh
		g.clearRect(0, 0, panel.getWidth(), panel.getHeight());
		
		// -------------------------------------------------------------------
		// guide lines
		
		// draw a line at the Y origin
		g.setColor(Color.GRAY);
		g.drawLine(
			0,                  visualizationYOrigin,
			dataDisplayedWidth, visualizationYOrigin);
		g.drawLine(
			0,                  visualizationYOrigin - visualizationYRange,
			dataDisplayedWidth, visualizationYOrigin - visualizationYRange);
		g.drawLine(
			0,                  visualizationYOrigin + visualizationYRange,
			dataDisplayedWidth, visualizationYOrigin + visualizationYRange);
		
		
		if (showGridLines)
		{
			g.setColor(Color.LIGHT_GRAY);
			for (int i = 9; i > -10; --i)
			{
				if (i == 0)
					continue;
				
				int y = visualizationYOrigin - (int)((i / 10.0d) * visualizationYRange);
				g.drawLine(0, y, dataDisplayedWidth, y);
			}
		}
		
		
		// -------------------------------------------------------------------
		// pulses
		
		for (int i = 0; i < pulses.size(); ++i)
		{
			Pulse pulse = pulses.get(i);
			
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
			
			
			if ((pulse.flowMeterID == 1 && showFM1PulseBoxes) ||
				(pulse.flowMeterID == 2 && showFM2PulseBoxes))
			{
				// draw a box from the pulse's start time to end time
				g.fillRect(
					startX,
					topY,
					(endX - startX) + 1,
					(bottomY - topY) + 1);
				
				// draw boxes around pulses
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
		
		g.setColor(Color.BLACK);
		for (int i = 1; i < samples.size(); ++i)
		{
			AudioSample sample         = samples.get(i);
			AudioSample previousSample = samples.get(i - 1);
			
			// draw a line from the previous sample to this one
			g.drawLine(
					getXForTime(previousSample.timeNS), getYForAmplitude(previousSample.amplitude),
					getXForTime(sample        .timeNS), getYForAmplitude(sample        .amplitude));
		}
		
		
		// -------------------------------------------------------------------
		// guide line labels
		
		if (showGridLines)
		{
			int topY    = visualizationYOrigin - visualizationYRange - 10;
			int bottomY = visualizationYOrigin + visualizationYRange + 10;
			
			g.setColor(Color.LIGHT_GRAY);
			g.fillRect(scrollPosX, topY, 30, bottomY - topY);
			
			g.setColor(Color.BLACK);
			for (int i = 10; i > -11; --i)
			{
				double ratio = i / 10.0d;
				int y = visualizationYOrigin - (int)(ratio * visualizationYRange);
				
				g.drawString(Double.toString(ratio), scrollPosX + 3, y + 5);
			}
		}
		
		
		// -------------------------------------------------------------------
		// info
		
		// draw the scroll position
		g.setColor(Color.BLACK);
		g.drawString("Samples Processed: " + totalNumSamples,             scrollPosX + 20, 30);
		g.drawString("Current Time: "      + visualizationTimePositionNS, scrollPosX + 20, 45);
		
		g.setColor(FM1_COLOR);
		g.drawString("FM1 Pulses: "        + totalNumPulsesFM1,           scrollPosX + 20, 60);
		
		g.setColor(FM2_COLOR);
		g.drawString("FM2 Pulses: "        + totalNumPulsesFM2,           scrollPosX + 20, 75);
		
		
		g.setColor(Color.BLACK);
		g.drawString("W, S: Zoom",                       scrollPosX + 300, 30);
		g.drawString("A, D: Pan",                        scrollPosX + 300, 45);
		g.drawString("1, 2: Toggle Pulse Boxes",         scrollPosX + 300, 60);
		g.drawString("3: Toggle Pulse Amplitude Deltas", scrollPosX + 300, 75);
		g.drawString("4: Toggle Grid Lines",             scrollPosX + 300, 90);
		
		
		g.setColor(FM1_COLOR);
		g.drawString("FM1 Pulse Amplitude Delta Threshold: " + FlowMeterReader.FM1_PULSE_AMPLITUDE_DELTA_THRESHOLD_RATIO + " (" + FlowMeterReader.FM1_PULSE_AMPLITUDE_DELTA_THRESHOLD + ")", scrollPosX + 600, 30);
		
		g.setColor(FM2_COLOR);
		g.drawString("FM2 Pulse Amplitude Delta Threshold: " + FlowMeterReader.FM2_PULSE_AMPLITUDE_DELTA_THRESHOLD_RATIO + " (" + FlowMeterReader.FM2_PULSE_AMPLITUDE_DELTA_THRESHOLD + ")", scrollPosX + 600, 45);
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
		return (int)((timeNS - visualizationTimePositionNS) * visualizationXScale);
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

