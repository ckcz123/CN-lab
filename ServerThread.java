import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;
import java.util.TimeZone;

public class ServerThread extends Thread implements Runnable {
	Socket socket;
	public ServerThread(Socket _socket) {
		socket=_socket;
	}
	public void run() {
		try {
			SimpleDateFormat simpleDateFormat=new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss 'GMT'", Locale.getDefault());
			simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
			
			InetAddress inetAddress=socket.getInetAddress();
			if (inetAddress==null) return;
			String ip=inetAddress.getHostAddress();
	
			BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter printWriter=new PrintWriter(socket.getOutputStream());
			String string=bufferedReader.readLine();
			String uri, method;
			
			if (string==null) return;
			if (string.startsWith("GET") || string.startsWith("POST")
					|| string.startsWith("HEAD")) {
					
				Scanner scanner=new Scanner(string);
				method=scanner.next();
				uri=scanner.next();
				scanner.close();

				String hint="New HTTP request from "+ip+" ("+string+")";
				Log.writeLog(hint);
					
			}
			else {
				Log.error(printWriter, "400", "Bad Request", "Your request line: "+string, ip);
				return;
			}
			
			// get other headers
			string=bufferedReader.readLine();
			Date lastModified=null;
			while (!"".equals(string) && !"\r\n".equals(string)) {
				string=string.trim();
				
				// get if-modified-since
				if (string.startsWith("If-Modified-Since")) {
					int pos=string.indexOf(":");
					string=string.substring(pos+1).trim();
					try {
						lastModified=simpleDateFormat.parse(string);
					}
					catch (Exception e) {}
				}
				
				string=bufferedReader.readLine();
			}
			
			if (uri.startsWith("/")) uri=uri.substring(1);
			
			File file=new File(uri);
			if (!file.exists()) {
				Log.error(printWriter, "404", "Not found", "File not found.", ip);
				return;
			}
			
			String mimeType=getMIMEType(file);
			long fileLaseModified=file.lastModified();
			// 304
			if (lastModified!=null
					&& lastModified.getTime()>=fileLaseModified) {
				printWriter.print("HTTP/1.1 304 Not Modified\r\n");
				printWriter.print("\r\n");
				printWriter.flush();
				Log.writeLog("Response to "+ip+": HTTP/1.1 304 Not Modified");
				return;
			}
			
			printWriter.print("HTTP/1.1 200 OK\r\n");
			Log.writeLog("Response to "+ip+": HTTP/1.1 200 OK");
			printWriter.print("Date: "+simpleDateFormat.format(new Date())+"\r\n");
			printWriter.print("Last-Modified: "+simpleDateFormat.format(fileLaseModified)+"\r\n");
			printWriter.print("Content-type: "+mimeType+"\r\n");
			printWriter.print("Content-length: "+file.length()+"\r\n");
			printWriter.print("\r\n");
			printWriter.flush();
			
			// get file content
			if (!method.equals("HEAD")) {	
				byte[] bytes=new byte[(int)file.length()];
				FileInputStream fileInputStream=new FileInputStream(file);
				fileInputStream.read(bytes);
				fileInputStream.close();
				// To transfer as text?
				if (mimeType.startsWith("text")) {
					printWriter.print(new String(bytes, "utf-8"));
					printWriter.flush();
				}
				else {
					socket.getOutputStream().write(bytes);
					socket.getOutputStream().flush();
				}			
			}
		}
		catch (Exception e) {e.printStackTrace();}
		finally {
			try {
				if (socket!=null)
					socket.close();
			}
			catch (Exception e) {}
		}
	}
	
	String getMIMEType(File file) {
		String[][] MIME_TABLE={
			{".3gp","video/3gpp"},{".apk","application/vnd.android.package-archive"},
			{".asf","video/x-ms-asf"},{".avi","video/x-msvideo"},
			{".bmp","image/bmp"},{".txt","text/plain"},{".jpg","image/jpeg"},
			{".jpeg","image/jpeg"},{".jpz","image/jpeg"},
			{".doc","application/msword"},{".docx","application/msword"},
			{".png","image/png"},
			{".html","text/html"},{".htm","text/html"},{".ico","image/x-icon"},
			{".js","application/x-javascript"},{".mp3","audio/x-mpeg"},
			{".mov","video/quicktime"},{".mpg","video/x-mpeg"},{".mp4","video/mp4"},
			{".pdf","application/pdf"},{".ppt","application/vnd.ms-powerpoint"},
			{".swf","application/x-shockwave-flash"},{".wav","audio/x-wav"},
			{".zip","application/zip"},{".rar","application/x-rar-compressed"},
			{".rm","audio/x-pn-realaudio"},{".rmvb","audio/x-pn-realaudio"}
		};
		String type="*/*";
		String end=getSuffix(file);
		if (end.equals("")) return type;
		for (int i=0;i<MIME_TABLE.length;i++) {
			if (end.equals(MIME_TABLE[i][0])) type=MIME_TABLE[i][1];
		}
		return type;
	}
	
	String getSuffix(File file) {
		String fname=file.getName();
		int doIndex=fname.lastIndexOf(".");
		if (doIndex<0) return "";
		return fname.substring(doIndex, fname.length()).toLowerCase(Locale.getDefault());
	}
}
