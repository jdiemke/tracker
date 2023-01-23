package com.github.jdiemke.tracker;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import com.github.jdiemke.tracker.DarkTracker.Pattern;
import com.github.jdiemke.tracker.DarkTracker.Sample;

/**
 * TODO:
 * - pattern editieren vereinfachen
 * - loop points einbauen
 * - sample klasse
 * - abfrage der play position optimieren!!!
 * @author jdiemke
 */
public class AWTWindow {

    static int patternX = 0;
    static int patternY = 0;
    static int instrument = 0;
    static Texture texture, texture2, content, button,pixelfont;
    static HashMap<Integer, Integer> vkToNoteMapping;
    static HashSet<Integer> sampleIds;
    static DarkTracker tracker ;
    static Node gui;
    static Node dragNode = null;
    static Frame frame = null; 
    static int  otctave = 5;
    
    static int mousex = 0;
    // MIDI Note Numbers 0 to 127

    public static final String[] noteRepresentations = new String[] {
        "C-", "C#", "D-", "D#", "E-", "F-", "F#", "G-", "G#", "A-", "A#", "B-"
    };
    
    public static String midiNoteToString(int midiNote) {
        return noteRepresentations[midiNote%12]+(midiNote/12);
    }
    public static void main(String[] args) throws LineUnavailableException, UnsupportedAudioFileException, IOException, MidiUnavailableException {
    	
    	// algorithm search all devices that can be transmit midi and add a receiver to them
    	MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();
    	
    	for(MidiDevice.Info info2 : info)
    		System.out.println("x" +info2);
    	MidiDevice device=null;
    	try {
			device = MidiSystem.getMidiDevice(info[1]);
		} catch (MidiUnavailableException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	device.open();
    	//
    	device.getTransmitter().setReceiver(new Receiver() {
			
			@Override
			public void send(MidiMessage message, long timeStamp) {
				if(message instanceof ShortMessage) {
					if(message.getStatus() == 0x90) {// NOTE ON
						System.out.println("status:" + message.getStatus());
						System.out.println("note:" + AWTWindow.midiNoteToString(((ShortMessage)message).getData1()));
						
						int midiNote = ((ShortMessage)message).getData1();
						 Pattern pattern = tracker.pattern;
		                    int j = Math.min(pattern.channels.size()-1,Math.max(0,patternX));
		                    int i = Math.min(pattern.channels.get(0).size()-1,Math.max(0,patternY));
		                    if(true) {
		                    	int step = tracker.getStep();
			                    //pattern.channels.get(j).get(step).note = midiNote;
			                    //pattern.channels.get(j).get(step).instrument = instrument;
			//                    if(instrument==0)
			//                    	pattern.channels.get(j).get(i).volume = 45;
			              
		                    	AWTWindow.tracker.channels.get(4).note = midiNote;
		                    	AWTWindow.tracker.channels.get(4).sample = AWTWindow.tracker.samples.get(instrument);
		                    	AWTWindow.tracker.channels.get(4).samplePosition = 0;
		                    }
					}
				}
				
			}
			
			@Override
			public void close() {
				// TODO Auto-generated method stub
				
			}
		});
    	
        tracker = new DarkTracker();
        tracker.pattern.channels.get(0).get(0).note = 60;
        tracker.pattern.channels.get(0).get(0).instrument = 0;

        tracker.pattern.channels.get(1).get(0).note = 60;
        tracker.pattern.channels.get(1).get(0).instrument = 1;

        tracker.pattern.channels.get(2).get(0).note = 60;
        tracker.pattern.channels.get(2).get(0).instrument = 2;

        vkToNoteMapping = new HashMap<Integer, Integer>();
        vkToNoteMapping.put(KeyEvent.VK_Y,  0); // C
        vkToNoteMapping.put(KeyEvent.VK_S,  1); // C#
        vkToNoteMapping.put(KeyEvent.VK_X,  2); // D
        vkToNoteMapping.put(KeyEvent.VK_D,  3); // D#
        vkToNoteMapping.put(KeyEvent.VK_C,  4); // E
        vkToNoteMapping.put(KeyEvent.VK_V,  5); // F
        vkToNoteMapping.put(KeyEvent.VK_G,  6); // F#
        vkToNoteMapping.put(KeyEvent.VK_B,  7); // G
        vkToNoteMapping.put(KeyEvent.VK_H,  8); // G#
        vkToNoteMapping.put(KeyEvent.VK_N,  9); // A
        vkToNoteMapping.put(KeyEvent.VK_J, 10); // A#
        vkToNoteMapping.put(KeyEvent.VK_M, 11); // B
        sampleIds = new HashSet<Integer>();
        sampleIds.add(KeyEvent.VK_0);
        sampleIds.add(KeyEvent.VK_1);
        sampleIds.add(KeyEvent.VK_2);
        sampleIds.add(KeyEvent.VK_3);
        frame = new JFrame();

        frame.setUndecorated(true);
        System.setProperty("jogl.disable.openglcore", "true");
        final GLCapabilities glcapabilities = new GLCapabilities(GLProfile.getDefault());
        glcapabilities.setStencilBits(8);
        glcapabilities.setNumSamples(8);
        glcapabilities.setSampleBuffers(true);
        glcapabilities.setDoubleBuffered(true);
   
        
        final GLJPanel canvas = new GLJPanel(glcapabilities);
        canvas.setIgnoreRepaint (true);
        buildGUI();

        canvas.addMouseListener(new MouseListener() { 
		    @Override
		    public void mouseReleased(MouseEvent e) {
		    	System.out.println("released in main");
		    	AWTWindow.mouseReleased(e);
		    }
		    
		    @Override
		    public void mousePressed(MouseEvent e) {
		    System.out.println("pressed in main");
			// TODO Auto-generated method stub
		    	AWTWindow.mousePressed(e);
		    }
		    
		    @Override
		    public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub
			
		    }
		    
		    @Override
		    public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub
			
		    }
		    
		    @Override
		    public void mouseClicked(MouseEvent e) {
		    	System.out.println("clicked in main");
				
				AWTWindow.mouseClicked(e);
		    }
        });
        
