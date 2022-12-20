package com.github.jdiemke.tracker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.github.jdiemke.tracker.DarkTracker.LoopMode;
import com.github.jdiemke.tracker.DarkTracker.Sample;

/**
 * https://www.youtube.com/watch?v=R82FExdydLM
 * https://www.youtube.com/watch?v=kAyEOcVZyJg
 * http://www.voegler.eu/pub/audio/digital-audio-mixing-and-normalization.html
 * http://www.drdobbs.com/jvm/creating-music-components-in-java/229700113
 * http://creatingsound.com/2013/06/dsp-audio-programming-series-part-1/
 * http://www.cs.au.dk/~dsound/DigitalAudio.dir/MidiAndFrequencies/
 * MidiAndNoteFrequencies.html https://www.youtube.com/watch?v=I6G0CnBSWVk
 * http://stackoverflow.com/questions/5625573/byte-array-to-short-array-and-back
 * -again-in-java
 * 
 * TODO:
 *      - GUI TOOlkit
 *      - current play position in sample editor
 *      - sample loader
 *      
 *      http://openglgui.sourceforge.net/gui_tut1.html
 *      http://www.gamedev.net/page/resources/_/technical/directx-and-xna/developing-a-gui-using-c-and-directx-part-1-r994
 *      http://www.gamedev.net/page/resources/_/technical/game-programming/creating-a-very-simple-gui-system-for-small-games-part-i-r3652
 * 
 * @author trigger
 */
public class DarkTracker {

    int ROWS_PER_PATTERN = 64;

    public enum LoopMode {
        NO_LOOPING, LOOPING, PING_PONG
    }

    public enum ReplayerState {
        PLAYING, PAUSED, NOT_PLAYING
    }

    public class Sample {

	// samples
	int[] sampleData;
	
	// loop stuff
	LoopMode loopMode;
	int loopStart;
	int loopLength;
	
        int length;
        int volume = 64;
        int fineTune;
       
        String name;
        
	
        public Sample(int[] sampleData, String name) {
	    this.sampleData = sampleData;
	    this.name = name;
	}
        
        public Sample(int[] sampleData, LoopMode loopMode, int loopStart, int loopLenth) {
	    this.sampleData = sampleData;
	    this.loopMode = loopMode;
	    this.loopStart = loopStart;
	    this.loopLength = loopLenth;
	}
	
    }

    private Thread audioRenderingThread = null;

    // pattern order
    ArrayList<Integer> orderList;

    public int BPM = 128;
    // SAMPLE_RATE samples per second
    public int SAMPLE_RATE = 44100;
    public int SAMPLE_SIZE = 16;
    public int CHANNELS = 1;
    public boolean SIGNED = true;
    public boolean BIG_ENDIAN = true;
    public volatile boolean stop = false;

    int[] stuff;
    int[] kick_sample;
    int[] snare_sample;
    int[] synth_sample;
    int[] off_sample;
    int[] vocal_sample;
    public ArrayList<Sample> samples = new ArrayList<Sample>();

    volatile int step = 0, startStep =0;
    int part = 0;

    // put this outside of the play method because otherwise a pause
    // will remove already playing samples fromthe channels!!!!
    ArrayList<Channel> channels = new ArrayList<Channel>();

    int SAMPLES_PER_STEP;
    SourceDataLine auline;
    AudioFormat format;

    Mixer mixer = new Mixer();
    Pattern pattern;

    public Pattern getPattern() {
        return pattern;
    }

    public int getStep() {
        return (startStep +(auline.getFramePosition() / SAMPLES_PER_STEP)) % 32;
    }

    public ArrayList<Sample> getSamples() {
    	return samples;
    }

