package net.awesomebox.flowMeterReader.signalVisualizerWindow;

import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.awesomebox.flowMeterReader.FlowMeterReader;
import net.awesomebox.flowMeterReader.signalVisualizer.SignalVisualizer;
import net.awesomebox.flowMeterReader.signalVisualizer.SignalVisualizerDrawer;

public class SignalVisualizerInfoPanel extends JPanel
{
	private static final long serialVersionUID = -1745153348775713789L;
	
	private final SignalVisualizer signalVisualizer;
	
	private final JLabel samplesProcessedLabel;
	private final JLabel fm1PulsesLabel;
	private final JLabel fm2PulsesLabel;
	
	public SignalVisualizerInfoPanel(SignalVisualizer signalVisualizer)
	{
		super();
		
		this.signalVisualizer = signalVisualizer;
		
		// main layout
		this.setLayout(new GridLayout(1, 2));
		
		
		// create stats panel
		JPanel statsPanel = new JPanel();
		statsPanel.setLayout(new GridLayout(0, 2, 10, 0));
		
		statsPanel.add(new JLabel("Samples Processed:"));
		statsPanel.add(samplesProcessedLabel = new JLabel());
		
		statsPanel.add(new JLabel("FM1 Pulses:"));
		statsPanel.add(fm1PulsesLabel = new JLabel());
		fm1PulsesLabel.setForeground(SignalVisualizerDrawer.FM1_COLOR);
		
		statsPanel.add(new JLabel("FM2 Pulses:"));
		statsPanel.add(fm2PulsesLabel = new JLabel());
		fm2PulsesLabel.setForeground(SignalVisualizerDrawer.FM2_COLOR);
		
		statsPanel.add(new JLabel("FM1 Pulse Amplitude Delta Threshold:"));
		statsPanel.add(new JLabel(FlowMeterReader.FM1_PULSE_AMPLITUDE_DELTA_THRESHOLD_RATIO + " (" + FlowMeterReader.FM1_PULSE_AMPLITUDE_DELTA_THRESHOLD + ")"));
		
		statsPanel.add(new JLabel("FM2 Pulse Amplitude Delta Threshold:"));
		statsPanel.add(new JLabel(FlowMeterReader.FM2_PULSE_AMPLITUDE_DELTA_THRESHOLD_RATIO + " (" + FlowMeterReader.FM2_PULSE_AMPLITUDE_DELTA_THRESHOLD + ")"));
		
		this.add(statsPanel);
		
		
		// create instruction panel
		JPanel instructionPanel = new JPanel();
		instructionPanel.setLayout(new GridLayout(0, 1, 10, 0));
		
		instructionPanel.add(new JLabel("W, S: Zoom"));
		instructionPanel.add(new JLabel("A, D: Pan"));
		instructionPanel.add(new JLabel("1, 2: Toggle Pulse Boxes"));
		instructionPanel.add(new JLabel("3: Toggle Pulse Amplitude Deltas"));
		instructionPanel.add(new JLabel("4: Toggle Grid Lines"));
		
		this.add(instructionPanel);
	}
	
	
	/**
	 * Updates the label values.
	 */
	public void refresh()
	{
		samplesProcessedLabel.setText(Long.toString(signalVisualizer.getTotalNumSamples()));
		fm1PulsesLabel.setText(Long.toString(signalVisualizer.getTotalNumPulsesFM1()));
		fm2PulsesLabel.setText(Long.toString(signalVisualizer.getTotalNumPulsesFM2()));
	}
}