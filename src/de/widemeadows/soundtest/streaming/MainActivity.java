package de.widemeadows.soundtest.streaming;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import org.jetbrains.annotations.NotNull;


public class MainActivity extends Activity implements SeekBar.OnSeekBarChangeListener, CompoundButton.OnCheckedChangeListener
{
	/**
	 * Die minimale Frequenz
	 */
	private final float MinFrequency = 220;

	/**
	 * Die maximale Frequenz
	 */
	private final float MaxFrequency = 660;

	/**
	 * Die Frequenz
	 */
	private volatile float frequency = MinFrequency;

	/**
	 * Die TextView
	 */
	@NotNull
	private TextView frequencyTextView;

	/**
	 * The streaming audio track
	 */
	private AudioTrack track;

	/**
	 * Die Puffergröße
	 */
	private int bufferSize = 1024;

	/**
	 * The sampling rate in Hz
	 */
	protected static final int SamplingRate = 44100;

	/**
	 * Gibt an, ob die Anwendung läuft
	 */
	private volatile boolean isRunning = false;

	/**
	 * Der Streaming-Thread
	 */
	private Thread streamingThread;

	@Override
	protected void onStop() {
		super.onStop();
		track.release();
	}

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

	    // Streaming Audio-Track erzeugen
	    int minBufferSize = AudioTrack.getMinBufferSize(SamplingRate, AudioFormat.CHANNEL_CONFIGURATION_MONO,AudioFormat.ENCODING_PCM_16BIT);
	    bufferSize = Math.max(minBufferSize, 8192);
	    track = new AudioTrack(AudioManager.STREAM_MUSIC, SamplingRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
	    Log.v("Audio streaming test", "Min buffer size: " + minBufferSize + ", Audio buffer size: " + bufferSize);
	    Log.v("Audio streaming test", "Native sampling rate: " + AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC) + " Hz");

	    // Button verdrahten
	    ToggleButton button = (ToggleButton) findViewById(R.id.button);
	    button.setOnCheckedChangeListener(this);

	    // SeekBar verdrahten
	    SeekBar bar = (SeekBar) findViewById(R.id.frequencySpinner);
	    bar.setOnSeekBarChangeListener(this);

	    // Frequenzanzeige verdrahten
	    frequencyTextView = (TextView)findViewById(R.id.frequencyView);
	    showFrequency();
    }

	/**
	 * Zeigt die Frequenz an
	 */
	private void showFrequency() {
		frequencyTextView.setText(Float.toString(frequency) + " Hz");
	}

	/** @inheritDoc */
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		frequency = Math.round((MaxFrequency - MinFrequency) * (progress/100.0f) + MinFrequency);
		showFrequency();
	}

	/** @inheritDoc */
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	/** @inheritDoc */
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

	/**
	 * Startet das Streaming
	 */
	private void startStreaming() {
		assert !isRunning;
		track.flush();
		track.play();

		// Start thread
		isRunning = true;
		streamingThread = new Thread(sineGenerator);
		streamingThread.start();
	}

	/**
	 * Hält die Wiedergabe an
	 */
	private void stopStreaming() {
		assert isRunning;

		// Thread und Wiedergabe anhalten
		isRunning = false;
		track.stop();
		try {
			streamingThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/** @inheritDoc */
	@Override
	public void onCheckedChanged(CompoundButton compoundButton, boolean fromUser) {
		if (compoundButton.isChecked()) {
			startStreaming();
		}
		else {
			stopStreaming();
		}
	}

	/** @inheritDoc */
	@Override
	protected void onPause() {
		// Wiedergabe anhalten
		((ToggleButton) findViewById(R.id.button)).setChecked(false);
		super.onPause();
	}

	/**
	 * Der Sinusgenerator
	 */
	private final Runnable sineGenerator = new Runnable() {

		/**
		 * Streaming-Methode
		 */
		@Override
		public void run() {
			float angle = 0;

			//Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

			Log.v("Audio thread", "Starting thread ...");
			short[] buffer = new short[100]; // new short[(int)(SamplingRate/frequency)];
			//Random random = new Random();
			while(isRunning) {

				// Inkrement berechnen
				float increment = (float)(2*Math.PI * frequency / SamplingRate);

				// Samples generieren
				for (int i = 0; i < buffer.length; ++i) {
					//float value = random.nextFloat()*2-1;
					float value = (float) Math.sin(angle);
					buffer[i] = (short)Math.floor(value*Short.MAX_VALUE);
					angle += increment;
				}

				// In den Puffer schreiben
				track.write(buffer, 0, buffer.length);
			}
			Log.v("Audio thread", "Thread left.");
		}
	};
}
