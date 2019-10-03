package de.botcompany;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.net.*;
import java.lang.management.*;

public class LinuxProcessSizeDetector {
  public static void main(String[] args) throws IOException {
    rssFixedForZGC_verbose = true;
    if (args.length == 0)
      System.out.println("RSS of this process: " + rssFixedForZGC());
    else {
      int pid = Integer.parseInt(args[0]);
      System.out.println("RSS of process " + pid + ": " + rssFixedForZGC(pid));
    }
  }
  
  static boolean rssFixedForZGC_verbose = false;

  // get corrected RSS for this process
  public static long rssFixedForZGC() throws IOException {
    return rssFixedForZGC(processID_int());
  }

  // get corrected RSS for another process
  public static long rssFixedForZGC(int pid) throws IOException {
    //if (!is64BitLinux()) fail("Not on 64 Bit Linux");
    String range = null;
    TreeMap<String, Long> rssByRange = new TreeMap();
    Pattern patAddr = Pattern.compile("^([0-9a-f]{1,16})-");
    BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/" + pid + "/smaps")));
    try {
      String s;
      while ((s = in.readLine()) != null) {
        Matcher m = patAddr.matcher(s);
        String addr = m.find() ? m.group(1) : null;
        if (addr != null) {
          //print("Have address: " + addr);
          String cleanedAddr = longToHex(parseUnsignedHexLong(addr) & ~((1L << 42) - 1));
          range = takeFirst_string(6, cleanedAddr);
          continue;
        }
        // else print("Not range: " + s);
        String[] p = parseColonProperty_array(s);
        if (p != null && eqic(p[0], "rss")) {
          long size = Long.parseLong(beforeSpace(p[1]));
          if (size != 0)
            rssByRange.put(range, getOrCreate_long(rssByRange, range) + size);
        }
      }
      if (rssFixedForZGC_verbose)
        for (Map.Entry<String, Long> e : rssByRange.entrySet()) {
          System.out.println("Address range " + rpad(e.getKey(), 16, 'x') + ": " + toK(e.getValue()) + " MB");
        }
      Long min = null;
      for (String _range : ll("000004", "000008", "000010")) {
        Long l = rssByRange.get(_range);
        if (l != null)
          min = min == null ? l : Math.min(min, l);
      }
      long total = 0;
      for (long l : rssByRange.values()) total += l;
      long guess = min == null ? total : total - min * 3;
      return guess * 1024;
    } finally {
      _close(in);
    }
  }

  // current process ID
  static int processID_int() {
    return parseInt(getPID());
  }

  static String longToHex(long l) {
    return bytesToHex(longToBytes(l));
  }

  static long parseUnsignedHexLong(String s) {
    // from Java 8
    return Long.parseUnsignedLong(s, 16);
  }

  static String takeFirst_string(int n, String s) {
    return substring(s, 0, n);
  }

  static String takeFirst_string(String s, int n) {
    return substring(s, 0, n);
  }

  static String[] parseColonProperty_array(String s) {
    if (s == null)
      return null;
    int i = s.indexOf(':');
    if (i < 0)
      return null;
    return new String[] { trimSubstring(s, 0, i), trimSubstring(s, i + 1) };
  }

  static boolean eqic(String a, String b) {
    if ((a == null) != (b == null))
      return false;
    if (a == null)
      return true;
    return a.equalsIgnoreCase(b);
  }

  static boolean eqic(char a, char b) {
    if (a == b)
      return true;
    char u1 = Character.toUpperCase(a);
    char u2 = Character.toUpperCase(b);
    if (u1 == u2)
      return true;
    return Character.toLowerCase(u1) == Character.toLowerCase(u2);
  }

  static String beforeSpace(String s) {
    return onlyUntilSpace(s);
  }

  static <A> long getOrCreate_long(Map<A, Long> map, A key) {
    Long b = map.get(key);
    if (b == null)
      map.put(key, b = 0L);
    return b;
  }

  static String rpad(String s, int l) {
    return rpad(s, l, ' ');
  }

  static String rpad(String s, int l, char c) {
    return lengthOfString(s) >= l ? s : s + rep(c, l - lengthOfString(s));
  }

  static String rpad(int l, String s) {
    return rpad(s, l);
  }

  static long toK(long l) {
    return (l + 1023) / 1024;
  }

