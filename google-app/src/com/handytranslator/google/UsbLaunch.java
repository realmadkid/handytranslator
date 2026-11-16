package com.handytranslator.google;
import android.content.*;
import android.hardware.usb.*;
final class UsbLaunch {
 static boolean connected(Context context,String name){
  UsbManager manager=context.getSystemService(UsbManager.class);if(manager==null||name==null)return false;
  UsbDevice device=manager.getDeviceList().get(name);
  if(device==null||!BoardIdentity.matches(device.getVendorId(),device.getProductId()))return false;
  for(int i=0;i<device.getInterfaceCount();i++){UsbInterface port=device.getInterface(i);if(port.getInterfaceClass()==1&&port.getInterfaceSubclass()==3)return true;}
  return false;
 }
 static String attached(Context context,Intent intent){
  if(intent==null||!UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction()))return null;
  try{UsbDevice device=intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);return device!=null&&connected(context,device.getDeviceName())?device.getDeviceName():null;}
  catch(RuntimeException invalid){return null;}
 }
}