        canvas.addMouseMotionListener(new MouseMotionListener() {
			
			@Override
			public void mouseMoved(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
				System.out.println("dragged main window");
				AWTWindow.mouseDragger(e);	
			}
		});
        
        frame.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void keyReleased(KeyEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void keyPressed(KeyEvent e) {
                System.out.println(e.getKeyChar());
                if(e.getKeyCode() == KeyEvent.VK_RIGHT)
                    patternX++;
                if(e.getKeyCode() == KeyEvent.VK_LEFT)
                    patternX--;
                if(e.getKeyCode() == KeyEvent.VK_DOWN)
                    patternY++;
                if(e.getKeyCode() == KeyEvent.VK_UP)
                    patternY--;
                
                if(e.getKeyCode() == KeyEvent.VK_PAGE_UP)
                	otctave = Math.min(otctave+1, 10);
                if(e.getKeyCode() == KeyEvent.VK_PAGE_DOWN)
                	otctave = Math.max(otctave-1, 0);

                System.out.println("x:" +patternX + " y:"+patternY);
                System.out.println(e.getKeyCode());
                
                boolean editMode = true;
               
                if(vkToNoteMapping.keySet().contains(e.getKeyCode())) {
                    System.out.println("char=" + e.getKeyChar() + ", mapped code="+vkToNoteMapping.get(e.getKeyCode()));
                    int midiNote = vkToNoteMapping.get(e.getKeyCode()) +12*otctave;
                    System.out.println("octave: " + otctave);
                    System.out.println(noteRepresentations[midiNote%12]+(midiNote/12));
                    Pattern pattern = tracker.pattern;
                    int j = Math.min(pattern.channels.size()-1,Math.max(0,patternX));
                    int i = Math.min(pattern.channels.get(0).size()-1,Math.max(0,patternY));
                    if(editMode) {
	                    pattern.channels.get(j).get(i).note = midiNote;
	                    pattern.channels.get(j).get(i).instrument = instrument;
	//                    if(instrument==0)
	//                    	pattern.channels.get(j).get(i).volume = 45;
	                    patternY++;
                    } else {
                    	AWTWindow.tracker.channels.get(0).note = midiNote;
                    	AWTWindow.tracker.channels.get(0).sample = AWTWindow.tracker.samples.get(instrument);
                    	AWTWindow.tracker.channels.get(0).samplePosition = 0;
                    }
                }
                
                if(e.getKeyCode() == KeyEvent.VK_P){
                    tracker.play();
                }
                
                if(e.getKeyCode() == KeyEvent.VK_DELETE){
                    Pattern pattern = tracker.pattern;
                    int j = Math.min(pattern.channels.size()-1,Math.max(0,patternX));
                    int i = Math.min(pattern.channels.get(0).size()-1,Math.max(0,patternY));
                    pattern.channels.get(j).get(i).note = null;
                    patternY++;
                }
                
                if(sampleIds.contains(e.getKeyCode())) {
                    instrument = e.getKeyCode() -'0';
                }
            }
        });

        canvas.addGLEventListener(new GLEventListener() {

            

            @Override
            public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
                GL2 gl2 = drawable.getGL().getGL2();
                gl2.glMatrixMode(GL2.GL_PROJECTION);
                gl2.glLoadIdentity();

                // coordinate system origin at lower left with width and height same as the window
                GLU glu = new GLU();
                glu.gluOrtho2D(0.0f, width, height, 0);
                //gl2.glOrtho(-0.5, (width - 1) + 0.5, (height - 1) + 0.5, -0.5, 0.0, 1.0);

                gl2.glMatrixMode(GL2.GL_MODELVIEW);
                gl2.glLoadIdentity();

                gl2.glViewport(0, 0, width, height);

            }

            @Override
            public void init(GLAutoDrawable drawable) {
                GL2 gl2 = drawable.getGL().getGL2();
                gl2.setSwapInterval(1);
                gl2.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
                gl2.glDisable(GL2.GL_DEPTH_TEST);
               // gl2.glEnable(GL2.GL_STENCIL_TEST);
                try {
                    // Create a OpenGL Texture object from (URL, mipmap, file suffix)
                    // Use URL so that can read from JAR and disk file.
                    System.out.println(new FileInputStream("./data/images/font.png"));

                    texture = TextureIO.newTexture(new FileInputStream("./data/images/font.png"), // relative to project root
                            false, null);

                    // Use linear filter for texture if image is larger than the original texture
                    gl2.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
                    // Use linear filter for texture if image is smaller than the original texture
                    gl2.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);

                    // Texture image flips vertically. Shall use TextureCoords class to retrieve
                    // the top, bottom, left and right coordinates, instead of using 0.0f and 1.0f.
                    
                    texture2 = TextureIO.newTexture(new FileInputStream("./data/images/caption.png"), // relative to project root
                            false, null);

                    // Use linear filter for texture if image is larger than the original texture
                    gl2.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
                    // Use linear filter for texture if image is smaller than the original texture
                    gl2.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);

                    
                    content = TextureIO.newTexture(new FileInputStream("./data/images/content.png"), // relative to project root
                            false, null);

                    // Use linear filter for texture if image is larger than the original texture
                    gl2.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
                    // Use linear filter for texture if image is smaller than the original texture
                    gl2.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
                    
                    button = TextureIO.newTexture(new FileInputStream("./data/images/button.png"), // relative to project root
                            false, null);

                    // Use linear filter for texture if image is larger than the original texture
                    gl2.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
                    // Use linear filter for texture if image is smaller than the original texture
                    gl2.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
                    
                    pixelfont = TextureIO.newTexture(new FileInputStream("./data/images/pixelfont.png"), // relative to project root
                            false, null);

                    // Use linear filter for texture if image is larger than the original texture
                    gl2.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
                    // Use linear filter for texture if image is smaller than the original texture
                    gl2.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
                } catch(GLException e) {
                    e.printStackTrace();
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void dispose(GLAutoDrawable drawable) {
                // TODO Auto-generated method stub

            }

            @Override
            public void display(GLAutoDrawable drawable) {
                GL2 gl2 = drawable.getGL().getGL2();

                /**
                 * DRAW GUI
                 */
                gl2.glClear(GL2.GL_COLOR_BUFFER_BIT);
                gl2.glLoadIdentity();
                texture.disable(gl2);
                drawGUI(gl2);
               
            }
        });
        
        canvas.setSize(640, 480);
        frame.add(canvas);
       // frame.pack();
        frame.setResizable(false);
        FPSAnimator anim = new FPSAnimator(canvas,30);
        
        frame.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
            	tracker.stop();
            	anim.stop();
                frame.dispose();
                System.exit(0);
            }
        });

        

      frame.pack();
      canvas.setFocusable(false);
        frame.setVisible(true);
       frame.setSize(640, 480);
        anim.start();
        frame.requestFocus();
    }
    
    protected static void mouseDragger(MouseEvent e) {
    	
    	if (AWTWindow.dragNode != null) {
    		System.out.println("inside " +dragNode.text);
    		dragNode.mouseDragged(e);
    	}
		
	}
	protected static void mouseReleased(MouseEvent e) {
		
	 	if (AWTWindow.dragNode != null) {
    		System.out.println("inside " +dragNode.text);
    		dragNode.mouseReleased(e.getPoint());
    	}
		
	}
    
	protected static void mousePressed(MouseEvent e) {
		if (AWTWindow.gui != null) {
			AWTWindow.gui.mousePressed(e.getPoint());
		}
	}
	protected static void mouseClicked(MouseEvent e) {
		if(AWTWindow.gui != null) {
			AWTWindow.gui.mouseClicked(e.getPoint());
		}
	}
	static void drawChar(GL2 gl, int x, int y, int character) {
        int width = 8;
        int height = 8;
        float dletax = (8 / 256.0f);
        float dletay = (8 / 16.0f);
        int pos = character - ' ';
        float xpos = ((pos % 32) * dletax);
        float ypos = ((pos / 32) * (dletay));

        gl.glBegin(GL2.GL_QUADS);
        gl.glTexCoord2f(xpos, ypos);
        gl.glVertex2f(x + 0, y + 0);

        gl.glTexCoord2f(xpos + dletax, ypos);
        gl.glVertex2f(x + width , y + 0);

        gl.glTexCoord2f(xpos + dletax, ypos + dletay);
        gl.glVertex2f(x + width , y + height);

        gl.glTexCoord2f(xpos, ypos + dletay);
        gl.glVertex2f(x + 0, y + height );
        gl.glEnd();
    }
	
	static void drawChar2(GL2 gl, int x, int y, int character) {
        int width = 12;
        int height = 8;
        float dletax = (12 / 192.0f);
        float dletay = (8 / 48.0f);
        int pos = character - ' ';
        float xpos = ((pos % 16) * dletax);
        float ypos = ((pos / 16) * (dletay));

        gl.glBegin(GL2.GL_QUADS);
        gl.glTexCoord2f(xpos, ypos);
        gl.glVertex2f(x + 0, y + 0);

        gl.glTexCoord2f(xpos + dletax, ypos);
        gl.glVertex2f(x + width , y + 0);

        gl.glTexCoord2f(xpos + dletax, ypos + dletay);
        gl.glVertex2f(x + width , y + height);

        gl.glTexCoord2f(xpos, ypos + dletay);
        gl.glVertex2f(x + 0, y + height );
        gl.glEnd();
    }

    static void drawText(GL2 gl, int x, int y, String text) {
        for (int i = 0; i < text.length(); i++) {
            drawChar(gl, x + i * 8, y, text.charAt(i));
        }
    }
    static int[] pixelWidth = new int[]{
    		4, 12, 12, 12, 12, 12,  8, 12, 12, 12, 12, 12, 12, 12,  3, 12,
    		7,  3,  7,  7, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12,
    		12, 7,  7,  7,  7, 7, 7, 7, 12, 12, 12, 7, 6, 9, 12, 7,
    		7, 12, 7, 7, 7, 7, 7, 12, 12, 12, 12, 12, 12, 12, 12, 12,
    		7, 7, 12, 7, 7, 7, 12, 3, 12, 3, 12, 7, 5, 9, 7, 7,
    		7, 12, 6, 7, 12, 7, 7, 7, 12, 12, 12, 12, 12, 12, 12, 12,
    };
    static void drawText2(GL2 gl, int x, int y, String text) {
    	int xx = x;
        for (int i = 0; i < text.length(); i++) {
            drawChar2(gl, xx, y, text.charAt(i));
            xx +=pixelWidth[text.charAt(i)- ' '];
        }
    }
    
    public static void buildGUI() {
    	Node window = new Node();
    	window.position = new Point(0, 0);
    	window.dimension = new Dimension(640, 480);
    	window.color = new Color(255,0, 255);
    	window.text = "MAIN WINDOW";
    	
    	Node content = new Content();
    	content.position = new Point(0, 24);
    	content.dimension = new Dimension(640, 456);
    	content.color = new Color(54,54, 54);
    	content.text = "CONTENT";
    	
    	Node button1 = new PlayButton();
    	button1.position = new Point(8-1, 8-1);
    	button1.dimension = new Dimension(8*10+2, 22);
    	button1.color = new Color(0,255, 0);
    	button1.text = "PLAY";
    	
    	Node button2 = new PauseButton();
    	button2.position = new Point(8+8*10+4-1, 8-1);
    	button2.dimension = new Dimension(8*10+2, 22);
    	button2.color = new Color(0,255, 0);
    	button2.text = "PAUSE";
    	
       	Node button3 = new PauseButton();
    	button3.position = new Point(8+8*10+4-1+8*10+2+2, 8-1);
    	button3.dimension = new Dimension(8*10+2, 22);
    	button3.color = new Color(0,255, 0);
    	button3.text = "STOP";
    	
    	Node pattern = new PatternView();
    	pattern.position = new Point(8, 8*3+4+8);
    	pattern.dimension = new Dimension((13*4+3)*8,32*8);
    	pattern.color = new Color(0,255, 0);
    	pattern.text = "PATTERN";
    	
    	Node sampleEditor = new SampleEditor();
    	sampleEditor.position = new Point(8, 32*8+8*3+4+8+8);
    	sampleEditor.dimension = new Dimension(640-16,480-(32*8+8*3+4+8+8+8+36));
    	sampleEditor.color = new Color((int)(255*0.2),(int)(255*0.2),(int)(255*0.2));
    	sampleEditor.text = "SAMPLE";
    	
    	Node slider = new Slider();
    	
    	slider.position = new Point(0, 0);
    	slider.dimension = new Dimension(200,32);
    	slider.color = new Color((int)(255*0.2),(int)(255*0.2),(int)(255*0.2));
    	slider.text = "SLIDER";
    	
    	sampleEditor.add(slider);
    	
    	content.add(button1);
    	content.add(button2);
    	content.add(button3);
    	content.add(pattern);
    	content.add(sampleEditor);
 
    	window.add(content);
    	
    	Node caption = new Caption();
    	caption.position = new Point(0, 0);
    	caption.dimension = new Dimension(640, 24);
    	caption.color = new Color(250,255, 100);
    	caption.text = "Dark Matter Tracker";
    	
    	Node close = new CloseButton();
    	close.position = new Point(640-33, 5);
    	close.dimension = new Dimension(28,14);
    	close.color = new Color(250,255, 100);
    	close.text = "CLOSE";
    	
    	caption.add(close);
    	
    	window.add(caption);
    	
    	AWTWindow.gui = window;
    }
    public static void drawGUI(GL2 gl) {	
    	AWTWindow.gui.draw(gl);
    }
    
    public static void mouseClicked() {
    	
    }
    
    public static void drawRect(GL2 gl) {
    	gl.glColor3ub((byte)0, (byte)255, (byte)0);
    	int x =0;
    	int y=0;
    	int width =640; // -1 not necessary because rasterization leaves last pixel
    	int height=480;
    	gl.glBegin(GL2.GL_QUADS);
    		gl.glVertex2i(x,y+  0);
    		gl.glVertex2i(x+width,y+  0);
    		gl.glVertex2i(x+width,y+ height);
    		gl.glVertex2i(x+0,y+ height);
    	gl.glEnd();
    }

}

