package sk.upjs.siete;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Scanner;

public class Client {

	public static final int TIMEOUT = 200;
	public static final int MAX_INTERVALS = 62;

	public static void main(String[] args) {
		try (DatagramSocket infoSocket = new DatagramSocket()) {
			byte[] requestData = InfoServer.INFOR_REQUEST.getBytes();
			DatagramPacket requestPacket = new DatagramPacket(requestData, requestData.length,
					InetAddress.getByName("localhost"), InfoServer.INFO_SERVER_PORT);
			infoSocket.send(requestPacket);

			// waiting for info about file from server
			byte[] buffer = new byte[infoSocket.getReceiveBufferSize()];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			infoSocket.receive(packet);

			byte[] recievedData = packet.getData();
			String recieved = new String(recievedData).trim();

			System.out.println("recieved " + recieved);
			Scanner sc = new Scanner(recieved);
			String fileName = sc.nextLine();
			long fileSize = sc.nextLong();
			sc.close();

			RandomAccessFile raf = new RandomAccessFile(fileName, "rw");
			raf.setLength(fileSize);
			Interval recievedParts = Interval.empty(0, fileSize);
			DatagramSocket dataSocket = new DatagramSocket(InfoServer.FILE_RECIEVE_PORT);
			while (true) {
				dataSocket.setSoTimeout(TIMEOUT + (int) Math.random() * TIMEOUT);
				byte[] dataBuffer = new byte[dataSocket.getReceiveBufferSize()];
				DatagramPacket dataPacket = new DatagramPacket(dataBuffer, dataBuffer.length);
				try {
					dataSocket.receive(dataPacket);
					ByteArrayInputStream bais = new ByteArrayInputStream(dataPacket.getData());
					ObjectInputStream ois = new ObjectInputStream(bais);
					int offset = ois.readInt();
					int length = ois.readInt();
					byte[] data = new byte[length];
					ois.read(data);
					raf.seek(offset);
					raf.write(data);

					// TODO: prijate data rozbit (offset, velkost pola, pole) a zapisat na disk a
					// zapisat (raf) si do recievedPrts co uz mam

					// TODO: ukoncit cyklus ked je recievedParts full

				} catch (SocketTimeoutException e) {

					// server isn't sending anything, gonna ask what's missing
					List<Interval> missing = recievedParts.getEmptySubintervals(MAX_INTERVALS);
					ByteArrayOutputStream baos = new ByteArrayOutputStream(4 + missing.size() * 16);
					ObjectOutputStream oos = new ObjectOutputStream(baos);
					oos.writeInt(missing.size());
					for (Interval interval : missing) {
						oos.writeLong(interval.getMin());
						oos.writeLong(interval.getMax());
					}
					oos.flush();
					byte[] reqData = baos.toByteArray();
					DatagramPacket reqPacket = new DatagramPacket(reqData, reqData.length, InetAddress.getByName("localhost"),
							InfoServer.REQUEST_SERVER_PORT);
					dataSocket.send(reqPacket);
				}
			}

		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
