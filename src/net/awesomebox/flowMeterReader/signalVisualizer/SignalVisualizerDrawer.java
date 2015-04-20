package net.awesomebox.flowMeterReader.signalVisualizer;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;

import net.awesomebox.flowMeterReader.AudioSample;
import net.awesomebox.flowMeterReader.FlowMeterReader;
import net.awesomebox.flowMeterReader.Pulse;

public class SignalVisualizerDrawer
{
	// ===================================================================
	// Constants
	//
	// ===================================================================
	
	public static final Color FM1_COLOR       = new Color(255, 0,   0);
	public static final Color FM1_LIGHT_COLOR = new Color(255, 128, 128);
	public static final Color FM2_COLOR       = new Color(0,   0,   255);
	public static final Color FM2_LIGHT_COLOR = new Color(128, 128, 255);
	
	
	
	// ===================================================================
	// Variables
	//
	// ===================================================================
	
	// size
	private int visualizationWidth;
	private int visualizationHeight;
	
	// x-axis
	private double visualizationXScale;
	
	// y-axis
	private int visualizationYOrigin;
	private int visualizationYRange;
	private double visualizationYScale;
	
	// view
	private long visualizationViewTimePositionNS;
	private long visualizationViewTimeSpanNS;
	private int visualizationViewFirstVisibleSampleIndex;
	private int visualizationViewLastVisibleSampleIndex;
	
	
	
	// ===================================================================
	// Update Methods
	//
	// ===================================================================
	
	void updateVisualizationSize(
		int visualizationWidth,
		int visualizationHeight)
	{
		this.visualizationWidth  = visualizationWidth;
		this.visualizationHeight = visualizationHeight;
	}
	
	void updateVisualizationXAxis(double visualizationXScale)
	{
		this.visualizationXScale = visualizationXScale;
	}
	
	void updateVisualizationYAxis(
		int visualizationYOrigin,
		int visualizationYRange,
		double visualizationYScale)
	{
		this.visualizationYOrigin = visualizationYOrigin;
		this.visualizationYRange  = visualizationYRange;
		this.visualizationYScale  = visualizationYScale;
	}
	
	void updateVisualizationView(
		long visualizationViewTimePositionNS,
		long visualizationViewTimeSpanNS,
		int visualizationViewFirstVisibleSampleIndex,
		int visualizationViewLastVisibleSampleIndex)
	{
		this.visualizationViewTimePositionNS          = visualizationViewTimePositionNS;
		this.visualizationViewTimeSpanNS              = visualizationViewTimeSpanNS;
		this.visualizationViewFirstVisibleSampleIndex = visualizationViewFirstVisibleSampleIndex;
		this.visualizationViewLastVisibleSampleIndex  = visualizationViewLastVisibleSampleIndex;
	}
	
	
	
	// ===================================================================
	// Drawing Methods
	//
	// ===================================================================
	
	// -------------------------------------------------------------------
	// samples
	
	void drawSamples(
		ArrayList<AudioSample> samples,
		Graphics g)
	{
		if (visualizationViewFirstVisibleSampleIndex < 0)
			return;
		
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
	// pulses
	
	void drawPulses(
		ArrayList<Pulse> pulses,
		boolean showFM1PulseBoxes,
		boolean showFM2PulseBoxes,
		boolean showPulseAmplitudeDeltas,
		Graphics g)
	{
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
	}
	
	
	// -------------------------------------------------------------------
	// boundary lines
	
	void drawBoundaryLines(Graphics g)
	{
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
	}
	
	
	// -------------------------------------------------------------------
	// grid lines
	
	void drawGridLines(Graphics g)
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
			g.drawLine(x, SignalVisualizer.VISUALIZATION_PADDING_TOP, x, visualizationHeight);
			
			// draw label
			g.setColor(Color.BLACK);
			
			// get displayTime
			String displayTime;
			if (unit == null)
				displayTime = time + "ns";
			else
				displayTime = ((time / (unitMul / 100)) / 100.0d) + unit; // round to two decimal places
			
			g.drawString(displayTime, x + 1, visualizationHeight - 2);
		}
	}
	
	void drawGridLineLables(Graphics g)
	{
		g.setColor(Color.LIGHT_GRAY);
		g.fillRect(0, 0, 30, visualizationHeight);
		
		g.setColor(Color.BLACK);
		for (int i = 10; i > -11; --i)
		{
			double ratio = i / 10.0d;
			int y = visualizationYOrigin - (int)(ratio * visualizationYRange);
			
			g.drawString(Double.toString(ratio), 3, y + 5);
		}
	}
	
	
	// -------------------------------------------------------------------
	// helpers
	
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
