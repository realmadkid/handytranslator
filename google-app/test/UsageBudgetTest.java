package com.handytranslator.google;
import java.time.Instant;
public class UsageBudgetTest {
 private static void check(boolean ok){if(!ok)throw new AssertionError();}
 public static void main(String[] args){
  check(UsageBudget.characters("가あ😀")==3);
  check(UsageBudget.allowed(1,13999,449999));
  check(!UsageBudget.allowed(2,13999,0));check(!UsageBudget.allowed(2,0,449999));
  check(!UsageBudget.allowed(0,0,0));check(!UsageBudget.allowed(3001,0,0));
  check(!UsageBudget.allowed(1,Integer.MAX_VALUE,0));check(!UsageBudget.allowed(1,-1,0));
  long before=Instant.parse("2026-12-01T07:59:59Z").toEpochMilli();
  check(UsageBudget.day(before).equals("2026-11-30"));check(UsageBudget.month(before).equals("2026-11"));
  check(UsageBudget.day(before+1000).equals("2026-12-01"));check(UsageBudget.month(before+1000).equals("2026-12"));
  check(UsageBudget.day(Instant.parse("2026-07-01T07:00:00Z").toEpochMilli()).equals("2026-07-01"));
  System.out.println("UsageBudgetTest PASS: Unicode code points, exact limits, overflow, Pacific month/day boundaries and DST");
 }
}
