package ass;

import java.awt.Font;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

@SuppressWarnings("serial")
public class Receiver extends JFrame implements ActionListener {
	static boolean transmitting = true;
	static boolean reliable = false;
	static int SizeOfPacket;
	static int NumOfPackets;
	static DatagramSocket socket = null;
	static InetAddress address;
	static Thread receiving;
	static String fileName;
	static int leftOverByte;
	static boolean[] acks;
	static boolean stop_receiving = false;

	private JFrame frame;
	private JFrame frame2;
	JTextField RcvPortTextField = new JTextField("4433");
	JTextField ipTextField = new JTextField("localhost");
	JTextField SenderportTextField = new JTextField("3344");
	JTextField fileNameTextField = new JTextField("Test.txt");
	TextArea displayArea = new TextArea();
	JCheckBox chckbxreliable = new JCheckBox("Reliable");

	public Receiver() {
		frame2 = new JFrame("Connection");
		frame2.setBounds(100, 100, 375, 205);
		frame2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame2.getContentPane().setLayout(null);

		JLabel lblIPAddress = new JLabel("Sender IP Address:");
		lblIPAddress.setFont(new Font("Tahoma", Font.PLAIN, 12));
		lblIPAddress.setBounds(33, 21, 140, 14);
		frame2.getContentPane().add(lblIPAddress);

		JLabel lblPortNumber = new JLabel("UDP Port of Sender:");
		lblPortNumber.setFont(new Font("Tahoma", Font.PLAIN, 12));
		lblPortNumber.setBounds(33, 48, 140, 14);
		frame2.getContentPane().add(lblPortNumber);

		JLabel lblPortNumber2 = new JLabel("UDP Port of Receiver:");
		lblPortNumber2.setFont(new Font("Tahoma", Font.PLAIN, 12));
		lblPortNumber2.setBounds(33, 75, 140, 14);
		frame2.getContentPane().add(lblPortNumber2);

		ipTextField.setBounds(155, 21, 185, 20);
		frame2.getContentPane().add(ipTextField);
		ipTextField.setColumns(10);

		JLabel lblFileName = new JLabel("Filename:");
		lblFileName.setFont(new Font("Tahoma", Font.PLAIN, 12));
		lblFileName.setBounds(33, 102, 140, 14);
		frame2.getContentPane().add(lblFileName);

		fileNameTextField.setBounds(155, 102, 185, 20);
		frame2.getContentPane().add(fileNameTextField);
		fileNameTextField.setColumns(10);

		RcvPortTextField.setBounds(155, 75, 185, 20);
		frame2.getContentPane().add(RcvPortTextField);
		RcvPortTextField.setColumns(10);

		SenderportTextField.setBounds(155, 48, 185, 20);
		frame2.getContentPane().add(SenderportTextField);
		SenderportTextField.setColumns(10);

		JButton btnConnect = new JButton("Connect");
		btnConnect.setMnemonic('C');
		btnConnect.addActionListener(this);
		btnConnect.setVisible(true);
		btnConnect.setEnabled(true);
		btnConnect.setBounds(33, 127, 307, 23);
		frame2.getContentPane().add(btnConnect);

		frame = new JFrame("Receiver");
		frame.setVisible(false);
		frame.setBounds(100, 100, 500, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);

		chckbxreliable.setBounds(346, 41, 113, 25);
		chckbxreliable.addActionListener(this);
		frame.getContentPane().add(chckbxreliable);

		displayArea.setBounds(20, 10, 300, 210);
		frame.getContentPane().add(displayArea);
		displayArea.setEditable(false);

		JButton btnDisconnect = new JButton("Disconnect");
		btnDisconnect.setMnemonic('D');
		btnDisconnect.setBounds(346, 200, 110, 23);
		btnDisconnect.addActionListener(this);
		frame.getContentPane().add(btnDisconnect);
	}

