package com.handytranslator.google;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

// A personal prototype credential, never embedded in the APK or included in backups.
final class KeyStore {
 private static final String ALIAS="google-translation-key";
 private static SecretKey encryptionKey() throws Exception {
  java.security.KeyStore ks=java.security.KeyStore.getInstance("AndroidKeyStore");ks.load(null);
  if(ks.containsAlias(ALIAS))return (SecretKey)ks.getKey(ALIAS,null);
  KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
  gen.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
   .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
  return gen.generateKey();
 }
 static boolean exists(Context c){return c.getSharedPreferences("credential",0).contains("cipher");}
 static void save(Context c,String value) throws Exception {
  if(!value.matches("[A-Za-z0-9_-]{20,200}"))throw new IllegalArgumentException();
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,encryptionKey());
  byte[] encrypted=cipher.doFinal(value.getBytes("UTF-8"));
  if(!c.getSharedPreferences("credential",0).edit().putString("iv",Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP))
   .putString("cipher",Base64.encodeToString(encrypted,Base64.NO_WRAP)).commit())throw new java.io.IOException();
 }
 static String read(Context c) throws Exception {
  android.content.SharedPreferences p=c.getSharedPreferences("credential",0);
  if(!exists(c))throw new IllegalStateException();
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,encryptionKey(),new GCMParameterSpec(128,Base64.decode(p.getString("iv",""),Base64.NO_WRAP)));
  return new String(cipher.doFinal(Base64.decode(p.getString("cipher",""),Base64.NO_WRAP)),"UTF-8");
 }
 static void clear(Context c){c.getSharedPreferences("credential",0).edit().clear().commit();}
}
