.class public Lcom/songbook/app/MainActivity;
.super Landroid/app/Activity;

.field public web:Landroid/webkit/WebView;

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Landroid/app/Activity;-><init>()V
    return-void
.end method

.method protected onCreate(Landroid/os/Bundle;)V
    .registers 7
    invoke-super {p0, p1}, Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V
    new-instance v0, Landroid/webkit/WebView;
    invoke-direct {v0, p0}, Landroid/webkit/WebView;-><init>(Landroid/content/Context;)V
    iput-object v0, p0, Lcom/songbook/app/MainActivity;->web:Landroid/webkit/WebView;
    invoke-virtual {v0}, Landroid/webkit/WebView;->getSettings()Landroid/webkit/WebSettings;
    move-result-object v1
    const/4 v2, 0x1
    invoke-virtual {v1, v2}, Landroid/webkit/WebSettings;->setJavaScriptEnabled(Z)V
    invoke-virtual {v1, v2}, Landroid/webkit/WebSettings;->setDomStorageEnabled(Z)V
    new-instance v3, Lcom/songbook/app/Bridge;
    invoke-direct {v3, p0}, Lcom/songbook/app/Bridge;-><init>(Lcom/songbook/app/MainActivity;)V
    const-string v4, "Android"
    invoke-virtual {v0, v3, v4}, Landroid/webkit/WebView;->addJavascriptInterface(Ljava/lang/Object;Ljava/lang/String;)V
    new-instance v3, Lcom/songbook/app/NativeWs;
    invoke-direct {v3, p0, v0}, Lcom/songbook/app/NativeWs;-><init>(Landroid/app/Activity;Landroid/webkit/WebView;)V
    const-string v4, "AndroidWs"
    invoke-virtual {v0, v3, v4}, Landroid/webkit/WebView;->addJavascriptInterface(Ljava/lang/Object;Ljava/lang/String;)V
    invoke-static {p0}, Lcom/songbook/app/NativeWs;->askPermission(Landroid/app/Activity;)V
    new-instance v3, Lcom/songbook/app/BtKeypad;
    invoke-direct {v3, p0, v0}, Lcom/songbook/app/BtKeypad;-><init>(Landroid/app/Activity;Landroid/webkit/WebView;)V
    const-string v4, "AndroidBt"
    invoke-virtual {v0, v3, v4}, Landroid/webkit/WebView;->addJavascriptInterface(Ljava/lang/Object;Ljava/lang/String;)V
    new-instance v3, Lcom/songbook/app/Updater;
    invoke-direct {v3, p0, v0}, Lcom/songbook/app/Updater;-><init>(Landroid/app/Activity;Landroid/webkit/WebView;)V
    const-string v4, "AndroidUp"
    invoke-virtual {v0, v3, v4}, Landroid/webkit/WebView;->addJavascriptInterface(Ljava/lang/Object;Ljava/lang/String;)V
    invoke-virtual {p0, v0}, Landroid/app/Activity;->setContentView(Landroid/view/View;)V
    const-string v4, "file:///android_asset/index.html"
    invoke-virtual {v0, v4}, Landroid/webkit/WebView;->loadUrl(Ljava/lang/String;)V
    return-void
.end method

.method public onBackPressed()V
    .registers 4
    iget-object v0, p0, Lcom/songbook/app/MainActivity;->web:Landroid/webkit/WebView;
    const-string v1, "window.__back?window.__back():Android.exit()"
    const/4 v2, 0x0
    invoke-virtual {v0, v1, v2}, Landroid/webkit/WebView;->evaluateJavascript(Ljava/lang/String;Landroid/webkit/ValueCallback;)V
    return-void
.end method
