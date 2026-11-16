#include <MIDIUSB.h>
// ATmega32U4, 5V/16 MHz. D4 and D5 each connect to GND through a NO push button.
const uint8_t pins[2]={4,5};
bool raw[2],stable[2];
unsigned long changed[2],lastHost=0,lastReport=0;
bool connected=false;
void cc(uint8_t control,uint8_t value){MidiUSB.sendMIDI({0x0B,0xBF,control,value});}
void report(){cc(103,42);cc(104,(stable[0]?1:0)|(stable[1]?2:0));MidiUSB.flush();lastReport=millis();}
void setup(){for(uint8_t i=0;i<2;i++){pinMode(pins[i],INPUT_PULLUP);raw[i]=stable[i]=digitalRead(pins[i])==LOW;changed[i]=millis();}}
void loop(){unsigned long now=millis();bool updated=false;
 for(uint8_t i=0;i<2;i++){bool b=digitalRead(pins[i])==LOW;if(b!=raw[i]){raw[i]=b;changed[i]=now;}if(raw[i]!=stable[i]&&now-changed[i]>=25){stable[i]=raw[i];updated=true;}}
 midiEventPacket_t p;do{p=MidiUSB.read();if(p.header&&p.byte1==0xBF&&p.byte2==102&&p.byte3==42){lastHost=now;connected=true;report();}}while(p.header);
 if(connected&&now-lastHost>2000)connected=false;
 if(connected&&(updated||now-lastReport>=250))report();
}