  static <A> List<A> ll(A... a) {
    ArrayList l = new ArrayList(a.length);
    if (a != null)
      for (A x : a) l.add(x);
    return l;
  }

  static void _close(AutoCloseable c) {
    if (c != null)
      try {
        c.close();
      } catch (Throwable e) {
        // Some classes stupidly throw an exception on double-closing
        if (c instanceof javax.imageio.stream.ImageOutputStream)
          return;
        else
          throw rethrow(e);
      }
  }

  static int parseInt(String s) {
    return emptyString(s) ? 0 : Integer.parseInt(s);
  }

  static int parseInt(char c) {
    return Integer.parseInt(str(c));
  }

  static String processID_cached;

  // try to get our current process ID
  static String getPID() {
    if (processID_cached == null) {
      String name = ManagementFactory.getRuntimeMXBean().getName();
      processID_cached = name.replaceAll("@.*", "");
    }
    return processID_cached;
  }

  public static String bytesToHex(byte[] bytes) {
    return bytesToHex(bytes, 0, bytes.length);
  }

  public static String bytesToHex(byte[] bytes, int ofs, int len) {
    StringBuilder stringBuilder = new StringBuilder(len * 2);
    for (int i = 0; i < len; i++) {
      String s = "0" + Integer.toHexString(bytes[ofs + i]);
      stringBuilder.append(s.substring(s.length() - 2, s.length()));
    }
    return stringBuilder.toString();
  }

  static byte[] longToBytes(long l) {
    return new byte[] { (byte) (l >>> 56), (byte) (l >>> 48), (byte) (l >>> 40), (byte) (l >>> 32), (byte) (l >>> 24), (byte) (l >>> 16), (byte) (l >>> 8), (byte) l };
  }

  static String substring(String s, int x) {
    return substring(s, x, strL(s));
  }

  static String substring(String s, int x, int y) {
    if (s == null)
      return null;
    if (x < 0)
      x = 0;
    if (x >= s.length())
      return "";
    if (y < x)
      y = x;
    if (y > s.length())
      y = s.length();
    return s.substring(x, y);
  }

  static String trimSubstring(String s, int x) {
    return trim(substring(s, x));
  }

  static String trimSubstring(String s, int x, int y) {
    return trim(substring(s, x, y));
  }

  static boolean eq(Object a, Object b) {
    return a == b || (a == null ? b == null : b != null && a.equals(b));
  }

  static String asString(Object o) {
    return o == null ? null : o.toString();
  }

  static String onlyUntilSpace(String s) {
    int i = s.indexOf(' ');
    return i >= 0 ? s.substring(0, i) : s;
  }

  static int lengthOfString(String s) {
    return s == null ? 0 : s.length();
  }

  static String rep(int n, char c) {
    return repeat(c, n);
  }

  static String rep(char c, int n) {
    return repeat(c, n);
  }

  static <A> List<A> rep(A a, int n) {
    return repeat(a, n);
  }

  static <A> List<A> rep(int n, A a) {
    return repeat(n, a);
  }

  static RuntimeException rethrow(Throwable t) {
    throw t instanceof RuntimeException ? (RuntimeException) t : new RuntimeException(t);
  }

  static RuntimeException rethrow(String msg, Throwable t) {
    throw new RuntimeException(msg, t);
  }

  static boolean emptyString(String s) {
    return s == null || s.length() == 0;
  }

  static String str(Object o) {
    return o == null ? "null" : o.toString();
  }

  static String str(char[] c) {
    return new String(c);
  }

  static int strL(String s) {
    return s == null ? 0 : s.length();
  }

  static String trim(String s) {
    return s == null ? null : s.trim();
  }

  static String trim(StringBuilder buf) {
    return buf.toString().trim();
  }

  static String trim(StringBuffer buf) {
    return buf.toString().trim();
  }

  static String repeat(char c, int n) {
    n = Math.max(n, 0);
    char[] chars = new char[n];
    for (int i = 0; i < n; i++) chars[i] = c;
    return new String(chars);
  }

  static <A> List<A> repeat(A a, int n) {
    n = Math.max(n, 0);
    List<A> l = new ArrayList(n);
    for (int i = 0; i < n; i++) l.add(a);
    return l;
  }

  static <A> List<A> repeat(int n, A a) {
    return repeat(a, n);
  }
}
