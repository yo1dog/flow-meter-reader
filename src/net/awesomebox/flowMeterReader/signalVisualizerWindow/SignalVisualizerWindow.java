package net.awesomebox.flowMeterReader.signalVisualizerWindow;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JFrame;
import javax.swing.JScrollBar;

import net.awesomebox.flowMeterReader.SignalVisualizer;

public class SignalVisualizerWindow implements KeyListener, SignalVisualizerDisplayPanelListener, SignalVisualizerScrubberScrollBarListener
{
	// ===================================================================
	// Constants
	//
	// ===================================================================
	
	private static final int VISUALIZATION_TOP_PADDING    = 120;
	private static final int VISUALIZATION_BOTTOM_PADDING = 40;
	private static final int VISUALIZATION_MIN_HEIGHT     = 100;
	
	
	
	// ===================================================================
	// Variables
	//
	// ===================================================================
	
	private SignalVisualizer signalVisualizer;
	
	// if the streamer should pause
	private boolean streamerShouldPause = false;
	public boolean getStreamerShouldPause()
	{
		return streamerShouldPause;
	}
	
	
	// -------------------------------------------------------------------
	// window elements
	
	private final JFrame frame;
	private final SignalVisualizerDisplayPanel visualizerDisplayPanel;
	private final SignalVisualizerScrubberScrollBar visualizerScrubberScrollBar;
	
	
	
	
	public SignalVisualizerWindow(SignalVisualizer signalVisualizer, int visulizationWidth, int visulizationHeight)
	{
		this.signalVisualizer = signalVisualizer;
		signalVisualizer.setVisualizationSize(visulizationWidth, visulizationHeight);
		
		
		// create the frame
		frame = new JFrame("SignalVisualizer");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.addKeyListener(this);
		
		
		// create the display
		visualizerDisplayPanel = new SignalVisualizerDisplayPanel(signalVisualizer, this);
		
		// set the starting size
		// TODO: set frame size and not panel size?
		visualizerDisplayPanel.setPreferredSize(new Dimension(
			signalVisualizer.getVisualizationWidth(),
			signalVisualizer.getVisualizationHeight()
		));
		
		frame.add(visualizerDisplayPanel, java.awt.BorderLayout.CENTER);
		
		
		// create the scrubber
		visualizerScrubberScrollBar = new SignalVisualizerScrubberScrollBar(signalVisualizer, this);
		visualizerScrubberScrollBar.setOrientation(JScrollBar.HORIZONTAL);
		
		frame.add(visualizerScrubberScrollBar, java.awt.BorderLayout.SOUTH);
		
		
		// finalize
		frame.pack();
	}
	
	public void show()
	{
		frame.setVisible(true);
	}
	
	
	
	
	
	/**
	 * Refreshes the window.
	 * 
	 * This should be called after the signal visualizer has been refreshed.
	 */
	public void refresh()
	{
		// update the scrubber
		visualizerScrubberScrollBar.refresh();
		
		// redraw the display
		visualizerDisplayPanel.repaint();
	}
	
	
	
	@Override
	public void onScrubberChange()
	{
		// redraw the display
		visualizerDisplayPanel.repaint();
	}
	
	@Override
	public void onDisplayPanelReszied()
	{
		// update the scrubber
		visualizerScrubberScrollBar.refresh();
	}
	
	
	@Override
	public void keyPressed(KeyEvent e)
	{
		switch(e.getKeyCode())
		{
			case KeyEvent.VK_SPACE:
				streamerShouldPause = !streamerShouldPause;
				System.out.println(streamerShouldPause);
				break;
			
			case KeyEvent.VK_W:
				visualizerDisplayPanel.zoomIn();
				visualizerDisplayPanel.repaint();
				visualizerScrubberScrollBar.refresh();
				break;
			
			case KeyEvent.VK_S: 
				visualizerDisplayPanel.zoomOut();
				visualizerDisplayPanel.repaint();
				visualizerScrubberScrollBar.refresh();
				break;
			
			case KeyEvent.VK_A: 
				visualizerScrubberScrollBar.panLeft();
				visualizerDisplayPanel.repaint();
				break;
			
			case KeyEvent.VK_D: 
				visualizerScrubberScrollBar.panRight();
				visualizerDisplayPanel.repaint();
				break;
			
			case KeyEvent.VK_1: 
				signalVisualizer.toggleShowFM1PulseBoxes();
				visualizerDisplayPanel.repaint();
				break;
			
			case KeyEvent.VK_2: 
				signalVisualizer.toggleShowFM2PulseBoxes();
				visualizerDisplayPanel.repaint();
				break;
			
			case KeyEvent.VK_3: 
				signalVisualizer.toggleShowPulseAmplitudeDeltas();
				visualizerDisplayPanel.repaint();
				break;
			
			case KeyEvent.VK_4: 
				signalVisualizer.toggleShowGridLines();
				visualizerDisplayPanel.repaint();
				break;
		}
	}
	
	@Override public void keyTyped(KeyEvent e) {}
	@Override public void keyReleased(KeyEvent e) {}
}