    public int[] loadSampleData(File file, AudioFormat format) throws UnsupportedAudioFileException, IOException {
        AudioInputStream source = AudioSystem.getAudioInputStream(file);

        System.out.println(AudioSystem.isConversionSupported(format, source.getFormat()));

        // AudioFormat format2 = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE,
        // CHANNELS, SIGNED, BIG_ENDIAN);
        AudioInputStream sourceStream = AudioSystem.getAudioInputStream(format, source);

        byte[] buffer = new byte[1024];

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int read;
        while ((read = sourceStream.read(buffer, 0, buffer.length)) > 0) {
            out.write(buffer, 0, read);
        }

        byte[] byteArray = out.toByteArray();
        short[] shortArray = new short[byteArray.length / 2];

        ByteBuffer.wrap(byteArray).order(ByteOrder.BIG_ENDIAN).asShortBuffer().get(shortArray);
        int[] data = new int[shortArray.length];

        for (int i = 0; i < data.length; i++)
            data[i] = shortArray[i];

        return data;
    }

    static int[] cymbal = new int[] {1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1,
            1, 0, 1, 1, 1};
    static int[] kick = new int[] {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0,
            0, 0, 0, 0};
    static int[] snare = new int[] {0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0,
            1, 0, 0, 0};
    static int[] synth = new int[] {1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0,
            0, 0, 0, 0};
    static int[] notes = new int[] {0, 2, 0, 5, 7, 0, 7, 0, 7, 9, 9, 9, 7, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 12, 0, 5,
            0, 2, 7, 0, 2};
    static int[] notes2 = new int[] {0, 12, 0, 12, 7, 0, 5, 0, 7, 0, 5, 1, 7, 0, 5, 0, 0, 0, 0, 1, 7, 0, 5, 0, 7, 0, 5,
            1, 7, 0, 5, 0};
    static int[] offbeat = new int[] {1, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0};
    static int[] flow = new int[] {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0};

    static double[] noteMultiplyer;

    public DarkTracker() throws LineUnavailableException, UnsupportedAudioFileException, IOException {
        /**
         * Get an ouput line to the sound card
         */
        format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE, CHANNELS, SIGNED, BIG_ENDIAN);
        auline = AudioSystem.getSourceDataLine(format);

        /**
         * Load samples
         */
        stuff = loadSampleData(new File("./data/samples/stabs.wav"), format);
        kick_sample = loadSampleData(new File("./data/samples/drums.wav"), format);
        snare_sample = loadSampleData(new File("./data/samples/bass.wav"), format);


        /**
         * SAMPLES per Beats one bar -> 4 beats (SAMPLE_RATE * 60)/ BPM =
         * samples per beat
         */
        int STEPS_PER_BEAT = 4;
        SAMPLES_PER_STEP = (int) ((SAMPLE_RATE * 60.0 / BPM) / STEPS_PER_BEAT);
        System.out.println("samples per step: " + SAMPLES_PER_STEP);
        
        samples.add(new Sample(kick_sample, "bass.wav"));
        samples.add(new Sample(snare_sample,LoopMode.NO_LOOPING,0, SAMPLES_PER_STEP));
        samples.add(new Sample(stuff, LoopMode.LOOPING, (int)(SAMPLES_PER_STEP*2), 2*SAMPLES_PER_STEP));
        samples.add(new Sample(synth_sample,"VEC2 Cymbals HH Closed 01.wav"//"VMH2 Toms & Deep Tones 086 G#.wav"
        		));
        // off_sample = loadSampleData(new File("SwizzBeats0025.wav"), format);
        // vocal_sample = loadSampleData(new File("VVE2 Vocal FX 025.wav"), format);

       

        channels.add(new Channel(SAMPLES_PER_STEP));
        channels.add(new Channel(SAMPLES_PER_STEP));
        channels.add(new Channel(SAMPLES_PER_STEP));
        channels.add(new Channel(SAMPLES_PER_STEP));

        channels.add(new Channel(SAMPLES_PER_STEP));
        channels.add(new Channel(SAMPLES_PER_STEP));
        channels.add(new Channel(SAMPLES_PER_STEP));
        channels.add(new Channel(SAMPLES_PER_STEP));


        
        noteMultiplyer = new double[60];

