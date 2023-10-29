import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;


public class Receiver {
    public static void main(String[] args) {
        if (args.length != 0) {
            System.out.println("Usage: java Receiver");
            return;
        }

        try {
            // Create a DatagramSocket that listens on port 6500
            DatagramSocket socket = new DatagramSocket(6500);
            byte[] receiveData = new byte[504];
            int sequenceNumber = 0;
            String receivedFileName = null;

            while (true) {
                // Create a DatagramPacket to receive data
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);

                // Extract the data from the received packet
                byte[] packetData = receivePacket.getData();
                String packetContent = new String(packetData, 0, receivePacket.getLength());

                // Check if it's a METADATA packet
                if (packetContent.startsWith("METADATA:")) {
                    // Parse the metadata information from the packet
                    String[] metaData = packetContent.split(":");
                    receivedFileName = metaData[1];
                    int fileSize = Integer.parseInt(metaData[2]);
                    System.out.println("Received metadata - File Name: " + receivedFileName + ", File Size: " + fileSize);
                } else {
                    // Handle data packets here
                    if (receivedFileName != null) {
                        // Generate the name for the received file
                        String originalFileName = "received_" + receivedFileName;

                        // Write the data to the file with the received file name
                        FileOutputStream fileOutputStream = new FileOutputStream(originalFileName, true);

                        // Write the data excluding the first 4 bytes (sequence number)
                        fileOutputStream.write(packetData, 4, receivePacket.getLength() - 4);
                        fileOutputStream.close();
                    }
                }

                // Send an acknowledgment for the received packet
                byte[] ackData = ByteBuffer.allocate(4).putInt(sequenceNumber).array();
                DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, receivePacket.getAddress(), 6000);
                socket.send(ackPacket);

                sequenceNumber++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
