package com.handytranslator.google;
import android.content.Context;
import android.media.midi.*;
import android.os.*;
import java.io.IOException;

// Derived from the reference transport; a distinct handshake prevents old firmware triggering speech.
final class MidiBridge {
 interface Listener{void buttons(int mask);void disconnected();}
 private final MidiManager manager;private final Handler h;private final Listener listener;
 private MidiDevice device;private MidiInputPort input;private MidiOutputPort output;
 private volatile int generation;private int id=-1;private boolean opening,closed,verified;private long lastSeen;
 private final MidiParser parser;
 private final MidiReceiver receiver;
 private final MidiManager.DeviceCallback callback=new MidiManager.DeviceCallback(){
  public void onDeviceAdded(MidiDeviceInfo i){scan();}
  public void onDeviceRemoved(MidiDeviceInfo i){if(i.getId()==id){disconnect();scan();}}
 };
 private final Runnable pulse=new Runnable(){public void run(){if(closed)return;if(device==null)scan();else{
  if(lastSeen!=0&&SystemClock.elapsedRealtime()-lastSeen>2500){disconnect();}else send(102,42);
 }h.postDelayed(this,500);}};
 MidiBridge(Context c,Handler handler,Listener l){h=handler;listener=l;manager=(MidiManager)c.getSystemService(Context.MIDI_SERVICE);
  parser=new MidiParser((cc,v)->{if(cc==103&&v==42){verified=true;lastSeen=SystemClock.elapsedRealtime();}if(cc==104&&v<4&&verified){lastSeen=SystemClock.elapsedRealtime();listener.buttons(v);}});
  receiver=new MidiReceiver(){public void onSend(byte[] b,int off,int count,long t){byte[] copy=new byte[count];System.arraycopy(b,off,copy,0,count);final int token=generation;h.post(()->{if(token==generation&&!closed)parser.accept(copy,0,copy.length);});}};
  if(manager!=null)manager.registerDeviceCallback(callback,h);h.post(pulse);
 }
 private void scan(){if(closed||manager==null||opening||device!=null)return;
  MidiDeviceInfo selected=null;for(MidiDeviceInfo i:manager.getDevices()){if(i.getType()==MidiDeviceInfo.TYPE_USB&&i.getInputPortCount()>0&&i.getOutputPortCount()>0){String name=i.getProperties().getString(MidiDeviceInfo.PROPERTY_NAME,"").toLowerCase(java.util.Locale.ROOT);if(name.contains("leonardo")||name.contains("handytranslator")||name.contains("pro micro")){if(selected!=null)return;selected=i;}}}
  if(selected==null)return;opening=true;id=selected.getId();final int ticket=++generation;
  try{manager.openDevice(selected,d->{if(ticket!=generation||closed){close(d);return;}opening=false;if(d==null){id=-1;return;}device=d;input=d.openInputPort(0);output=d.openOutputPort(0);if(input==null||output==null){disconnect();return;}output.connect(receiver);lastSeen=SystemClock.elapsedRealtime();send(102,42);},h);}catch(RuntimeException e){disconnect();}
 }
 void send(int cc,int v){if(input!=null)try{input.send(new byte[]{(byte)191,(byte)cc,(byte)v},0,3);}catch(IOException e){disconnect();}}
 private static void close(java.io.Closeable c){if(c!=null)try{c.close();}catch(IOException ignored){}}
 private void disconnect(){generation++;opening=false;verified=false;parser.reset();close(output);close(input);close(device);output=null;input=null;device=null;id=-1;lastSeen=0;listener.disconnected();}
 void close(){closed=true;h.removeCallbacks(pulse);if(manager!=null)manager.unregisterDeviceCallback(callback);disconnect();}
}
