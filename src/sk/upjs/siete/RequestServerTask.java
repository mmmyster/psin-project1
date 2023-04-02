package sk.upjs.siete;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class RequestServerTask implements Runnable {

	private Interval partsToSend;

	public RequestServerTask(Interval partsToSend) {
		this.partsToSend = partsToSend;
	}

	@Override
	public void run() {
		try (DatagramSocket socket = new DatagramSocket(InfoServer.REQUEST_SERVER_PORT)) {
			while (true) {

				// waiting for client's request
				byte[] buffer = new byte[socket.getReceiveBufferSize()];
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				socket.receive(packet);
				System.out.println("RequestServer: request recieved");

				byte[] data = packet.getData();
				ByteArrayInputStream bais = new ByteArrayInputStream(data);
				ObjectInputStream ois = new ObjectInputStream(bais);
				int count = ois.readInt();
				for (int i = 0; i < count; i++) {
					long min = ois.readLong();
					long max = ois.readLong();
					partsToSend.addFullSubinterval(min, max);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