        for (int i = 0; i < 60; i++) {
            noteMultiplyer[i] = Math.pow(1.059f, i - 12);
        }
        pattern = new Pattern();
    }

    class Note {

        Integer note; // 0 - 127 actually this is the note object!
        Integer instrument; // 0-63
        Integer volume; // 0-63 or velocitx
        boolean noteOff; // NoteEventTyp.ON / OFF

        // int effect;

        public Note(Integer note, Integer instruemtn, Integer volume) {
            this.note = note;// TODO Auto-generated constructor stub
            this.instrument = instruemtn;
            this.volume = volume;
        }
    }

    class Pattern {

        ArrayList<ArrayList<Note>> channels = new ArrayList<ArrayList<Note>>();

        public Pattern() {
            ArrayList<Note> channel = new ArrayList<DarkTracker.Note>();
            for (int i = 0; i < 32; i++) {
              
               channel.add(new Note(null, null, null));
            }
            channels.add(channel);

            channel = new ArrayList<DarkTracker.Note>();
            for (int i = 0; i < 32; i++) {
              
                channel.add(new Note(null, null, null));
            }
           
            channels.add(channel);

            channel = new ArrayList<DarkTracker.Note>();
            for (int i = 0; i < 32; i++) {
                channel.add(new Note(null, null, null));
            }
            channels.add(channel);

            channel = new ArrayList<DarkTracker.Note>();
            for (int i = 0; i < 32; i++) {

                channel.add(new Note(null, null, null));
            }
            channels.add(channel);
            
            channel = new ArrayList<DarkTracker.Note>();
            for (int i = 0; i < 32; i++) {

                channel.add(new Note(null, null, null));
            }
            channels.add(channel);
            
            channel = new ArrayList<DarkTracker.Note>();
            for (int i = 0; i < 32; i++) {

                channel.add(new Note(null, null, null));
            }
            channels.add(channel);
            
            channel = new ArrayList<DarkTracker.Note>();
            for (int i = 0; i < 32; i++) {

                channel.add(new Note(null, null, null));
            }
            channels.add(channel);
            
            channel = new ArrayList<DarkTracker.Note>();
            for (int i = 0; i < 32; i++) {

                channel.add(new Note(null, null, null));
            }
            channels.add(channel);
        }
    }

    public void play() {
        audioRenderingThread = new Thread() {

            @Override
            public void run() {
                // is this really needed?
                setPriority(Thread.MAX_PRIORITY);

                try {
                    auline.open(format,SAMPLES_PER_STEP);
                } catch(LineUnavailableException e) {
                    return;
                }

                auline.start();

                byte[] mixBuffer = new byte[SAMPLES_PER_STEP * 2]; // short is 2
                // bytes
                int[] intMixBuffer = new int[SAMPLES_PER_STEP];
                int[] emptyMixBuffer = new int[SAMPLES_PER_STEP];
                Arrays.fill(emptyMixBuffer, 0);

                while (!stop) {

                    for (int i = 0; i < pattern.channels.size(); i++) {
                        if (pattern.channels.get(i).get(step).note != null) {
                            
                            channels.get(i).note = pattern.channels.get(i).get(step).note;
                            channels.get(i).samplePosition = 0;
                            
                            if(pattern.channels.get(i).get(step).volume!= null)
                                channels.get(i).volume = pattern.channels.get(i).get(step).volume;
                            if(pattern.channels.get(i).get(step).instrument != null)
                                channels.get(i).sample = samples.get(pattern.channels.get(i).get(step).instrument);
                        } else if(pattern.channels.get(i).get(step).noteOff == true) {
                            channels.get(i).sample = null;
                        }
                    }

                    step = (step + 1) % cymbal.length;
                    System.out.println("step " + step);
                    System.out.println("getstep " + getStep());
                    // Arrays.fill(intMixBuffer, 0);
                    System.arraycopy(emptyMixBuffer, 0, intMixBuffer, 0, emptyMixBuffer.length);

                    int[] channelMixBuffer;
                    for (Channel channel : channels) {
                        channelMixBuffer = channel.mix();
                        for (int i = 0; i < intMixBuffer.length; i++) {
                            intMixBuffer[i] += channelMixBuffer[i];
                        }
                    }

                    // add clipping here!!
                    for (int i = 0; i < intMixBuffer.length; i++) {
                        int sample = (intMixBuffer[i] * 304) / (channels.size() * 64);
                        short shortsample = (short) (Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sample)));
                        mixBuffer[i * 2 + 1] = (byte) ((shortsample) & 0xFF);
                        mixBuffer[i * 2] = (byte) ((shortsample >> 8) & 0xFF);
                    }

                    auline.write(mixBuffer, 0, mixBuffer.length);
                }
                auline.drain();
                auline.stop();
                auline.close();
                stop = false;
                startStep = step;
                System.out.println("getstep " + getStep());
            }
        };
        audioRenderingThread.start();
    }

    public void nonBlockingPause() {
        stop = true;

    }
    
    
    public void pause() {
        stop = true;
        try {
            audioRenderingThread.join();
        } catch(InterruptedException e) {
        }
        // TODO: reset playback position
        stop = false;
    }

    public void stop() {
        stop = true;
        try {
            audioRenderingThread.join();
        } catch(InterruptedException e) {
        }
        // TODO: reset playback position
        stop = false;
        step = 0;
        part = 0;

        // reset channels
        for (Channel channel : channels) {
            channel.sample = null;
            channel.samplePosition = 0;
        }
    }

    // MIDI Note Numbers 0 to 127
    public static final String[] noteRepresentations = new String[] {"C-", "C#", "D-", "D#", "E-", "F-", "F#", "G-",
            "G#", "A-", "A#", "B-"};

    public static void main(String[] args) throws LineUnavailableException, UnsupportedAudioFileException, IOException {
        // note, instrument, volume, stuff

        int note = 55;
        System.out.println(noteRepresentations[note % 12] + (note / 12));

        DarkTracker tracker = new DarkTracker();

        tracker.play();

        try {
            Thread.sleep(10000);
        } catch(InterruptedException e) {
        }

        tracker.pause();
//
//        try {
//            Thread.sleep(1000);
//        } catch(InterruptedException e) {
//        }

        tracker.play();
    }

}

