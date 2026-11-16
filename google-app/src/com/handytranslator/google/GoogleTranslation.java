package com.handytranslator.google;
import android.content.Context;
import android.content.pm.PackageManager;
import java.net.*;
import java.io.*;
import java.security.MessageDigest;
import javax.net.ssl.HttpsURLConnection;
import org.json.*;

final class GoogleTranslation {
 static String certificate(Context c) throws Exception {
  byte[] cert=c.getPackageManager().getPackageInfo(c.getPackageName(),PackageManager.GET_SIGNING_CERTIFICATES).signingInfo.getApkContentsSigners()[0].toByteArray();
  StringBuilder out=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-1").digest(cert))out.append(String.format(java.util.Locale.ROOT,"%02X",b&255));return out.toString();
 }
 static String translate(Context c,String text,int direction) throws Exception {
  final String key;
  try{key=KeyStore.read(c);}catch(Exception e){throw new IOException("API 키를 다시 저장하세요");}
  JSONObject body=new JSONObject().put("q",text).put("source",direction==0?"ko":"ja").put("target",direction==0?"ja":"ko").put("format","text").put("model","nmt");
  byte[] bytes=body.toString().getBytes("UTF-8");
  HttpsURLConnection connection=(HttpsURLConnection)new URL("https://translation.googleapis.com/language/translate/v2").openConnection();
  try {
   connection.setRequestMethod("POST");connection.setConnectTimeout(10000);connection.setReadTimeout(15000);connection.setInstanceFollowRedirects(false);
   connection.setRequestProperty("Content-Type","application/json; charset=utf-8");connection.setRequestProperty("X-Goog-Api-Key",key);
   connection.setRequestProperty("X-Android-Package",c.getPackageName());connection.setRequestProperty("X-Android-Cert",certificate(c));
   connection.setDoOutput(true);connection.setFixedLengthStreamingMode(bytes.length);
   if(Thread.currentThread().isInterrupted())throw new IOException("취소됨");
   if(!UsageMeter.reserve(c,text))throw new IOException("앱 사용량 한도 또는 기기 날짜를 확인하세요");
   try(OutputStream out=connection.getOutputStream()){out.write(bytes);}
   int code=connection.getResponseCode();
   if(code!=200)throw new IOException(code==403?"Google 403 · API 활성화·결제·키 제한·할당량 확인":code==429?"Google 429 · 할당량 초과, 잠시 후 다시 시도":"Google 응답 오류 "+code);
   ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=connection.getInputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>65536)throw new IOException("번역 응답 크기 초과");out.write(buffer,0,n);}}
   String result=new JSONObject(new String(out.toByteArray(),"UTF-8")).getJSONObject("data").getJSONArray("translations").getJSONObject(0).getString("translatedText").trim();
   if(result.isEmpty())throw new IOException("빈 번역 결과");return result;
  }catch(java.net.SocketTimeoutException e){throw new IOException("번역 시간초과 · 자동 재요청하지 않습니다");}
  finally{connection.disconnect();}
 }
}
