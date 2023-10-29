import java.io.*;
import java.net.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Random;

public class Sender {
    public static void main(String[] args) {
        // Check if the required command-line arguments are provided
        if (args.length != 4) {
            System.out.println("Usage: java Sender <receiver_ip> <receiver_port> <file_path> <packet_size>");
            return;
        }

        // Extract command-line arguments
        String receiverIp = args[0];
        int receiverPort = Integer.parseInt(args[1]);
        String filePath = args[2];
        int packetSize = Integer.parseInt(args[3]);
        String originalFileName = new File(filePath).getName(); // Extract the original file name

        try {
            // Create a DatagramSocket for sending data
            DatagramSocket socket = new DatagramSocket(6000);
            InetAddress receiverAddress = InetAddress.getByName(receiverIp);

            // Read the file
            File file = new File(filePath);
            FileInputStream fileInputStream = new FileInputStream(file);
            int fileSize = (int) file.length();

            // Include the original file name and size in metadata
            String metaData = "METADATA:" + originalFileName + ":" + fileSize;
            byte[] metaDataBytes = metaData.getBytes();
            DatagramPacket metaPacket = new DatagramPacket(metaDataBytes, metaDataBytes.length, receiverAddress, receiverPort);
            socket.send(metaPacket);

            // Randomly select the packet loss probability from the given values (0.1, 0.3, 0.6)
            double[] possiblePacketLossProbabilities = {0.1, 0.3, 0.6};

            byte[] fileData = new byte[packetSize];
            int sequenceNumber = 0;
            int bytesRead;
            long startTime = System.currentTimeMillis();

            while ((bytesRead = fileInputStream.read(fileData)) != -1) {
                // Randomly determine packet loss with the specified probability
                double packetLossProbability = possiblePacketLossProbabilities[new Random().nextInt(possiblePacketLossProbabilities.length)];

                if (Math.random() > packetLossProbability) {
                    // Continue sending the packet
                    byte[] packetData = new byte[bytesRead + 4];
                    // Adding a header to the packet with the sequence number
                    byte[] sequenceBytes = new byte[4];
                    sequenceBytes[0] = (byte) (sequenceNumber >> 24);
                    sequenceBytes[1] = (byte) (sequenceNumber >> 16);
                    sequenceBytes[2] = (byte) (sequenceNumber >> 8);
                    sequenceBytes[3] = (byte) sequenceNumber;

                    System.arraycopy(sequenceBytes, 0, packetData, 0, 4);
                    System.arraycopy(fileData, 0, packetData, 4, bytesRead);

                    // Create a DatagramPacket for sending the data packet
                    DatagramPacket packet = new DatagramPacket(packetData, packetData.length, receiverAddress, receiverPort);
                    socket.send(packet);

                    // Set a timer for retransmission
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            // Resend the packet if no acknowledgment is received
                            try {
                                socket.send(packet);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }, 1000);

                    // Wait for acknowledgment
                    byte[] ackData = new byte[4];
                    DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length);
                    socket.receive(ackPacket);

                    // Check if the acknowledgment sequence matches the sent packet sequence
                    int ackSequence = (ackData[0] << 24) | (ackData[1] << 16) | (ackData[2] << 8) | ackData[3];
                    if (ackSequence == sequenceNumber) {
                        timer.cancel(); // Cancel the timer
                        sequenceNumber++;
                    }
                }
            }

            // Calculate transfer time and throughput
            long endTime = System.currentTimeMillis();
            double transferTime = (endTime - startTime) / 1000.0; // in seconds
            double throughput = (double) fileSize / (1024.0 * 1024.0) / transferTime; // in MBps
            System.out.println("File transfer completed.");
            System.out.println("Transfer time: " + transferTime + " seconds");
            System.out.println("Throughput: " + throughput + " MBps");

            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
