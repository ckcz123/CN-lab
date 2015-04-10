import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Scanner;

public class ProxyThread extends Thread implements Runnable {
	
	Socket socket;
	String hostname, address, port;
	boolean isUsingHttps=false;
	public ProxyThread(Socket _socket) {
		socket=_socket;isUsingHttps=false;
	}
	public void run() {
		try {
			InetAddress inetAddress=socket.getInetAddress();
			if (inetAddress==null) return;
			String ip=inetAddress.getHostAddress();
			InputStream inputStream=socket.getInputStream();
			
			String string=readLine(inputStream);
			
	//		if (string==null) return;
			Scanner scanner=new Scanner(string);
			String method=scanner.next().toLowerCase(Locale.getDefault());
			String uri=scanner.next();
			scanner.close();
			
			// https?
			if ("connect".equals(method)) {
				readToBlankLine(inputStream);
				proxyUseHttps(uri);
				isUsingHttps=true;
				return;
			}
			
			// Only allows GET and HEAD request?
			/*
			if (!"get".equals(method) && !"head".equals(method)){
				Log.error(new PrintWriter(socket.getOutputStream()), "500", "METHOD ERROR", "Please use GET or HEAD method! (Your method: "
							+method.toUpperCase(Locale.getDefault())+" Url: "+uri+")", ip);
				return;
			}
			*/
			// Only allows HTTP request
			if (!uri.startsWith("http://")) {
				Log.error(new PrintWriter(socket.getOutputStream()), "500", "HTTP ERROR", "Please use HTTP request!"
						+" ("+uri+")", ip);
				return;
			}
			
			parse_uri(uri.substring(7));
			Log.writeLog("New proxy request from "+ip+" (Request " +hostname+": "+address+" "+port+" HTTP/1.0)");
			
			String headers="";
			headers+=String.format("%s %s HTTP/1.0\r\nHost: %s\r\nConnection: close\r\nProxy-Connection: close\r\n\r\n", 
					method.toUpperCase(Locale.getDefault()),address,hostname);
			
			// add other headers...
			//string=bufferedReader.readLine();
			string=readLine(inputStream);
			while (!"".equals(string)) {
				string=string.trim();
				if (!string.startsWith("Host:") && !string.startsWith("Connection:")) {
					headers+=string+"\r\n";
				}
				//string=bufferedReader.readLine();
				string=readLine(inputStream);
			}
			
			try {
				Socket clientSocket=new Socket();
				clientSocket.connect(new InetSocketAddress(InetAddress.getByName(hostname), Integer.parseInt(port)));
				
				if (clientSocket.isConnected()) {
					PrintWriter clientWriter=new PrintWriter(clientSocket.getOutputStream());
					clientWriter.write(headers);
					clientWriter.flush();
					
					byte[] bytes=new byte[10240];

					int readNum=0;
					int totalBytes=0;
					inputStream=clientSocket.getInputStream();
					
					byte[] typebytes=new byte[8];
					inputStream.read(typebytes);
					// change HTTP/1.1 to HTTP/1.0
					if (new String(typebytes).equals("HTTP/1.1"))
						typebytes[7]='0';
					socket.getOutputStream().write(typebytes);
					totalBytes+=8;
					
					while (true) {						
						readNum=inputStream.read(bytes);
						if (readNum<=0) break;
						socket.getOutputStream().write(bytes, 0, readNum);
						totalBytes+=readNum;
					}
					socket.getOutputStream().flush();
					
					Log.writeLog("Proxy request from "+ip
							+" to "+hostname+" finished! Content-Size: "+FormetFileSize(totalBytes));
					
				}
				else {
					Log.error(new PrintWriter(socket.getOutputStream()), "404", "Not found", "Cannot connect to "+hostname, ip);
				}
			
				clientSocket.close();
			}
			catch (Exception e) {
				Log.error(new PrintWriter(socket.getOutputStream()), "500", "Internal Error", e.getMessage()
						+" ("+uri+")", ip);
			}
		}
		catch (Exception e) {}
		finally {
			try {
				if (socket!=null && !isUsingHttps)
					socket.close();
			}
			catch (Exception e) {}
		}
		
	}
	
	void proxyUseHttps(String uri) {
		parse_uri(uri);
		
		try {
			Socket serverSocket=new Socket();
			serverSocket.connect(new InetSocketAddress(InetAddress.getByName(hostname), Integer.parseInt(port)));
			
			if (serverSocket.isConnected()) {

				PrintWriter printWriter=new PrintWriter(socket.getOutputStream());
				printWriter.write("HTTP/1.1 200 OK\r\n\r\n");
				printWriter.flush();
				
				new HttpsProxyThread(socket, serverSocket).start();
				new HttpsProxyThread(serverSocket, socket).start();
				
				Log.writeLog("HTTPS proxy request from "+
						socket.getInetAddress().getHostAddress()+" to "+hostname+" start!");
				
			}
			else {
				Log.writeLog("Error: https connect not establised.");
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
	
	String readLine(InputStream inputStream) {
		String string="";
		int data;
		try {
			while (true) {
				data=inputStream.read();
				if (data=='\r') {
					data=inputStream.read();
					if (data=='\n') return string;
					string+='\r';
				}
				else string+=(char)data;
			}
		}
		catch (Exception e) {
			return string;
		}
		
	}
	
	void readToBlankLine(InputStream inputStream) {
		while (!"".equals(readLine(inputStream)));
	}
	
	void parse_uri(String uri) {
	    int i;
	    port="80";
	    hostname=new String();
	    address=new String();
	    for (i=0;i<uri.length();i++)
	    {
	    	char c=uri.charAt(i);
	        if (c=='/' || c==':')
	            break;
	        hostname+=c;
	    }
	    if (i==uri.length())
	    {
	        address="/";
	        return;
	    }
	    if (uri.charAt(i)==':')
	    {
	        int tmp=0; // get port
	        i++;
	        while (i<uri.length() && uri.charAt(i)!='/')
	        {
	            tmp*=10;
	            tmp+=uri.charAt(i)-'0';
	            i++;
	        }
	        port=tmp+"";
	    }
	    if (i==uri.length())
	    {
	        address="/";
	        return;
	    }
	    // here, uri[i] = '/';
	    address=new String(uri.substring(i));
	}
	
	String FormetFileSize(long fileS) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        if (fileS < 1024) {
            fileSizeString = df.format((double) fileS) + "B";
        } else if (fileS < 1048576) {
            fileSizeString = df.format((double) fileS / 1024) + "K";
        } else if (fileS < 1073741824) {
            fileSizeString = df.format((double) fileS / 1048576) + "M";
        } else {
            fileSizeString = df.format((double) fileS / 1073741824) + "G";
        }
        return fileSizeString;
    }
	
}
