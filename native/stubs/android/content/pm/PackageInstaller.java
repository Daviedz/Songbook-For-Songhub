package android.content.pm;
import java.io.OutputStream;
public class PackageInstaller {
  public int createSession(SessionParams p) throws java.io.IOException {throw new RuntimeException();}
  public Session openSession(int id) throws java.io.IOException {throw new RuntimeException();}
  public static class SessionParams { public static final int MODE_FULL_INSTALL = 1; public SessionParams(int mode){throw new RuntimeException();} }
  public static class Session implements java.io.Closeable {
    public OutputStream openWrite(String name, long off, long len) throws java.io.IOException {throw new RuntimeException();}
    public void fsync(OutputStream o) throws java.io.IOException {throw new RuntimeException();}
    public void commit(android.content.IntentSender s){throw new RuntimeException();}
    public void close(){throw new RuntimeException();} }
}
