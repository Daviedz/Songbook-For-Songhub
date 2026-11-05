package android.webkit;
public class WebView { public boolean post(Runnable r){r.run();return true;} public void evaluateJavascript(String s, ValueCallback<String> c){ System.out.println("JS> "+(s.length()>160?s.substring(0,160)+"…":s)); } }
