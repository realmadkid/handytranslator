package com.handytranslator.google;
// Whole-state messages make simultaneous press and reconnect deterministic.
final class ButtonGate {
 interface Sink{void down(int direction);void up();}
 private boolean armed;private int active=-1,last;
 private final Sink sink;
 ButtonGate(Sink s){sink=s;}
 void reset(){if(active!=-1)sink.up();armed=false;active=-1;last=0;}
 void accept(int mask){if(mask<0||mask>3||mask==last&&armed)return;last=mask;
  if(!armed){if(mask==0)armed=true;return;}
  if(active!=-1){int bit=1<<active;if((mask&bit)==0){active=-1;sink.up();armed=mask==0;}return;}
  if(mask==3){armed=false;return;}
  if(mask==1||mask==2){active=mask==1?0:1;sink.down(active);}
 }
}
