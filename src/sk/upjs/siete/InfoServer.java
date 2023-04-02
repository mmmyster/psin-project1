package sk.upjs.siete;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InfoServer {

	public static final String FILE_PATH = "/Users/jakub/Desktop/javahead-title.png";
	public static final int INFO_SERVER_PORT = 9876;
	public static final String INFOR_REQUEST = "send info";
	public static final int FILE_RECIEVE_PORT = 8765; // all clients are listening here
	public static final int REQUEST_SERVER_PORT = 11000;

	public static void main(String[] args) {
		File fileToSend = new File(FILE_PATH);
		if (!fileToSend.exists() || !fileToSend.isFile()) {
			System.out.println("file not found");
			return;
		}
		String fileName = fileToSend.getName();
		long fileSize = fileToSend.length();

		ExecutorService threadService = Executors.newCachedThreadPool();
		Interval partsToSend = Interval.empty(0, fileSize);
		RequestServerTask requestServerTask = new RequestServerTask(partsToSend);
		threadService.execute(requestServerTask);
		FileSenderTask fileSenderTask = new FileSenderTask(partsToSend, fileToSend);
		threadService.execute(fileSenderTask);

		byte[] fileInfo = (fileName + "\n" + fileSize).getBytes();
		System.out.println("info about file: " + fileName + " with size: " + fileSize);
		try (DatagramSocket socket = new DatagramSocket(INFO_SERVER_PORT)) {

			while (true) {

				// waiting for client's request
				byte[] buffer = new byte[socket.getReceiveBufferSize()];
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				socket.receive(packet);

				// checking if request is valid
				byte[] requestData = packet.getData();
				String request = new String(requestData).trim();
				if (INFOR_REQUEST.equals(request)) {
					InetAddress ip = packet.getAddress();
					int port = packet.getPort();
					DatagramPacket responsePacket = new DatagramPacket(fileInfo, fileInfo.length, ip, port);
					socket.send(responsePacket);
					System.out.println("sending info to client: " + ip + " : " + port);
				}
			}

		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