class Node {
	
	ArrayList<Node> childElements = new ArrayList<Node>();
	Point position;
	Dimension dimension;
	String text = new String();
	Color color = new Color(255,255,255);
	
	MouseListener mouseListener = null;
	
	
	public void add(Node node) {
		childElements.add(node);
	}
	
	public void mouseDragged(MouseEvent e) {
		mouseDraggedAction(e);
	}

	public void mouseDraggedAction(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mousePressed(Point point) {
		if(contains(point)) {
			for(Node child : childElements) {
				if (child.contains(new Point(point.x - position.x, point.y - position.y))) {
					child.mousePressed(new Point(point.x - position.x, point.y - position.y));
					return;
				}
				
			}
			mousePressedAction(new Point(point.x - position.x, point.y - position.y));
			AWTWindow.dragNode = this;
			System.out.println("dragnode: " + this.text);
		}
		else {
			System.out.println("fuck");
		}
		
	}

	protected void mousePressedAction(Point point) {
		
	}
	
protected void mouseReleasedAction(Point point) {
		
	}

	public void mouseReleased(Point point) {
		mouseReleasedAction(point);
		
	}

	public void mouseClicked(Point point) {		
		if(contains(point)) {
			for(Node child : childElements) {
				if (child.contains(new Point(point.x - position.x, point.y - position.y))) {
					child.mouseClicked(new Point(point.x - position.x, point.y - position.y));
					return;
				}
				
			}
			mouseClickedAction(new Point(point.x - position.x, point.y - position.y));
			AWTWindow.dragNode = this;
			System.out.println("dragnode: " + this.text);
		}
	}
	
	public boolean contains(Point point) {
		Rectangle rect = new Rectangle(position.x, position.y, dimension.width, dimension.height);
		return rect.contains(point);
	}
	
	public void mouseClickedAction(Point point) {
		System.out.println("CLICK"+text + " : " + "mouse pos: " + point);
	}
	
	void draw(GL2 gl) {
    	int x =position.x;
    	int y=position.y;
    	int width =dimension.width; // -1 not necessary because rasterization leaves last pixel
    	int height=dimension.height;
    	
		 
		/****/
		 ///////////////
   
		gl.glEnable(GL2.GL_STENCIL_TEST);
		//gl.glStencilMask(~0);
		gl.glStencilMask(0xFF);
		gl.glClearStencil(0x0);
        gl.glClear(GL2.GL_STENCIL_BUFFER_BIT);
        
        gl.glStencilFunc(GL2.GL_ALWAYS, 1, 1);
        gl.glStencilOp(GL2.GL_REPLACE, GL2.GL_REPLACE, GL2.GL_REPLACE);
        
        
        // draw here
        gl.glPushMatrix(); // stack size is at least 32 so limit pushes to 32
		gl.glTranslatef(x, y, 0);
		gl.glColor3ub((byte)color.getRed(), (byte)color.getGreen(), (byte)color.getBlue());
        
        gl.glBegin(GL2.GL_QUADS);
		gl.glVertex2i(0,0+  0);
		gl.glVertex2i(0+width,0+  0);
		gl.glVertex2i(0+width,0+ height);
		gl.glVertex2i(0+0,0+ height);
	gl.glEnd();
    	
        gl.glStencilMask(0);
        gl.glStencilFunc(GL2.GL_EQUAL, 1, 1);
        gl.glStencilOp(GL2.GL_KEEP, GL2.GL_KEEP, GL2.GL_KEEP);
        
       // draw here


    	AWTWindow.texture.enable(gl);
    	AWTWindow.drawText(gl, 0,0, text);
    	AWTWindow.texture.disable(gl);
    	
    	drawContent(gl);
    	
    	  gl.glDisable(GL2.GL_STENCIL_TEST);
  		/****/
    	 
    	for(Node node : childElements) {
    		node.draw(gl);
    	}
    	
    	gl.glPopMatrix();
	}
	
	void drawContent(GL2 gl) {
		
	}
	
}

class Caption extends Node {
	Point mouseDownCompCoords;
	
