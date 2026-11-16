package com.handytranslator.google;
import android.content.*;

final class UsageMeter {
 private static int used(SharedPreferences p,String key,String period){return p.getString(key+"Period","").equals(period)?p.getInt(key,0):0;}
 static synchronized boolean reserve(Context c,String text){
  SharedPreferences p=c.getSharedPreferences("usage",0);long now=System.currentTimeMillis();
  // Rolling the device clock backwards must not silently reset the local allowance.
  if(now<p.getLong("last",0))return false;
  String day=UsageBudget.day(now),month=UsageBudget.month(now);int d=used(p,"day",day),m=used(p,"month",month),n=UsageBudget.characters(text);
  if(!UsageBudget.allowed(n,d,m))return false;
  return p.edit().putString("dayPeriod",day).putString("monthPeriod",month).putInt("day",d+n).putInt("month",m+n).putLong("last",now).commit();
 }
 static synchronized String summary(Context c){SharedPreferences p=c.getSharedPreferences("usage",0);long now=System.currentTimeMillis();return "이 폰 요청 문자: 오늘 "+used(p,"day",UsageBudget.day(now))+" / 14,000 · 이번 달 "+used(p,"month",UsageBudget.month(now))+" / 450,000";}
}
