package net.awesomebox.flowMeterReader;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

public class SignalVisualizerKeyBindings
{
	public static void attachKeyBindings(final SignalVisualizer visualizer)
	{
		final JPanel visualizerPanel = visualizer.visualizerPanel;
		
		visualizerPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false), "TogglePause");
		visualizerPanel.getActionMap().put("TogglePause", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			
			@Override
	        public void actionPerformed(ActionEvent ae) {
	            if (visualizer.pauseStreamer)
	            {
	            	System.out.println("Unpaused");
	            	visualizer.pauseStreamer = false;
	            }
	            else
	            {
	            	System.out.println("Paused");
	            	visualizer.pauseStreamer = true;
	            }
	        }
	    });
		
		visualizerPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, false), "ZoomIn");
		visualizerPanel.getActionMap().put("ZoomIn", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			
			@Override
	        public void actionPerformed(ActionEvent ae) {
				double newVisualizationXScale = visualizer.visualizationXScale * SignalVisualizer.ZOOM_RATE;
				
				if (newVisualizationXScale > SignalVisualizer.MAX_VISUALIZATION_X_SCALE)
					return;
				
				visualizer.visualizationXScale = newVisualizationXScale;
				visualizer.recalculateVisualizationWindowTimeSpanNS(true);
				visualizerPanel.repaint();
	        }
	    });
		
		visualizerPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0, false), "ZoomOut");
		visualizerPanel.getActionMap().put("ZoomOut", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			
			@Override
	        public void actionPerformed(ActionEvent ae) {
				double newVisualizationXScale = visualizer.visualizationXScale / SignalVisualizer.ZOOM_RATE;
				
				if (newVisualizationXScale < SignalVisualizer.MIN_VISUALIZATION_X_SCALE)
					return;
				
				visualizer.visualizationXScale = newVisualizationXScale;
				visualizer.recalculateVisualizationWindowTimeSpanNS(true);
				visualizerPanel.repaint();
	        }
	    });
		
		
		
		visualizerPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, false), "PanLeft");
		visualizerPanel.getActionMap().put("PanLeft", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			
			@Override
	        public void actionPerformed(ActionEvent ae) {
				if (visualizer.scrollBar == null)
					return;
				
				visualizer.scrollBar.getModel().setValue(visualizer.scrollBar.getModel().getValue() - visualizer.scrollBar.getUnitIncrement());
	        }
	    });
		
		visualizerPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0, false), "PanRight");
		visualizerPanel.getActionMap().put("PanRight", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			
			@Override
	        public void actionPerformed(ActionEvent ae) {
				if (visualizer.scrollBar == null)
					return;
				
				visualizer.scrollBar.getModel().setValue(visualizer.scrollBar.getModel().getValue() + visualizer.scrollBar.getUnitIncrement());
	        }
	    });
		
		
		
		visualizerPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_1, 0, false), "ToggleFM1PulseBoxes");
		visualizerPanel.getActionMap().put("ToggleFM1PulseBoxes", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			
			@Override
	        public void actionPerformed(ActionEvent ae) {
				visualizer.showFM1PulseBoxes = !visualizer.showFM1PulseBoxes;
				visualizerPanel.repaint();
	        }
	    });
		
		visualizerPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_2, 0, false), "ToggleFM2PulseBoxes");
		visualizerPanel.getActionMap().put("ToggleFM2PulseBoxes", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			
			@Override
	        public void actionPerformed(ActionEvent ae) {
				visualizer.showFM2PulseBoxes = !visualizer.showFM2PulseBoxes;
				visualizerPanel.repaint();
	        }
	    });
		
		visualizerPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_3, 0, false), "TogglePulseAmplitudeDeltas");
		visualizerPanel.getActionMap().put("TogglePulseAmplitudeDeltas", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			
			@Override
	        public void actionPerformed(ActionEvent ae) {
				visualizer.showPulseAmplitudeDeltas = !visualizer.showPulseAmplitudeDeltas;
				visualizerPanel.repaint();
	        }
	    });
		
		visualizerPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_4, 0, false), "ToggleGridLines");
		visualizerPanel.getActionMap().put("ToggleGridLines", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			
			@Override
	        public void actionPerformed(ActionEvent ae) {
				visualizer.showGridLines = !visualizer.showGridLines;
				visualizerPanel.repaint();
	        }
	    });
	}
}
