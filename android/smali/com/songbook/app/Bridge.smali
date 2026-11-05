.class public Lcom/songbook/app/Bridge;
.super Ljava/lang/Object;

.field private a:Lcom/songbook/app/MainActivity;

.method public constructor <init>(Lcom/songbook/app/MainActivity;)V
    .registers 2
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    iput-object p1, p0, Lcom/songbook/app/Bridge;->a:Lcom/songbook/app/MainActivity;
    return-void
.end method

.method public copy(Ljava/lang/String;)V
    .registers 5
    .annotation runtime Landroid/webkit/JavascriptInterface;
    .end annotation
    iget-object v0, p0, Lcom/songbook/app/Bridge;->a:Lcom/songbook/app/MainActivity;
    const-string v1, "clipboard"
    invoke-virtual {v0, v1}, Landroid/app/Activity;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;
    move-result-object v1
    check-cast v1, Landroid/content/ClipboardManager;
    const-string v2, "Song number"
    invoke-static {v2, p1}, Landroid/content/ClipData;->newPlainText(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Landroid/content/ClipData;
    move-result-object v2
    invoke-virtual {v1, v2}, Landroid/content/ClipboardManager;->setPrimaryClip(Landroid/content/ClipData;)V
    return-void
.end method

.method public exit()V
    .registers 3
    .annotation runtime Landroid/webkit/JavascriptInterface;
    .end annotation
    iget-object v0, p0, Lcom/songbook/app/Bridge;->a:Lcom/songbook/app/MainActivity;
    const/4 v1, 0x1
    invoke-virtual {v0, v1}, Landroid/app/Activity;->moveTaskToBack(Z)Z
    return-void
.end method
