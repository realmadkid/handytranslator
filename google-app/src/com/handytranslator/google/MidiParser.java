package com.handytranslator.google;
final class MidiParser {
 interface Sink{void cc(int controller,int value);}
 private int status,needed,index,first;
 private final Sink sink;
 MidiParser(Sink s){sink=s;}
 void reset(){status=needed=index=0;}
 void accept(byte[] bytes,int offset,int count){for(int i=offset;i<offset+count;i++){
  int b=bytes[i]&255;if(b>=248)continue;
  if(b>=128){index=0;status=b<240?b:0;needed=((b&224)==192)?1:2;continue;}
  if(status==0)continue;
  if(index==0)first=b;
  if(++index==needed){if(status==191)sink.cc(first,b);index=0;}
 }}
}