	@Override
	void drawContent(GL2 gl) {
		super.drawContent(gl);
		AWTWindow.texture2.enable(gl);
		AWTWindow.texture2.bind(gl);
		int x =position.x;
    	int y=position.y;
    	int width =dimension.width; // -1 not necessary because rasterization leaves last pixel
    	int height=dimension.height;
    	gl.glColor3f(1, 1, 1);
		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2f(0,0);
		gl.glVertex2i(x,y+  0);
		gl.glTexCoord2f(1,0);
		gl.glVertex2i(x+width,y+  0);
		gl.glTexCoord2f(1,1);
		gl.glVertex2i(x+width,y+ height);
		gl.glTexCoord2f(0,1);
		gl.glVertex2i(x+0,y+ height);
		gl.glEnd();
		
		AWTWindow.texture2.disable(gl);
		
		AWTWindow.pixelfont.enable(gl);
		AWTWindow.pixelfont.bind(gl);
		gl.glColor3f(0.17f, 0.17f, 0.17f);
    	AWTWindow.drawText2(gl, 9+1,8+1, text);
		gl.glColor3f(0.7f, 0.7f, 0.7f);
    	AWTWindow.drawText2(gl, 9,8, text);
    	AWTWindow.pixelfont.disable(gl);
    
	}
	public void mouseDraggedAction(MouseEvent e) {
		AWTWindow.frame.setLocation(e.getLocationOnScreen());
		
	}
	

}

class Content extends Node {

	
	@Override
	void drawContent(GL2 gl) {
		super.drawContent(gl);
		AWTWindow.content.enable(gl);
		AWTWindow.content.bind(gl);
		int x =position.x;
    	int y=position.y;
    	int width =dimension.width; // -1 not necessary because rasterization leaves last pixel
    	int height=dimension.height;
    	gl.glColor3f(1, 1, 1);
		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2f(0,0);
		gl.glVertex2i(0,0+  0);
		gl.glTexCoord2f(1,0);
		gl.glVertex2i(0+width,0+  0);
		gl.glTexCoord2f(1,1);
		gl.glVertex2i(0+width,0+ height);
		gl.glTexCoord2f(0,1);
		gl.glVertex2i(0+0,0+ height);
		gl.glEnd();
		
		AWTWindow.content.disable(gl);
		
		AWTWindow.texture.enable(gl);
		AWTWindow.texture.bind(gl);
		gl.glColor3f(0.7f, 0.7f, 0.7f);
    	AWTWindow.drawText(gl, 9,9, text);
    	
    	AWTWindow.pixelfont.bind(gl);
    	gl.glColor3f(0.19f, 0.19f, 0.19f);
    	AWTWindow.drawText2(gl, 300+1,9+1, "BPM: " +AWTWindow.tracker.BPM);
		gl.glColor3f(0.9f, 0.9f, 0.9f);
    	AWTWindow.drawText2(gl, 300,9, "BPM: " +AWTWindow.tracker.BPM);
    	//AWTWindow.pi.bind(gl);
    	for(int i=0; i < AWTWindow.tracker.getSamples().size(); i++) {
    		Sample sample = AWTWindow.tracker.getSamples().get(i);
    		String name = sample.name != null ? sample.name : "unknown";
    		gl.glColor3f(0.19f, 0.19f, 0.19f);
        	AWTWindow.drawText2(gl, 450+1,9+1+i*8,  name);
        	if(i == AWTWindow.instrument)
        		gl.glColor3f(0.9f, 0.6f, 0.6f);
        	else
        		gl.glColor3f(0.9f, 0.9f, 0.9f);
        	AWTWindow.drawText2(gl, 450,9+i*8, name);
    	}
    		
    	AWTWindow.texture.disable(gl);
    
	}

	

}

class CloseButton extends Node {
	
