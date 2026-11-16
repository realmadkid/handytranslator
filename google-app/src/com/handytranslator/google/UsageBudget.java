package com.handytranslator.google;
import java.time.*;

// Local safety margin, NOT an account-wide billing meter. Persist before sending;
// uncertain/failed requests are deliberately not refunded or automatically retried.
final class UsageBudget {
 static final int DAILY=14000,MONTHLY=450000,PER_REQUEST=3000;
 static final ZoneId ZONE=ZoneId.of("America/Los_Angeles");
 static String day(long millis){return Instant.ofEpochMilli(millis).atZone(ZONE).toLocalDate().toString();}
 static String month(long millis){return day(millis).substring(0,7);}
 static int characters(String text){return text.codePointCount(0,text.length());}
 static boolean allowed(int count,int daily,int monthly){return count>0&&count<=PER_REQUEST&&daily>=0&&monthly>=0&&daily<=DAILY-count&&monthly<=MONTHLY-count;}
}
