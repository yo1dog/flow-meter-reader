package net.awesomebox.flowMeterReader;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

import net.awesomebox.flowMeterReader.signalVisualizer.SignalVisualizer;
import net.awesomebox.flowMeterReader.signalVisualizerWindow.SignalVisualizerWindow;


public class Main
{
	// starting screen size
	private static final int STARTING_VISUALIZATION_WIDTH  = 1200;
	private static final int STARTING_VISUALIZATION_HEIGHT = 500;
	
	
	public static void main(String[] args) throws Exception
	{
		streamFromMic();
		//streamFromFile();
	}
	
	private static void streamFromMic() throws Exception
	{
		// create our format
		AudioFormat audioFormat = new AudioFormat(
			AudioFormat.Encoding.PCM_SIGNED, // encoding
			16000.0f,                        // sample rate.
			16,                              // sample size in bits. NOTE: If you change this, you will have to change the amplitude data type.
			1,                               // channels
			2,                               // frame size
			16000.0f,                        // frame rate
			true);                           // big-endian
		
		
		// create the data line info
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
		if (!AudioSystem.isLineSupported(info))
			throw new Exception("Line is not supported");
		
		// get the data line
		TargetDataLine line = (TargetDataLine)AudioSystem.getLine(info);
		line.open(audioFormat);
		
		
		// create the visualizer
		int sampleRate = (int)audioFormat.getSampleRate();
		SignalVisualizer signalVisualizer = new SignalVisualizer(sampleRate, 10 * FlowMeterReader.NS_IN_S, true);
		
		// create the visualizer window
		SignalVisualizerWindow signalVisualizerWindow = new SignalVisualizerWindow(signalVisualizer, STARTING_VISUALIZATION_WIDTH, STARTING_VISUALIZATION_HEIGHT);
		signalVisualizerWindow.show();
		
		// create the reader
		FlowMeterReader flowMeterReader = new FlowMeterReader(sampleRate);
		
		
		// start listening
		line.start();
		
		byte[] audioByteBuffer = new byte[line.getBufferSize()];
		while (true)
		{
			// check if we should pause
			if (signalVisualizerWindow.getStreamerShouldPause())
			{
				Thread.sleep(100);
				line.flush();
				continue;
			}
			
			// read bytes from the line
			int numBytesRead = line.read(audioByteBuffer, 0, 180);
			
			if (line.available() > line.getBufferSize() / 2)
				System.err.println("Getting behind! " + line.available());
			
			// check if we read any bytes
			if (numBytesRead <= 0)
				continue;
			
			// read the audio data
			FlowMeterReading reading = flowMeterReader.readFlowMeterAudioData(audioByteBuffer, 0, numBytesRead, audioFormat.isBigEndian());
			
			// update the visualizer
			signalVisualizer.addSamples(reading.samples);
			signalVisualizer.addPulses(reading.pulses);
			signalVisualizer.refresh();
			
			// update the visualizer window
			signalVisualizerWindow.refresh();
		}
	}
	
	private static void streamFromFile() throws Exception
	{
		// get test file
		File testFile = new File(System.getProperty("user.dir") + File.separatorChar + "res" + File.separatorChar + "testBoth.wav");
		
		// load file as audio stream
		AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(testFile);
		
		// get the audio format from the steam
		AudioFormat audioFormat = audioInputStream.getFormat();
		
		
		// create the visualizer
		int sampleRate = (int)audioFormat.getSampleRate();
		SignalVisualizer signalVisualizer = new SignalVisualizer(sampleRate);
		
		// create the visualizer window
		SignalVisualizerWindow signalVisualizerWindow = new SignalVisualizerWindow(signalVisualizer, STARTING_VISUALIZATION_WIDTH, STARTING_VISUALIZATION_HEIGHT);
		signalVisualizerWindow.show();
		
		// create the reader
		FlowMeterReader flowMeterReader = new FlowMeterReader(sampleRate);
		
		
		// read the audio stream
		int numBytesRead = 0;
		int totalNumBytesRead = 0;
		byte[] audioByteBuffer = new byte[1000000];
		
		do
		{
			// read the bytes into the buffer
			try
			{
				numBytesRead = audioInputStream.read(audioByteBuffer);
			}
			catch (IOException e)
			{
				e.printStackTrace();
				continue;
			}
			
			// check if we read any bytes
			if (numBytesRead <= 0)
				continue;
			
			// read the audio data
			FlowMeterReading reading = flowMeterReader.readFlowMeterAudioData(audioByteBuffer, 0, numBytesRead, audioFormat.isBigEndian());
			
			// update the visualizer
			signalVisualizer.addSamples(reading.samples);
			signalVisualizer.addPulses(reading.pulses);
			signalVisualizer.refresh();
			
			// update the visualizer window
			signalVisualizerWindow.refresh();
			
			totalNumBytesRead += numBytesRead;
		}
		while (numBytesRead > -1);
		
		audioInputStream.close();
		
		System.out.println("Done Reading");
		System.out.println("Read " + totalNumBytesRead + " bytes total");
	}
}
