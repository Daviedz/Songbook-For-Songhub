package android.app;
public class Activity extends android.content.Context { public int checkSelfPermission(String p){return Integer.getInteger("perm",0);} public void requestPermissions(String[] p,int c){System.out.println("requestPermissions "+String.join(",",p));} public void runOnUiThread(Runnable r){r.run();} }
