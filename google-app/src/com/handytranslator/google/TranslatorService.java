package com.handytranslator.google;

import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;
import android.speech.*;
import android.speech.tts.*;
import java.util.*;
import java.util.concurrent.*;

/** No Activity, overlay or accessibility dependency once the user starts a session. */
public class TranslatorService extends Service {
 static TranslatorService current;
 static String status="세션을 시작하세요",lastTranslation="";
 private final Handler h=new Handler(Looper.getMainLooper());
 private final ExecutorService network=Executors.newSingleThreadExecutor();
 private MidiBridge midi;private ButtonGate gate;private SpeechRecognizer speech;private TextToSpeech tts;
 private PowerManager.WakeLock cpu;private AudioManager audio;private AudioFocusRequest focus;
 private boolean armed,held,released,ready,busy,ttsReady,destroyed;
 private int direction,epoch;private Future<?> request;
 private final StringBuilder transcript=new StringBuilder();
 private final Runnable maxHold=()->{if(held)release();};
 private final Runnable finalTimeout=()->{if(released)fail("음성인식 응답 시간초과");};
 private final Runnable prepareTimeout=()->{if(speech!=null&&!ready)fail("음성인식 준비 시간초과 · 인식 서비스 설정 확인");};
 private final Runnable playbackTimeout=()->{if(busy)fail("음성 출력 시간초과");};
 private final Runnable sessionTimeout=()->{note("2시간 세션 종료 · 앱에서 다시 시작하세요");stopSelf();};
 private final Runnable delayedTest=()->speak("画面を消したまま、翻訳を読み上げます。",0,epoch);
 public IBinder onBind(Intent i){return null;}
 public void onCreate(){super.onCreate();current=this;audio=getSystemService(AudioManager.class);}
 public int onStartCommand(Intent intent,int flags,int id){
  if(intent!=null&&"STOP".equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
  if(armed)return START_NOT_STICKY;
  try {
   NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("session","버튼 번역 세션",NotificationManager.IMPORTANCE_LOW));
   startForeground(1,notification("준비 중"));
   cpu=getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"HandyTranslator:GoogleSession");cpu.acquire(7260000);
   armed=true;gate=new ButtonGate(new ButtonGate.Sink(){public void down(int d){press(d);}public void up(){release();}});
   midi=new MidiBridge(this,h,new MidiBridge.Listener(){public void buttons(int mask){gate.accept(mask);}public void disconnected(){cancel();gate.reset();note("USB 연결 대기 · 두 버튼을 놓고 연결하세요");}});
   tts=new TextToSpeech(this,result->{if(destroyed)return;ttsReady=result==TextToSpeech.SUCCESS;if(ttsReady){tts.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());note("세션 시작 · 전원 버튼으로 화면을 꺼도 됩니다");}else note("음성 읽기 초기화 실패 · TTS 설정 확인");});
   tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){public void onStart(String id){}public void onDone(String id){h.post(()->finishPlayback(id,false));}public void onError(String id){h.post(()->finishPlayback(id,true));}});
   h.postDelayed(sessionTimeout,7200000);note("음성 출력 준비 중");
  }catch(RuntimeException e){note("세션 시작 실패 · 앱 화면에서 마이크 권한을 확인하세요");stopSelf();}
  return START_NOT_STICKY;
 }
 private Notification notification(String text){
  PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,TranslatorService.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  return new Notification.Builder(this,"session").setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("HandyTranslator Google").setContentText(text).setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"세션 종료",stop).build()).build();
 }
 private void note(String text){status=text;if(armed&&!destroyed)getSystemService(NotificationManager.class).notify(1,notification(text));android.util.Log.i("HandyGoogle",text);}
 private Voice voice(int d){if(!ttsReady||tts==null||tts.getVoices()==null)return null;String language=d==0?"ja":"ko";for(Voice v:tts.getVoices()){if(v.getLocale().getLanguage().equals(language)&&!v.isNetworkConnectionRequired()&&(v.getFeatures()==null||!v.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)))return v;}return null;}
 void press(int d){
  if(!armed||held||released||busy)return;
  if(!KeyStore.exists(this)){note("앱에 Google API 키를 저장하세요");return;}
  if(voice(d)==null){note("한국어·일본어 오프라인 TTS 음성을 설치하세요");return;}
  if(!SpeechRecognizer.isRecognitionAvailable(this)){note("폰의 음성인식 서비스를 설치·설정하세요");return;}
  direction=d;transcript.setLength(0);held=true;released=false;epoch++;startRecognizer(epoch);h.postDelayed(maxHold,45000);
 }
 private void startRecognizer(final int token){
  if(!armed||!held||token!=epoch)return;
  ready=false;
  try{
   speech=SpeechRecognizer.createSpeechRecognizer(this);final SpeechRecognizer recognizer=speech;
   speech.setRecognitionListener(new RecognitionListener(){
    private boolean valid(){return token==epoch&&speech==recognizer&&!destroyed;}
    public void onReadyForSpeech(Bundle b){if(!valid())return;ready=true;h.removeCallbacks(prepareTimeout);if(released){speech.stopListening();return;}note(direction==0?"한국어 입력 중 · 놓으면 일본어":"日本語 입력 중 · 놓으면 한국어");Vibrator v=getSystemService(Vibrator.class);if(v!=null)v.vibrate(VibrationEffect.createOneShot(35,80));}
    public void onBeginningOfSpeech(){}public void onRmsChanged(float f){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){}
    public void onError(int error){if(!valid())return;finishRecognizer();boolean empty=error==SpeechRecognizer.ERROR_NO_MATCH||error==SpeechRecognizer.ERROR_SPEECH_TIMEOUT;if(empty&&held)h.postDelayed(()->startRecognizer(token),150);else if(empty&&released)submit();else fail("음성인식 오류 "+error+" · 다시 누르세요");}
    public void onResults(Bundle b){if(!valid())return;ArrayList<String> list=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(list!=null&&!list.isEmpty()&&!list.get(0).trim().isEmpty()){if(transcript.length()>0)transcript.append(' ');transcript.append(list.get(0).trim());}finishRecognizer();if(held)h.postDelayed(()->startRecognizer(token),150);else submit();}
    public void onPartialResults(Bundle b){}public void onEvent(int t,Bundle b){}
   });
   Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE,direction==0?"ko-KR":"ja-JP").putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
   speech.startListening(i);h.postDelayed(prepareTimeout,8000);note("음성인식 준비 중 · 진동 후 말하세요");
  }catch(RuntimeException e){fail("음성인식 시작 실패 · 폰의 인식 서비스 확인");}
 }
 void release(){if(!held)return;held=false;released=true;h.removeCallbacks(maxHold);note("인식 결과 대기");if(speech!=null){try{if(ready)speech.stopListening();h.postDelayed(finalTimeout,7000);}catch(RuntimeException e){fail("음성인식 종료 실패");}}else submit();}
 private void finishRecognizer(){h.removeCallbacks(prepareTimeout);if(speech!=null){speech.destroy();speech=null;}ready=false;}
 private void submit(){h.removeCallbacks(finalTimeout);released=false;if(transcript.length()==0){note("인식된 말이 없습니다 · 다시 누르세요");return;}translate(transcript.toString(),direction);transcript.setLength(0);}
 void sample(int d){if(!armed||held||released||busy)return;if(voice(d)==null){note("한국어·일본어 오프라인 TTS 음성을 설치하세요");return;}epoch++;translate(d==0?"오늘 날씨가 좋네요.":"今日はいい天気ですね。",d);}
 private void translate(String text,int d){
  if(!KeyStore.exists(this)){note("앱에 Google API 키를 저장하세요");return;}
  busy=true;final int token=epoch;note("Google 번역 중");
  request=network.submit(()->{try{String result=GoogleTranslation.translate(this,text,d);h.post(()->{if(!armed||destroyed||token!=epoch)return;lastTranslation=result;speak(result,d,token);});}catch(Exception e){String message=e instanceof java.io.IOException?e.getMessage():"번역 처리 오류";h.post(()->{if(armed&&!destroyed&&token==epoch)fail(message==null?"네트워크 오류":message);});}});
 }
 void screenOffTest(){if(!armed||held||released||busy)return;epoch++;busy=true;note("10초 뒤 일본어 읽기 · 지금 전원 버튼으로 화면을 끄세요");h.postDelayed(delayedTest,10000);}
 private void speak(String text,int d,int token){
  if(!armed||destroyed||token!=epoch)return;Voice v=voice(d);if(v==null){fail("한국어·일본어 오프라인 TTS 음성을 설치하세요");return;}
  focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()).setOnAudioFocusChangeListener(change->{if(change==AudioManager.AUDIOFOCUS_LOSS||change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)fail("다른 오디오 사용으로 읽기를 중단했습니다");},h).build();
  if(audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){fail("음성 출력 오디오 사용 불가");return;}
  if(tts.setVoice(v)!=TextToSpeech.SUCCESS){fail("음성 선택 실패");return;}
  busy=true;note("번역문 읽는 중");if(tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,Integer.toString(token))!=TextToSpeech.SUCCESS){fail("음성 출력 실패");return;}h.postDelayed(playbackTimeout,90000);
 }
 private void finishPlayback(String id,boolean error){if(!armed||destroyed||!id.equals(Integer.toString(epoch)))return;h.removeCallbacks(playbackTimeout);busy=false;abandonFocus();note(error?"음성 출력 실패 · TTS 언어 데이터 확인":"완료 · 다음 버튼을 누르세요");}
 private void abandonFocus(){if(focus!=null){audio.abandonAudioFocusRequest(focus);focus=null;}}
 private void cancel(){epoch++;held=released=busy=false;h.removeCallbacks(maxHold);h.removeCallbacks(finalTimeout);h.removeCallbacks(playbackTimeout);h.removeCallbacks(delayedTest);if(speech!=null)speech.cancel();finishRecognizer();transcript.setLength(0);if(request!=null){request.cancel(true);request=null;}if(tts!=null)tts.stop();abandonFocus();}
 private void fail(String message){cancel();note(message);}
 public void onDestroy(){destroyed=true;armed=false;cancel();h.removeCallbacksAndMessages(null);if(midi!=null)midi.close();if(tts!=null)tts.shutdown();network.shutdownNow();if(cpu!=null&&cpu.isHeld())cpu.release();stopForeground(STOP_FOREGROUND_REMOVE);current=null;status="세션 종료";super.onDestroy();}
}
