package sk.upjs.siete;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class FileSenderTask implements Runnable {

	public static final int CHUNK_SIZE = 1000;

	private Interval partsToSend;
	private File file;

	public FileSenderTask(Interval partsToSend, File file) {
		this.partsToSend = partsToSend;
		this.file = file;
	}

	@Override
	public void run() {
		try {
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			while (true) {
				Interval interval = partsToSend.getAndEraseNextFullSubintervalBlocked(CHUNK_SIZE);
				raf.seek(interval.getMin());
				byte[] buffer = new byte[CHUNK_SIZE];
				raf.read(buffer);

				ByteArrayOutputStream baos = new ByteArrayOutputStream(CHUNK_SIZE + 8);
				ObjectOutputStream oos = new ObjectOutputStream(baos);
				oos.writeLong(interval.getMin());
				oos.writeInt(CHUNK_SIZE);
				oos.write(buffer);
				oos.flush();
				oos.close();
				buffer = baos.toByteArray();
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName("localhost"),
						InfoServer.FILE_RECIEVE_PORT);
				DatagramSocket socket = new DatagramSocket();
				socket.send(packet);

				// TODO: vytvorime pole byteov pre packet (offset, velkost pola, pole) a posleme
				// to broadcastom vsetkym klientom na port FILE_RECIEVE_PORT
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
