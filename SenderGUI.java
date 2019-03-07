import java.awt.EventQueue;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.TextArea;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.*;
import javax.swing.*;

public class SenderGUI extends JFrame implements ActionListener {
	static JPanel cnctPanel = new JPanel();
	static JPanel guiPanel = new JPanel();
	static JLabel file = new JLabel("File Name:");
	static JLabel rcvrIP = new JLabel("Receiver IP:");
	static JLabel rcvrPortNum = new JLabel("Receiver Port Number:");
	static JLabel sndrPortNum = new JLabel("Sender Port Number:");
	static JLabel datagramSize = new JLabel("Datagram Size:");
	static JLabel timeoutLabel = new JLabel("Timeout (milliseconds):");
	static JTextField fileText = new JTextField();
	static JTextField rcvrIPText = new JTextField("127.0.0.1");
	static JTextField rcvrPortText = new JTextField("9898");
	static JTextField sndrPortText = new JTextField("9899");
	static JTextField datagramText = new JTextField();
	static JTextField timeoutText = new JTextField();
	static TextArea textArea = new TextArea();
	static JButton trnsfr = new JButton("Transfer");
	static JButton cnct = new JButton("Connect");
	static JButton dscnct = new JButton("Disconnect");
	static File trnsfr_file;
	static InetAddress rIP;
	static int rcvrPort;
	static int sndrPort;
	static int mds;
	static int timeout;
	static long trnsTime = 0;
	static DatagramSocket socket;
	static FileInputStream fileStream;
	static double bytes;
	static int seq_num;
	static byte[][] pkts;
	static int acked[];
	static Thread thread;
	static boolean transfering = false;

	public SenderGUI() {
		this.setBounds(100, 100, 400, 200);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setVisible(true);
		this.setTitle("Sender GUI");

		connectPanel();
		cnct.addActionListener(this);

		dscnct.addActionListener(this);
		trnsfr.addActionListener(this);
	}

	public void connectPanel() {
		add(cnctPanel);
		cnctPanel.setLayout(null);
		cnctPanel.setVisible(true);
		this.setSize(400, 200);

		cnctPanel.add(rcvrIP);
		cnctPanel.add(rcvrIPText);
		cnctPanel.add(rcvrPortNum);
		cnctPanel.add(rcvrPortText);
		cnctPanel.add(sndrPortNum);
		cnctPanel.add(sndrPortText);
		cnctPanel.add(cnct);

		rcvrIP.setBounds(30, 20, 150, 15);
		rcvrIPText.setBounds(175, 18, 150, 20);
		rcvrPortNum.setBounds(30, 50, 150, 15);
		rcvrPortText.setBounds(175, 48, 150, 20);
		sndrPortNum.setBounds(30, 80, 150, 15);
		sndrPortText.setBounds(175, 78, 150, 20);
		cnct.setBounds (150, 110, 100, 30);
		cnct.setMnemonic('C');
	}

	public void connectedPanel() {
		add(guiPanel);
		guiPanel.setLayout(null);
		guiPanel.setVisible(true);
		this.setSize(600, 300);

		guiPanel.add(file);
		guiPanel.add(fileText);
		guiPanel.add(datagramSize);
		guiPanel.add(datagramText);
		guiPanel.add(timeoutLabel);
		guiPanel.add(timeoutText);
		guiPanel.add(textArea);
		guiPanel.add(dscnct);
		guiPanel.add(trnsfr);

		file.setBounds(30, 20, 150, 15);
		fileText.setBounds(180, 18, 150, 20);
		datagramSize.setBounds(30, 70, 150, 15);
		datagramText.setBounds(180, 68, 150, 20);
		timeoutLabel.setBounds(30, 120, 150, 15);
		timeoutText.setBounds(180, 118, 150, 20);
		textArea.setBounds(340, 18, 230, 225);
		textArea.setEditable(false);
		trnsfr.setBounds(60, 170, 100, 30);
		trnsfr.setMnemonic('T');
		dscnct.setBounds(190, 170, 100, 30);
		dscnct.setMnemonic('D');
		
	}
	
