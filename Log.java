import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Log {
	public static final void writeLog(String string) {
		long time=System.currentTimeMillis();
		SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss", Locale.getDefault());
		String date=simpleDateFormat.format(new Date(time));
		string="["+date+"]"+string;
		try {
			FileOutputStream fileOutputStream=new FileOutputStream("log.txt", true);
			fileOutputStream.write((string+"\r\n").getBytes());
			fileOutputStream.close();
			System.out.println(string);
		}
		catch (Exception e) {}
	}
	static void error(PrintWriter printWriter, String errornum, String shortmsg, String longmsg,
			String ip) {
		String string=String.format("<html><title>ERROR %s</title><body><center><h1>ERROR %s: %s</h1></center><br>"
				+ "<center><b>%s</b></center></body></html>", 
				errornum, errornum, shortmsg, longmsg);
		int len=string.length();
		
		String output=String.format("HTTP/1.1 %s %s\r\nContent-type: text/html\r\nContent-length: %d\r\n\r\n%s\r\n", 
				errornum, shortmsg, len, string);
		System.out.println(output);
		printWriter.print(output);
		printWriter.flush();
		
		writeLog("Response to "+ip+": HTTP/1.1 "+errornum+" "+shortmsg);
	}
}
