import java.net.*;

public class Server {
	public static void main(String[] args) {
		ServerSocket serverSocket=null;
		try {
			serverSocket=new ServerSocket();
			serverSocket.setReuseAddress(true);;
			serverSocket.setSoTimeout(10000);
			serverSocket.bind(new InetSocketAddress(10001));
			while (true) {
				try {
					Socket socket=serverSocket.accept();
					new ServerThread(socket).start();
				}
				catch (SocketTimeoutException e) {
				}
				catch (Exception e) {e.printStackTrace();}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				serverSocket.close();
			}
			catch (Exception e) {}
		}
	}
	
	
	
}