	public void createThread() {
		thread = new Thread () {
			public void run() {
				try {
					byte[] handshake = (Integer.toString(mds + 4) + " " + Integer.toString(seq_num) + " "
										+ Integer.toString((int) (((seq_num) * (mds + 4)) - bytes)) + " ").getBytes();
					DatagramPacket dPkt = new DatagramPacket(handshake, handshake.length);
					socket.send(dPkt);
					byte[] buffer = new byte[4];
					DatagramPacket p = new DatagramPacket(buffer, 4);
					socket.receive(p);
					transfering = true;
					int count = 0;

					while (transfering) {
						transfering = false;

						try {
							byte[] send = new byte[4 + mds];
							byte[] result = new byte[] { (byte) (count >> 24), (byte) (count >> 16),
														(byte) (count >> 8), (byte) count};
							System.arraycopy(result, 0, send, 0, 4);
							System.arraycopy(pkts[count], 0, send, 4, pkts[count].length);

							DatagramPacket n = new DatagramPacket(send, send.length);
							socket.send(n);

							byte[] tmp = new byte[mds];
							DatagramPacket ack = new DatagramPacket(tmp, 4);
							socket.receive(ack);
							
							int seq = java.nio.ByteBuffer.wrap(tmp).getInt();

							if (count == seq) {
								acked[seq] = 1;
								count++;
							}

						}  catch (SocketTimeoutException i) {
							textArea.setText("Receive timedout.");
						}
					}

					byte b[] = new byte[4];
					ByteBuffer tmp = ByteBuffer.wrap(b);
					tmp.putInt(-1);
					DatagramPacket done = new DatagramPacket(b, 4);
					socket.send(done);
					trnsTime = System.currentTimeMillis() - trnsTime;
					textArea.setText(String.format("It took %dms to transfer the file.", trnsTime));

				} catch (Exception e) {
					JOptionPane.showMessageDialog(null,
						"Error creating handshake.",
						"Handshake error",
						JOptionPane.ERROR_MESSAGE);
				}
			}
		};
	}

	public void makeConnection() {
		try {
			rIP = InetAddress.getByName(rcvrIPText.getText());
			rcvrPort = Integer.parseInt(rcvrPortText.getText());
			sndrPort = Integer.parseInt(sndrPortText.getText());
			socket = new DatagramSocket(sndrPort);
			socket.connect(rIP, rcvrPort);
			cnctPanel.setVisible(false);
			connectedPanel();

		} catch (Exception a) {
			JOptionPane.showMessageDialog(null,
				"Incorrect Port or IP Address.",
				"IP/Port Error",
				JOptionPane.ERROR_MESSAGE);
		}
	}

	public void transferFile() {
		try {
			try {
				trnsfr_file = new File(fileText.getText());
				fileStream = new FileInputStream(trnsfr_file);
			} catch (Exception c) {
				JOptionPane.showMessageDialog(null,
					"The file with that name could not be found.",
					"File Not Found",
					JOptionPane.ERROR_MESSAGE);
			}

			mds = Integer.parseInt(datagramText.getText());
			timeout = Integer.parseInt(timeoutText.getText());
			socket.setSoTimeout(timeout);

			trnsTime = System.currentTimeMillis();
			bytes = trnsfr_file.length();
			seq_num = (int) Math.ceil(bytes / mds);

			pkts = new byte[seq_num][mds];
			acked = new int[seq_num];
			for (int i = 0; i < seq_num; i++) {
				acked[i] = 0;
				fileStream.read(pkts[i]);
			}

			createThread();
			thread.start();

		} catch (Exception b) {
			JOptionPane.showMessageDialog(null,
				"Datagram Size/Timeout incorrect.",
				"Incomplete Fields",
				JOptionPane.ERROR_MESSAGE);
		}
	}


	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					SenderGUI window = new SenderGUI();
					window.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String  button = e.getActionCommand();

		if (button.equals("Connect")) {
			makeConnection();

		} else if (button.equals("Transfer")) {
			transferFile();

		} else if (button.equals("Disconnect")) {
			try {
				socket.close();
				guiPanel.setVisible(false);
				connectPanel();

			} catch (Exception d) {
				JOptionPane.showMessageDialog(null,
					"Could not close connection.",
					"Disconnect Error",
					JOptionPane.ERROR_MESSAGE);
			}
		}
	}
}
