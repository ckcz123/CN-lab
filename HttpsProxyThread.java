import java.net.Socket;

public class HttpsProxyThread extends Thread implements Runnable{
	Socket fromSocket,toSocket;
	public HttpsProxyThread(Socket _from, Socket _to) {
		fromSocket=_from;toSocket=_to;
	}
	public void run() {
		try {
			byte[] bts=new byte[10240];
			while (true) {
				int len;
				/*
				int data;
				data=fromSocket.getInputStream().read();
				if (data==-1)
					break;
				toSocket.getOutputStream().write(data);
				toSocket.getOutputStream().flush();
				//System.out.print((char)data);
				 */
				len=fromSocket.getInputStream().read(bts);
				if (len<=0) break;
				toSocket.getOutputStream().write(bts,0,len);
				toSocket.getOutputStream().flush();
			}
		}
		catch (Exception e) {}
	}
}