class Mixer {

    int masterVolume = 0;
    ArrayList<Channel> channels = new ArrayList<Channel>();

    public Mixer() {

    }

}

class Channel {

    Sample sample = null;
    int volume = 64;
    int sampleLength;
    double samplePosition;
    int[] mixBuffer;
    int note;
    boolean loop;
    int loopLength;
    int loopStart;
    

    public Channel(int size) {
        mixBuffer = new int[size];
    }

    // TODO: add looping points to sample: loop/pingpongetc
    public int[] mix() {
        // optimize loop!!!
        // computing step size once and not per sample drops from 200 to 150
        // micro seconds
        double stepSize =  Math.pow(1.059f, note-(12*5));
        //DarkTracker.noteMultiplyer[note + 12];

        // http://deku.rydia.net/program/sound2.html
        for (int i = 0; i < mixBuffer.length; i++) {

            if (sample != null) {
                if (samplePosition < sample.sampleData.length) {
                    mixBuffer[i] = (sample.sampleData[(int) samplePosition] * sample.volume / 64);
                    // mixBuffer[i] = (short)(interp(samplePosition)*volume/64);
                    samplePosition = (samplePosition + stepSize);
                    if(sample.loopMode == LoopMode.LOOPING) {
                        while(samplePosition >= sample.loopStart + sample.loopLength) {
                            samplePosition -= sample.loopLength;
                        }
                    }
                } else {
                    mixBuffer[i] = 0;
                    sample = null;
                }
            } else {
                mixBuffer[i] = 0;
                Arrays.fill(mixBuffer, i, mixBuffer.length, 0);
                break;
            }
        }

        return mixBuffer;
    }

}