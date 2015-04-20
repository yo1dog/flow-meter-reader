package net.awesomebox.flowMeterReader;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import net.awesomebox.flowMeterReader.signalVisualizerWindow.SignalVisualizerWindow;


// TODO: memory leak
public class Main
{
	// starting screen size
	private static final int STARTING_VISUALIZATION_WIDTH  = 1200;
	private static final int STARTING_VISUALIZATION_HEIGHT = 500;
	
	
	public static void main(String[] args) throws Exception
	{
		streamFromFile();
		//streamFromMic();
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
		
		
		
		int sampleRate = (int)audioFormat.getSampleRate();
		
		// create the visualizer
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
			
			if (numBytesRead > -1)
			{
				// read the audio data
				FlowMeterReading reading = flowMeterReader.readFlowMeterAudioData(audioByteBuffer, 0, numBytesRead, audioFormat.isBigEndian());
				
				// update the visualizer
				signalVisualizer.addSamples(reading.samples);
				signalVisualizer.addPulses(reading.pulses);
				signalVisualizer.refresh();
				
				// update the visualizer window
				signalVisualizerWindow.refresh();
			}
			
			Thread.sleep(1);
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
		
		// get additional info from the audio format
		DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
		
		// get the data line from the info
		SourceDataLine sourceDataLine = (SourceDataLine)AudioSystem.getLine(dataLineInfo);
		sourceDataLine.open(audioFormat);
		
		
		int sampleRate = (int)audioFormat.getSampleRate();
		
		// create the visualizer
		SignalVisualizer signalVisualizer = new SignalVisualizer(sampleRate);
		
		// create the visualizer window
		SignalVisualizerWindow signalVisualizerWindow = new SignalVisualizerWindow(signalVisualizer, STARTING_VISUALIZATION_WIDTH, STARTING_VISUALIZATION_HEIGHT);
		signalVisualizerWindow.show();
		
		// create the reader
		FlowMeterReader flowMeterReader = new FlowMeterReader(sampleRate);
		
		
		
		// open the audio stream into the clip
		int numBytesRead = 0;
		int totalNumBytesRead = 0;
		byte[] audioByteBuffer = new byte[1000000];
		
		sourceDataLine.start();
		
		do
		{
			// read the bytes into the buffer
			try
			{
				numBytesRead = audioInputStream.read(audioByteBuffer, 0, audioByteBuffer.length);
			}
			catch (IOException e)
			{
				e.printStackTrace();
				continue;
			}
			
			// process the data we have read so far
			if (numBytesRead > 0)
			{
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
			
			Thread.sleep(1);
		}
		while (numBytesRead > -1);
		
		sourceDataLine.close();
		
		System.out.println("Done Reading");
		System.out.println("Read " + totalNumBytesRead + " bytes total");
	}
}