	public void receiverThread() {
		receiving = new Thread() {
			public void run() {
				try {
					int packet_count = 1;
					transmitting = true;
					// HANDSHAKE-----------------------------------------------------
					String packetInfo[] = sync_handshake().split(" ");
					SizeOfPacket = Integer.parseInt(packetInfo[0]);
					NumOfPackets = Integer.parseInt(packetInfo[1]);
					leftOverByte = Integer.parseInt(packetInfo[2]);

					// Create buffers/2D file array for packets-----------------------
					byte[][] file = new byte[NumOfPackets][SizeOfPacket];
					acks = new boolean[NumOfPackets];
					displayArea.setText("");
					byte[] buffer = new byte[SizeOfPacket];

					// Create initial packet-----------------------------------------
					DatagramPacket packet = new DatagramPacket(buffer, SizeOfPacket);
					// Loop while still receiving------------------------------------
					while (transmitting) {
						packet_count++;
						socket.receive(packet);
						byte[] SNB = Arrays.copyOfRange(buffer, 0, 4);
						byte[] FPB;
						int sequenceNumber = java.nio.ByteBuffer.wrap(SNB).getInt();
						if (sequenceNumber == (NumOfPackets)) {
							FPB = Arrays.copyOfRange(buffer, 4, leftOverByte);
						} else {
							FPB = Arrays.copyOfRange(buffer, 4, buffer.length);
						}
						if (sequenceNumber == -1) {
							transmitting = false;
							FileOutputStream fileoutput = new FileOutputStream(fileName);
							for (int i = 0; i < NumOfPackets; i++) {
								fileoutput.write(file[i]);
							}
							fileoutput.close(); // KEEP AN EYE ON THIS
							displayArea.setText(displayArea.getText() + "File Received\n");
							displayArea.setCaretPosition(displayArea.getText().length() - 1);

						} else if (packet_count % 10 != 0 || reliable) {
							displayArea.setText(displayArea.getText() + "Packet number " + (sequenceNumber + 1) + " of "
									+ NumOfPackets + " received in order\n");
							displayArea.setCaretPosition(displayArea.getText().length() - 1);

							file[sequenceNumber] = FPB;
							acks[sequenceNumber] = true;

							DatagramPacket pSend = new DatagramPacket(SNB, 4);
							socket.send(pSend);

						}
					}

				} catch (Exception e) {
					System.out.println(e);
				}
				if (stop_receiving) {
					return;
				}
				if (!transmitting) {
					receiverThread();
					receiving.start();
				}
			}

		};

	}

	public static void main(final String[] args) {
		Receiver window = new Receiver();
		window.frame2.setVisible(true);
	}

	public String sync_handshake() {
		try {
			// Receive the SYN packet from sender---------------------
			byte[] bruce = new byte[64];
			DatagramPacket packet = new DatagramPacket(bruce, 64);
			socket.receive(packet);
			String sync = new String(bruce);

			// Send SYNACK packet to sender-------------------------
			bruce = new byte[4];
			ByteBuffer buffer = ByteBuffer.wrap(bruce);
			buffer.putInt(-1);
			DatagramPacket complete = new DatagramPacket(bruce, 4);
			socket.send(complete);

			return sync;
		} catch (Exception a) {
			System.out.println(a);
		}
		return "";
	}

	public void makeConnection() {
		try {
			socket = new DatagramSocket(Integer.parseInt(SenderportTextField.getText()));
			address = InetAddress.getByName(ipTextField.getText());
			socket.connect(address, Integer.parseInt(RcvPortTextField.getText()));
			fileName = fileNameTextField.getText();
			frame2.setVisible(false);
			frame.setVisible(true);
		} catch (Exception ae) {
			JOptionPane.showMessageDialog(null, "Could not connect", "Connection Issue", JOptionPane.ERROR_MESSAGE);
		}
	}

	public void killConnection() {
		try {
			socket.close();
			displayArea.setText("");
			frame2.setVisible(true);
			frame.setVisible(false);
		} catch (Exception ae) {
			JOptionPane.showMessageDialog(null, "Could not close connection", "Connection Issue",
					JOptionPane.ERROR_MESSAGE);

		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		String action = e.getActionCommand();
		if (action.equals("Connect")) {
			makeConnection();
			stop_receiving = false;
			receiverThread();
			receiving.start();
			// System.out.println("Here");
		} else if (action.equals("Disconnect")) {
			killConnection();
		} else if (action.equals("Reliable")) {
			reliable = chckbxreliable.isSelected();
		}

	}

}
