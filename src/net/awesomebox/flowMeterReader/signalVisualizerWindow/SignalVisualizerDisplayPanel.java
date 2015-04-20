package net.awesomebox.flowMeterReader.signalVisualizerWindow;

import java.awt.Graphics;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JPanel;

import net.awesomebox.flowMeterReader.SignalVisualizer;

/**
 * Custom panel for drawing our signal visualization.
 */
public class SignalVisualizerDisplayPanel extends JPanel implements ComponentListener
{
	private static final long serialVersionUID = 6891481639696278319L;
	
	private static final double ZOOM_RATE = 1.5d;
	
	private static final double MIN_VISUALIZATION_X_SCALE = 1.0d/20000000; // pixels per nanosecond. 1 pixel = 20000000 nanoseconds
	private static final double MAX_VISUALIZATION_X_SCALE = 1.0d/50;       // pixels per nanosecond. 1 pixel = 50 nanoseconds
	
	
	private final SignalVisualizer signalVisualizer;
	private final SignalVisualizerDisplayPanelListener listener;
	
	public SignalVisualizerDisplayPanel(SignalVisualizer signalVisualizer, SignalVisualizerDisplayPanelListener listener)
	{
		super();
		
		this.signalVisualizer = signalVisualizer;
		this.listener = listener;
		this.addComponentListener(this);
	}
	
	
	@Override
	public void paint(Graphics g)
	{
		signalVisualizer.drawVisualization(g);
	}
	
	@Override
	public void componentResized(ComponentEvent e)
	{
		// resize the visualizer
		signalVisualizer.setVisualizationSize(this.getWidth(), this.getHeight());
		
		listener.onDisplayPanelReszied();
	}
	
	
	
	public void zoomIn()
	{
		double newVisualizationXScale = signalVisualizer.getVisualizationXScale() * ZOOM_RATE;
		
		if (newVisualizationXScale > MAX_VISUALIZATION_X_SCALE)
			return;
		
		// record the center of the view
		long visualizationViewCenterTimeNS = signalVisualizer.getVisualizationViewTimePositionNS() + signalVisualizer.getVisualizationViewTimeSpanNS() / 2;
		
		// set the new X scale
		signalVisualizer.setVisualizationXScale(newVisualizationXScale);
		
		// re-center the view
		signalVisualizer.setVisualizationViewTimePositionNS(visualizationViewCenterTimeNS - signalVisualizer.getVisualizationViewTimeSpanNS() / 2);
	}
	
	public void zoomOut()
	{
		double newVisualizationXScale = signalVisualizer.getVisualizationXScale() / ZOOM_RATE;
		
		if (newVisualizationXScale < MIN_VISUALIZATION_X_SCALE)
			return;
		
		// record the center of the view
		long visualizationViewCenterTimeNS = signalVisualizer.getVisualizationViewTimePositionNS() + signalVisualizer.getVisualizationViewTimeSpanNS() / 2;
		
		signalVisualizer.setVisualizationXScale(newVisualizationXScale);
		
		// re-center the view
		signalVisualizer.setVisualizationViewTimePositionNS(visualizationViewCenterTimeNS - signalVisualizer.getVisualizationViewTimeSpanNS() / 2);
	}
	
	
	
	@Override public void componentMoved(ComponentEvent e) {}
	@Override public void componentShown(ComponentEvent e) {}
	@Override public void componentHidden(ComponentEvent e) {}
}