	@Override
	public void mouseClickedAction(Point point) {
		System.exit(0);
	}
	
	@Override
	void draw(GL2 gl) {
	
	}
}

class PatternView extends Node {
	
	@Override
	void drawContent(GL2 gl) {
		super.drawContent(gl);
		DarkTracker tracker = AWTWindow.tracker;
		 int step = tracker.getStep();
		 int offset = step*8;
		 offset = AWTWindow.patternY *8;
		//System.out.println("offset" + tracker.getStep());
		
		 gl.glPushMatrix();
		 
        gl.glColor3f(0.2f, 0.2f, 0.2f);
        gl.glBegin(GL2.GL_QUADS);

        gl.glVertex2f(0, 0 );
        gl.glVertex2f((13*4+3)*8,0);
        gl.glVertex2f((3+13*4)*8, 8*32);
        gl.glVertex2f(0, 8*32);
        gl.glEnd();
        
        
        gl.glTranslatef(0, 16*8-offset,0);

        gl.glColor3f(0.25f, 0.25f, 0.25f);
        gl.glBegin(GL2.GL_QUADS);

        gl.glVertex2f(0+3*8-4+1, 0 );
        gl.glVertex2f((13*8)+3*8-4-1,0);
        gl.glVertex2f((13*8)+3*8-4-1, 8*32);
        gl.glVertex2f(0+3*8-4+1, 8*32);
        
        gl.glVertex2f(0+3*8-4+(13*8)+1, 0 );
        gl.glVertex2f((13*8)+3*8-4+(13*8)-1,0);
        gl.glVertex2f((13*8)+3*8-4+(13*8)-1, 8*32);
        gl.glVertex2f(0+3*8-4+(13*8)+1, 8*32);
        gl.glEnd();
        
        gl.glColor3f(0.7f, 0.7f, 0.3f);
        gl.glBegin(GL2.GL_QUADS);

        gl.glVertex2f(0, 0 + offset);
        gl.glVertex2f((3+13*4)*8, 0 + offset);
        gl.glVertex2f((3+13*4)*8, 8  + offset);
        gl.glVertex2f(0, 8  + offset);
        gl.glEnd();
  
        
        AWTWindow.texture.bind(gl);
        AWTWindow.texture.enable(gl);
        
        for(int i =0; i < 32; i++) {
          if (i % 4 == 0 && i != step) {
        	  AWTWindow.texture.disable(gl);
          gl.glColor3f(0.3f, 0.4f, 0.3f);
          gl.glBegin(GL2.GL_QUADS);

          gl.glVertex2f(0, 0 + i * 8 );
          gl.glVertex2f((3+13*4)  * 8, 0 + i * 8 );
          gl.glVertex2f((3+13*4) * 8, 8  + i * 8 );
          gl.glVertex2f(0, 8  + i * 8 );
          gl.glEnd();
          }
          AWTWindow.texture.enable(gl);
          
          if(i%4==0)
              gl.glColor3f(0.9f, 1.0f, 0.9f);
          else
              gl.glColor3f(0.7f, 0.7f, 0.7f);
          AWTWindow.drawText(gl, 0, 8 * i,  String.format("%02d", i));
        }
        Pattern pattern = tracker.getPattern();
        for(int j =0; j < pattern.channels.size(); j++)
        for(int i =0; i < pattern.channels.get(j).size(); i++) {
            Integer midiNote = pattern.channels.get(j).get(i).note;
            Integer instrument = pattern.channels.get(j).get(i).instrument;
            Integer volume = pattern.channels.get(j).get(i).volume;
            boolean off = pattern.channels.get(j).get(i).noteOff;
            String cell =  "";
            if(off) {
        	cell += "OFF";
            } else  {
        	cell += (midiNote != null) ? AWTWindow.midiNoteToString(midiNote) : "---";
            }
            cell = cell + ( instrument != null ? String.format("%02d", instrument) : "--");
            cell = cell + ( volume != null ? String.format(" %02d", volume) : " --");
            cell = cell + " ---";
            if(i%4==0)
                gl.glColor3f(0.9f, 1.0f, 0.9f);
            else
                gl.glColor3f(0.7f, 0.7f, 0.7f);
            AWTWindow.drawText(gl, j*13*8+3*8, 8 * i, cell);
        }
        AWTWindow.texture.bind(gl);  // same as g
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
     
        gl.glColor3f(0.7f, 0.7f, 0.3f);
        
        int j = Math.min(pattern.channels.size()-1,Math.max(0,AWTWindow.patternX));
        int i = Math.min(pattern.channels.get(0).size()-1,Math.max(0,AWTWindow.patternY));
        int xpos = j*13*8+3*8;
        int ypos = 8 * i;
        AWTWindow.texture.disable(gl);
        gl.glBegin(GL2.GL_QUADS);

        gl.glVertex2f(xpos, ypos);
        gl.glVertex2f(xpos+13*8, ypos);
        gl.glVertex2f(xpos+13*8, ypos+8);
        gl.glVertex2f(xpos, ypos+8);
        gl.glEnd();
        AWTWindow.texture.enable(gl);
        gl.glColor4f(0.8f, 0.4f, 0.4f, 1.0f);
        
        
        Integer midiNote = pattern.channels.get(j).get(i).note;
        Integer instrument = pattern.channels.get(j).get(i).instrument;
        Integer volume = pattern.channels.get(j).get(i).volume;
        String cell =  midiNote != null ? AWTWindow.midiNoteToString(midiNote) : "---";
        cell = cell + ( instrument != null ? String.format("%02d", instrument) : "--");
        cell = cell + ( volume != null ? String.format(" %02d", volume) : " --");
        cell = cell + " ---";
     
            gl.glColor3f(0.7f, 0.7f, 0.3f);
            AWTWindow.drawText(gl, j*13*8+3*8, 8 * i, cell);
 
            
            gl.glPopMatrix();
            
            AWTWindow.pixelfont.disable(gl);
            gl.glColor3f(0.15f, 0.15f, 0.15f);
            gl.glBegin(GL2.GL_QUADS);

            gl.glVertex2f(0, 0 );
            gl.glVertex2f(this.dimension.width,0);
            gl.glVertex2f(this.dimension.width, 16);
            gl.glVertex2f(0, 16);
            gl.glEnd();
            

            gl.glColor4f(.26f, 0.26f, 0.26f,1.0f);
            gl.glBegin(GL2.GL_QUADS);

            gl.glVertex2f(+3*8, 0+1 );
            gl.glVertex2f(13*8+3*8-2,0+1);
            gl.glVertex2f(13*8+3*8-2, 16-1);
            gl.glVertex2f(+3*8, 16-1);
            
            gl.glVertex2f(+3*8+(13*8*1), 0+1 );
            gl.glVertex2f((13*8*2)+3*8-2,0+1);
            gl.glVertex2f((13*8*2)+3*8-2, 16-1);
            gl.glVertex2f(+3*8+(13*8*1), 16-1);
            
            gl.glVertex2f(+3*8+(13*8*2), 0+1 );
            gl.glVertex2f((13*8*3)+3*8-2,0+1);
            gl.glVertex2f((13*8*3)+3*8-2, 16-1);
            gl.glVertex2f(+3*8+(13*8*2), 16-1);
            
            gl.glVertex2f(+3*8+(13*8*3), 0+1 );
            gl.glVertex2f((13*8*4)+3*8-2,0+1);
            gl.glVertex2f((13*8*4)+3*8-2, 16-1);
            gl.glVertex2f(+3*8+(13*8*3), 16-1);
            
            gl.glEnd();
            
          	 AWTWindow.pixelfont.enable(gl);
       		AWTWindow.pixelfont.bind(gl);
       		gl.glColor3ub((byte)255,(byte)255,(byte)255);
        	AWTWindow.drawText2(gl, 3*8+(13*8)*0,4, "       Track 01");
        	AWTWindow.drawText2(gl, 3*8+(13*8)*1,4, "       Track 02");
        	AWTWindow.drawText2(gl, 3*8+(13*8)*2,4, "       Track 03");
        	AWTWindow.drawText2(gl, 3*8+(13*8)*3,4, "       Track 04");
        	AWTWindow.pixelfont.disable(gl);
	}
	
}

class PlayButton extends Button {
	
