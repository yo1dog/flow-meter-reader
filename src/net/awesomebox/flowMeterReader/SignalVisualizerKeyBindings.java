package net.awesomebox.flowMeterReader;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.Timer;

public class SignalVisualizerKeyBindings
{
	public static void attachKeyBindings(final SignalVisualizer visualizer)
	{
		visualizer.panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false), "TogglePause");
		visualizer.panel.getActionMap().put("TogglePause", new AbstractAction() {
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
		
		visualizer.panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0, false), "ZoomIn");
		visualizer.panel.getActionMap().put("ZoomIn", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			
			@Override
	        public void actionPerformed(ActionEvent ae) {
				final int originalScrollBarCenter = visualizer.scroller == null? 0 : visualizer.scroller.getHorizontalScrollBar().getValue() + visualizer.scroller.getHorizontalScrollBar().getModel().getExtent() / 2;
				
				visualizer.visualizationXScale *= SignalVisualizer.ZOOM_RATE;
				visualizer.recalculateVisualizationTimeSpanNS();
				visualizer.refresh();
				visualizer.panel.revalidate();
				
				if (visualizer.scroller != null)
				{
					final int newScrollBarCenter = (int)(originalScrollBarCenter * SignalVisualizer.ZOOM_RATE);
					visualizer.scroller.getHorizontalScrollBar().setValue(newScrollBarCenter - visualizer.scroller.getHorizontalScrollBar().getModel().getExtent() / 2);
					
					// TODO: HACK
					Timer timer = new javax.swing.Timer(1, new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent evt) {
							visualizer.scroller.getHorizontalScrollBar().setValue(newScrollBarCenter - visualizer.scroller.getHorizontalScrollBar().getModel().getExtent() / 2);
						}
					});
					
					timer.setRepeats(false);
					timer.start();
				}
	        }
	    });
		
		visualizer.panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0, false), "ZoomOut");
		visualizer.panel.getActionMap().put("ZoomOut", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			
			@Override
	        public void actionPerformed(ActionEvent ae) {
				int originalScrollBarCenter = 0;
				if (visualizer.scroller != null)
					originalScrollBarCenter = visualizer.scroller.getHorizontalScrollBar().getValue() + visualizer.scroller.getHorizontalScrollBar().getModel().getExtent() / 2;
				
				visualizer.visualizationXScale /= SignalVisualizer.ZOOM_RATE;
				visualizer.recalculateVisualizationTimeSpanNS();
				visualizer.refresh();
				visualizer.panel.revalidate();
				
				if (visualizer.scroller != null)
				{
					int newScrollBarCenter = (int)(originalScrollBarCenter / SignalVisualizer.ZOOM_RATE);
					visualizer.scroller.getHorizontalScrollBar().setValue(newScrollBarCenter - visualizer.scroller.getHorizontalScrollBar().getModel().getExtent() / 2);
				}
	        }
	    });
		
		visualizer.panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, false), "PanLeft");
		visualizer.panel.getActionMap().put("PanLeft", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			
			@Override
	        public void actionPerformed(ActionEvent ae) {
				if (visualizer.scroller == null)
					return;
				
				visualizer.scroller.getHorizontalScrollBar().setValue(visualizer.scroller.getHorizontalScrollBar().getValue() - SignalVisualizer.PAN_SPEED);
	        }
	    });
		
		visualizer.panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0, false), "PanRight");
		visualizer.panel.getActionMap().put("PanRight", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			
			@Override
	        public void actionPerformed(ActionEvent ae) {
				if (visualizer.scroller == null)
					return;
				
				visualizer.scroller.getHorizontalScrollBar().setValue(visualizer.scroller.getHorizontalScrollBar().getValue() + SignalVisualizer.PAN_SPEED);
	        }
	    });
		
		
		visualizer.panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_1, 0, false), "ToggleFM1PulseBoxes");
		visualizer.panel.getActionMap().put("ToggleFM1PulseBoxes", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			
			@Override
	        public void actionPerformed(ActionEvent ae) {
				visualizer.showFM1PulseBoxes = !visualizer.showFM1PulseBoxes;
				visualizer.panel.repaint();
	        }
	    });
		
		visualizer.panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_2, 0, false), "ToggleFM2PulseBoxes");
		visualizer.panel.getActionMap().put("ToggleFM2PulseBoxes", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			
			@Override
	        public void actionPerformed(ActionEvent ae) {
				visualizer.showFM2PulseBoxes = !visualizer.showFM2PulseBoxes;
				visualizer.panel.repaint();
	        }
	    });
		
		visualizer.panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_3, 0, false), "TogglePulseAmplitudeDeltas");
		visualizer.panel.getActionMap().put("TogglePulseAmplitudeDeltas", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			
			@Override
	        public void actionPerformed(ActionEvent ae) {
				visualizer.showPulseAmplitudeDeltas = !visualizer.showPulseAmplitudeDeltas;
				visualizer.panel.repaint();
	        }
	    });
		
		visualizer.panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_4, 0, false), "ToggleGridLines");
		visualizer.panel.getActionMap().put("ToggleGridLines", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			
			@Override
	        public void actionPerformed(ActionEvent ae) {
				visualizer.showGridLines = !visualizer.showGridLines;
				visualizer.panel.repaint();
	        }
	    });
	}
}
