package com.handytranslator.google;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
 private final Handler h=new Handler();
 private TextView status,usage,keyStatus,result,sessionLabel;private EditText key;
 private Button sessionButton;private boolean touchHeld,askedNotifications,settingsPage;
 private String pendingUsb;private boolean resumed;
 private static final int BG=0xfff4f7f6,INK=0xff172d2a,MUTED=0xff60736f,ACCENT=0xff126b58;
 private final Runnable refresh=new Runnable(){public void run(){tryUsbStart();refreshUi();h.postDelayed(this,500);}};
 public void onCreate(Bundle saved){super.onCreate(saved);getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
  getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  pendingUsb=saved==null?UsbLaunch.attached(this,getIntent()):saved.getString("pendingUsb");
  showPage(saved!=null&&saved.getBoolean("settingsPage",false));
 }
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private android.graphics.drawable.GradientDrawable surface(int color,int radius){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
 private LinearLayout column(){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);return box;}
 private void gap(LinearLayout box,int height){View space=new View(this);box.addView(space,new LinearLayout.LayoutParams(1,dp(height)));}
 private void showPage(boolean settings){
  releaseTouch();settingsPage=settings;status=usage=keyStatus=result=sessionLabel=null;key=null;sessionButton=null;
  android.widget.FrameLayout frame=new android.widget.FrameLayout(this);frame.setBackgroundColor(BG);
  ScrollView scroll=new ScrollView(this){protected void onMeasure(int w,int height){int available=View.MeasureSpec.getSize(w);super.onMeasure(View.MeasureSpec.makeMeasureSpec(Math.min(available,dp(600)),View.MeasureSpec.EXACTLY),height);}};
  scroll.setFillViewport(true);scroll.setClipToPadding(false);
  frame.addView(scroll,new android.widget.FrameLayout.LayoutParams(-1,-1,Gravity.TOP|Gravity.CENTER_HORIZONTAL));
  LinearLayout box=column();box.setPadding(dp(20),dp(16),dp(20),dp(24));scroll.addView(box,new ScrollView.LayoutParams(-1,-2));setContentView(frame);
  frame.setOnApplyWindowInsetsListener((v,insets)->{int left=insets.getSystemWindowInsetLeft(),right=insets.getSystemWindowInsetRight(),top=insets.getSystemWindowInsetTop(),bottom=insets.getSystemWindowInsetBottom();android.view.DisplayCutout cutout=insets.getDisplayCutout();if(cutout!=null){left=Math.max(left,cutout.getSafeInsetLeft());right=Math.max(right,cutout.getSafeInsetRight());top=Math.max(top,cutout.getSafeInsetTop());bottom=Math.max(bottom,cutout.getSafeInsetBottom());}v.setPadding(left,top,right,bottom);return insets;});
  if(settings)settings(box);else home(box);refreshUi();
 }
 private void home(LinearLayout box){
  text(box,"HandyTranslator",26);text(box,"한국어  ↔  日本語",16);gap(box,16);
  LinearLayout card=column();card.setPadding(dp(18),dp(12),dp(18),dp(16));card.setBackground(surface(0xffffffff,20));box.addView(card,new LinearLayout.LayoutParams(-1,-2));
  sessionLabel=text(card,"",20);sessionLabel.setTextColor(ACCENT);status=text(card,"",15);
  sessionButton=add(card,"시작",()->{if(TranslatorService.current==null)startSession();else stopService(new Intent(this,TranslatorService.class));});
  gap(box,20);text(box,"누르고 말하기",16);
  for(int d=0;d<2;d++){final int direction=d;Button button=add(box,d==0?"한국어 → 日本語":"日本語 → 한국어",()->{});button.setMinHeight(dp(72));button.setTextSize(20);
   button.setOnTouchListener((v,e)->{if(e.getActionMasked()==MotionEvent.ACTION_DOWN){if(!touchHeld){TranslatorService service=service();if(service!=null){touchHeld=true;v.getParent().requestDisallowInterceptTouchEvent(true);button.setAlpha(0.65f);service.press(direction);}}return true;}if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL){button.setAlpha(1f);v.getParent().requestDisallowInterceptTouchEvent(false);releaseTouch();return true;}return true;});
  }
  result=text(box,"",20);result.setTextIsSelectable(true);gap(box,12);
  add(box,"설정",()->showPage(true));
 }
 private void settings(LinearLayout box){
  add(box,"← 돌아가기",()->showPage(false));text(box,"설정",26);gap(box,8);
  Switch auto=new Switch(this);auto.setText("보드 연결 시 자동 시작");auto.setTextSize(16);auto.setTextColor(INK);auto.setMinHeight(dp(56));auto.setPadding(dp(4),dp(8),dp(4),dp(8));auto.setChecked(getPreferences(0).getBoolean("usbAuto",true));box.addView(auto,new LinearLayout.LayoutParams(-1,-2));
  auto.setOnCheckedChangeListener((button,checked)->{getPreferences(0).edit().putBoolean("usbAuto",checked).apply();if(!checked)pendingUsb=null;});
  gap(box,12);text(box,"Google API",20);keyStatus=text(box,"",14);updateKeyStatus();
  key=new EditText(this);key.setHint("새 API 키");key.setTextSize(16);key.setTextColor(INK);key.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);key.setSingleLine(true);key.setSaveEnabled(false);key.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);key.setMinHeight(dp(52));box.addView(key,new LinearLayout.LayoutParams(-1,-2));
  add(box,"키 저장",()->{if(TranslatorService.current!=null){toast("세션 종료 후 변경하세요");return;}try{KeyStore.save(this,key.getText().toString().trim());key.setText("");updateKeyStatus();toast("저장했습니다");}catch(Exception e){toast("키 형식 또는 저장 상태를 확인하세요");}});
  add(box,"키 삭제",()->new AlertDialog.Builder(this).setMessage("저장한 API 키를 삭제할까요?").setNegativeButton("취소",null).setPositiveButton("삭제",(dialog,which)->{stopService(new Intent(this,TranslatorService.class));KeyStore.clear(this);key.setText("");updateKeyStatus();}).show());
  gap(box,12);usage=text(box,"",14);
  add(box,"테스트",()->new AlertDialog.Builder(this).setTitle("테스트").setItems(new String[]{"10초 뒤 음성 출력 · API 미사용","한 → 일 문장 · API 사용","일 → 한 문장 · API 사용"},(dialog,which)->{TranslatorService service=service();if(service==null)return;if(which==0)service.screenOffTest();else service.sample(which-1);showPage(false);}).setNegativeButton("닫기",null).show());
  add(box,"음성 설정",()->open(new Intent("com.android.settings.TTS_SETTINGS")));
  add(box,"배터리 설정",()->open(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)));
  add(box,"사용 안내",()->new AlertDialog.Builder(this).setTitle("사용 안내 · v0.3").setMessage("보드 연결 시 이 앱을 기본으로 선택하세요. 처음 시작할 때는 잠금 해제가 필요할 수 있습니다. 시작 후에는 화면을 꺼도 됩니다.\n\n준비 진동 후 말하고 버튼을 놓으세요. 한국어·일본어 오프라인 읽기 음성이 필요합니다.\n\n음성은 폰의 인식 제공자에게, 인식된 텍스트는 Google Cloud에 전송됩니다. 앱은 대화를 파일로 저장하지 않습니다.\n\n앱 한도: 일 14,000 / 월 450,000 문자. Cloud 무료 범위는 다른 사용량과 합산되므로 콘솔 할당량도 설정하세요.\n\n최대 입력 45초, 세션 2시간. 침묵으로 인식기가 재시작할 때 짧은 누락이 생길 수 있습니다.").setPositiveButton("확인",null).show());
  text(box,"Google · v0.3",13);
 }
 private void setText(TextView view,String value){if(view!=null&&!value.contentEquals(view.getText()))view.setText(value);}
 private void refreshUi(){
  boolean active=TranslatorService.current!=null;
  setText(sessionLabel,active?"번역 준비됨":"대기 중");setText(sessionButton,active?"종료":"시작");
  String detail=TranslatorService.status;
  if(!active)detail=KeyStore.exists(this)?"시작하거나 보드를 연결하세요":"설정에서 API 키를 등록하세요";
  else if(detail.startsWith("세션 시작"))detail="준비 진동 후 말하세요";
  setText(status,detail);setText(usage,UsageMeter.summary(this));
  if(result!=null){setText(result,TranslatorService.lastTranslation);result.setVisibility(TranslatorService.lastTranslation.isEmpty()?View.GONE:View.VISIBLE);}
 }
 public void onBackPressed(){if(settingsPage)showPage(false);else super.onBackPressed();}
 protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);pendingUsb=UsbLaunch.attached(this,intent);tryUsbStart();}
 protected void onSaveInstanceState(Bundle state){state.putString("pendingUsb",pendingUsb);state.putBoolean("settingsPage",settingsPage);super.onSaveInstanceState(state);}
 public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(focused)tryUsbStart();}
 private void tryUsbStart(){
  if(pendingUsb==null||!resumed||!hasWindowFocus())return;
  if(!getPreferences(0).getBoolean("usbAuto",true)||!UsbLaunch.connected(this,pendingUsb)){pendingUsb=null;return;}
  if(!getSystemService(PowerManager.class).isInteractive()||getSystemService(KeyguardManager.class).isKeyguardLocked())return;
  pendingUsb=null; // Consume once: resuming after Stop must never restart the session.
  if(TranslatorService.current!=null)return;
  if(!KeyStore.exists(this)){toast("자동 시작 대기 · API 키 저장 후 세션을 시작하세요");return;}
  if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){toast("자동 시작 대기 · 세션 시작을 눌러 마이크를 허용하세요");return;}
  try{startForegroundService(new Intent(this,TranslatorService.class));toast("보드 연결 · 번역 세션 자동 시작");}catch(RuntimeException e){toast("자동 시작 불가 · 세션 시작을 눌러 주세요");}
 }
 private void updateKeyStatus(){if(keyStatus!=null)keyStatus.setText(KeyStore.exists(this)?"등록됨":"미등록");}
 private void startSession(){if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},1);return;}if(!askedNotifications&&Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){askedNotifications=true;requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},2);return;}try{startForegroundService(new Intent(this,TranslatorService.class));}catch(RuntimeException e){toast("앱을 연 상태에서 다시 시작하세요");}}
 public void onRequestPermissionsResult(int code,String[] permissions,int[] results){super.onRequestPermissionsResult(code,permissions,results);if(code==1&&results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)toast("마이크 허용됨 · 세션 시작을 다시 누르세요");if(code==2)toast("세션 시작을 다시 누르세요. 알림을 허용하면 종료하기 편합니다.");}
 private TranslatorService service(){TranslatorService s=TranslatorService.current;if(s==null)toast("먼저 세션을 시작하세요");return s;}
 private TextView text(LinearLayout box,String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(size>=20?INK:MUTED);t.setPadding(0,dp(6),0,dp(6));box.addView(t,new LinearLayout.LayoutParams(-1,-2));return t;}
 private Button add(LinearLayout box,String label,Runnable task){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(0xffffffff);b.setMinHeight(dp(52));b.setPadding(dp(16),dp(12),dp(16),dp(12));b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ACCENT));b.setOnClickListener(v->task.run());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(8);box.addView(b,p);return b;}
 private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
 private void open(Intent intent){try{startActivity(intent);}catch(ActivityNotFoundException e){toast("폰 설정에서 음성 읽기 또는 배터리 설정을 여세요");}}
 private void releaseTouch(){if(touchHeld){touchHeld=false;if(TranslatorService.current!=null)TranslatorService.current.release();}}
 protected void onResume(){super.onResume();resumed=true;h.post(refresh);}
 protected void onPause(){resumed=false;releaseTouch();h.removeCallbacks(refresh);super.onPause();}
}