	@Override
	public void mouseClickedAction(Point point) {
		AWTWindow.tracker.play();
	}
	

}

class SampleEditor extends Node {
	@Override
	void drawContent(GL2 gl) {
      

      Pattern pattern = AWTWindow.tracker.getPattern();
     // gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
      //
      int channelNum = pattern.channels.size();
      // same as gl.glEnable(texture.getTarget());

      AWTWindow.texture.disable(gl);
      int offset = AWTWindow.tracker.getStep()*8;
     
      gl.glColor3f(0.8f, 0.8f, 0.8f);
      gl.glBegin(GL2.GL_QUADS);

      gl.glVertex2f(0, 0 );
      gl.glVertex2f(640-16,0);
      gl.glVertex2f(640-16, 16);
      gl.glVertex2f(0,16);
      gl.glEnd();
      
      
      int[] sample = AWTWindow.tracker.samples.get(AWTWindow.instrument).sampleData;
      gl.glLineWidth(1.0f);
      
      if(sample != null) {
    	  
    	  AWTWindow.pixelfont.enable(gl);
    	  AWTWindow.pixelfont.bind(gl);
    	  String name = AWTWindow.tracker.samples.get(AWTWindow.instrument).name;
    	  gl.glColor3f(0.2f,0.2f,0.2f);
    	  AWTWindow.drawText2(gl, 4, 4, name != null ? name : "UNKNOWN");
    	  AWTWindow.pixelfont.disable(gl);
          int x = sample.length / 640;// 4 / 2 = 2
          gl.glColor3f(0.7f,1f,0.7f);
          gl.glEnable(GL2.GL_MULTISAMPLE);
          gl.glBegin(GL2.GL_LINE_STRIP);
              for(int i=0; i < 640; i++) {
          	gl.glVertex2f(i+0.5f,0.5f+sample[Math.min(i*x, sample.length-1)]/(float)Short.MAX_VALUE *60+60+16);
              }
          gl.glEnd();
          gl.glDisable(GL2.GL_MULTISAMPLE);
          gl.glColor3f(1,0,1);
//          
//          System.out.println("start: "+tracker.samples.get(1).loopStart);
//          System.out.println("length: "+tracker.samples.get(1).loopLength);
//          System.out.println("end: "+((tracker.samples.get(1).loopLength+tracker.samples.get(1).loopStart)/x));
//          gl.glBegin(GL2.GL_LINES);
//          
//      	gl.glVertex2f(0,100);
//      	gl.glVertex2f(640,100);
//      	
//      	gl.glVertex2f(0.5f,1);
//      	gl.glVertex2f(0.5f,100);
//      	
//      //	System.out.println("start new :" + (tracker.samples.get(2).loopStart/x));
//      	gl.glVertex2f(AWTWindow.tracker.samples.get(AWTWindow.instrument).loopStart/x+1,100-50);
//      	gl.glVertex2f(AWTWindow.tracker.samples.get(AWTWindow.instrument).loopStart/x+1,100+50);
//
//      	gl.glVertex2f((AWTWindow.tracker.samples.get(AWTWindow.instrument).loopStart+AWTWindow.tracker.samples.get(AWTWindow.instrument).loopLength)/x,100-50);
//      	gl.glVertex2f((AWTWindow.tracker.samples.get(AWTWindow.instrument).loopStart+AWTWindow.tracker.samples.get(AWTWindow.instrument).loopLength)/x,100+50);
//
//      	
//      	gl.glEnd();
      }

	}
}

class Button extends Node {
	
