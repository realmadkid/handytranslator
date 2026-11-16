package com.handytranslator.google;
import java.util.*;
public class CoreTest {
 static void eq(Object a,Object b){if(!a.equals(b))throw new AssertionError(a+" != "+b);}
 public static void main(String[] args){List<String> out=new ArrayList<>();ButtonGate g=new ButtonGate(new ButtonGate.Sink(){public void down(int d){out.add("d"+d);}public void up(){out.add("u");}});
  g.accept(1);g.accept(0);g.accept(1);g.accept(1);g.accept(3);g.accept(2);g.accept(0);g.accept(2);g.accept(0);eq(out.toString(),"[d0, u, d1, u]");
  out.clear();g.accept(3);g.accept(1);g.accept(0);g.accept(2);g.reset();g.accept(2);g.accept(0);eq(out.toString(),"[d1, u]");
  List<String> cc=new ArrayList<>();MidiParser p=new MidiParser((c,v)->cc.add(c+":"+v));p.accept(new byte[]{(byte)191,103},0,2);p.accept(new byte[]{(byte)248,42,104,3},0,4);eq(cc.toString(),"[103:42, 104:3]");
  p.reset();p.accept(new byte[]{104,1,(byte)240,104,2,(byte)247},0,6);eq(cc.size(),2);System.out.println("CoreTest PASS: reconnect, duplicate, overlap, simultaneous, fragmented MIDI, realtime and SysEx");
 }
}
