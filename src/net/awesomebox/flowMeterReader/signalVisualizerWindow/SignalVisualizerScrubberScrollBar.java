package net.awesomebox.flowMeterReader.signalVisualizerWindow;

import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.swing.JScrollBar;

import net.awesomebox.flowMeterReader.AudioSample;
import net.awesomebox.flowMeterReader.signalVisualizer.SignalVisualizer;

public class SignalVisualizerScrubberScrollBar extends JScrollBar implements AdjustmentListener
{
	private static final long serialVersionUID = 5286426886080675707L;
	
	// The scroll bar uses ints and our times are in nanoseconds.
	// This means we will only be able to span 2 seconds. Scaling
	// the values down before converting to ints will allow use to
	// operate longer before overflowing
	private static final double SCROLL_BAR_SCALE = 0.0005d;
	
	private final SignalVisualizer signalVisualizer;
	private final SignalVisualizerScrubberScrollBarListener listener;
	
	public SignalVisualizerScrubberScrollBar(SignalVisualizer signalVisualizer, SignalVisualizerScrubberScrollBarListener listener)
	{
		super();
		
		this.signalVisualizer = signalVisualizer;
		this.listener = listener;
		this.addAdjustmentListener(this);
	}
	
	@Override
	public void adjustmentValueChanged(AdjustmentEvent e)
	{
		// set visualization view position
		long newVisualizationViewTimePositionNS = (long)(this.getModel().getValue() / SCROLL_BAR_SCALE);
		signalVisualizer.setVisualizationViewTimePositionNS(newVisualizationViewTimePositionNS);
		
		listener.onScrubberChange();
	}
	
	public void panLeft()
	{
		getModel().setValue(getModel().getValue() - getUnitIncrement());
	}
	
	public void panRight()
	{
		getModel().setValue(getModel().getValue() + getUnitIncrement());
	}
	
	
	
	
	
	public void refresh()
	{
		AudioSample oldestSample = signalVisualizer.getOldestSample();
		AudioSample newestSample = signalVisualizer.getNewestSample();
		
		if (oldestSample == null)
			return;
		
		// set the position and the extent to reflect the visualizer's view
		int value  = (int)(signalVisualizer.getVisualizationViewTimePositionNS() * SCROLL_BAR_SCALE);
		int extent = (int)(signalVisualizer.getVisualizationViewTimeSpanNS()     * SCROLL_BAR_SCALE);
		
		// set the min and max values to reflect the visualizer's oldest and newest samples
		int min = (int)(oldestSample.timeNS * SCROLL_BAR_SCALE);
		int max = (int)(newestSample.timeNS * SCROLL_BAR_SCALE);
		
		update(
			value,
			extent,
			min,
			max
		);
		
		setBlockIncrement(extent / 2);
		setUnitIncrement(extent / 10);
	}
	
	
	
	
	private void update(int value, int extent, int min, int max)
	{
		if (extent > max - min)
			extent = max - min;
		
		if (value < min)
			value = min;
		
		if (value + extent > max)
			value = max - extent;
		
		getModel().setRangeProperties(value, extent, min, max, getModel().getValueIsAdjusting());
	}
}