	boolean pressed = false;
	
	@Override
	protected void mousePressedAction(Point point) {
		pressed = true;
	}
	
	@Override
	protected void mouseReleasedAction(Point point) {
		pressed = false;
	}
	
	@Override
	void drawContent(GL2 gl) {
		super.drawContent(gl);
		AWTWindow.button.enable(gl);
		AWTWindow.button.bind(gl);
		int x =position.x;
    	int y=position.y;
    	int width =dimension.width; // -1 not necessary because rasterization leaves last pixel
    	int height=dimension.height;
    	gl.glColor3f(1, 1, 1);
		gl.glBegin(GL2.GL_QUADS);
			gl.glTexCoord2f(0,0);
			gl.glVertex2i(0,0+  0);
			gl.glTexCoord2f(1,0);
			gl.glVertex2i(0+width,0+  0);
			gl.glTexCoord2f(1,1);
			gl.glVertex2i(0+width,0+ height);
			gl.glTexCoord2f(0,1);
			gl.glVertex2i(0+0,0+ height);
		gl.glEnd();
		
		AWTWindow.button.disable(gl);
		
		AWTWindow.pixelfont.enable(gl);
		AWTWindow.pixelfont.bind(gl);
		gl.glColor3ub((byte)46,(byte)46,(byte)46);
    	AWTWindow.drawText2(gl, width/2-(text.length()*8/2)+1,(int)(height/2.0-8/2.0)+1, text);
		if(!pressed)
			gl.glColor3ub((byte)225,(byte)225,(byte)225);
		else
			gl.glColor3f(0,1,0);
    	AWTWindow.drawText2(gl, width/2-(text.length()*8/2),(int)(height/2.0-8/2.0), text);
    	AWTWindow.pixelfont.disable(gl);
    
	}
}

class Slider extends Node {
	float ratio=0.5f;
	
	@Override
	void drawContent(GL2 gl) {
		super.drawContent(gl);
		gl.glColor3f(1, 0, 0);
	gl.glBegin(GL2.GL_QUADS);
	
		gl.glVertex2i(0,0+  0);
	
		gl.glVertex2i((int)(this.dimension.getWidth()*ratio),0+  0);
	
		gl.glVertex2i((int)(this.dimension.getWidth()*ratio),(int)this.dimension.getHeight());
	
		gl.glVertex2i(0+0,(int)this.dimension.getHeight());
	gl.glEnd();
	}
	
	@Override
	protected void mousePressedAction(Point point) {
		 ratio = point.x / (float)this.dimension.width;
		AWTWindow.tracker.samples.get(AWTWindow.instrument).volume = (int)(ratio * 64);
	}

    @java.lang.Override
    public void mouseDragged(MouseEvent e) {
	    Point point = e.getPoint();
        ratio = point.x / (float)this.dimension.width;
        AWTWindow.tracker.samples.get(AWTWindow.instrument).volume = (int)(ratio * 64);
    }
}

class PauseButton extends Button {
	
	@Override
	public void mouseClickedAction(Point point) {
		AWTWindow.tracker.nonBlockingPause();
	}
	

}