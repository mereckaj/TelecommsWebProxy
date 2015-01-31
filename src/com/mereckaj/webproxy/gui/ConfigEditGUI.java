package com.mereckaj.webproxy.gui;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;

import javax.swing.JFormattedTextField;
import javax.swing.text.NumberFormatter;
import javax.swing.JRadioButton;

import com.mereckaj.webproxy.ProxySettings;
import java.text.Format;
import javax.swing.JTextField;

public class ConfigEditGUI {

	private JFrame frame;
	private JTextField txtError;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ConfigEditGUI window = new ConfigEditGUI();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public ConfigEditGUI() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException
				| IllegalAccessException | UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		frame.setResizable(false);

		JLabel lblPort = new JLabel("Port");
		lblPort.setBounds(12, 12, 142, 15);
		frame.getContentPane().add(lblPort);

		JLabel lblLogging = new JLabel("Logging Enabled");
		lblLogging.setBounds(12, 61, 142, 15);
		frame.getContentPane().add(lblLogging);

		JLabel lblFiltering = new JLabel("Filtering");
		lblFiltering.setBounds(12, 88, 142, 15);
		frame.getContentPane().add(lblFiltering);

		JButton btnLogFie = new JButton("Select");
		btnLogFie.setBounds(165, 110, 117, 25);
		frame.getContentPane().add(btnLogFie);

		JButton btnBlockedHost = new JButton("Select");
		btnBlockedHost.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			}
		});
		btnBlockedHost.setBounds(165, 138, 117, 25);
		frame.getContentPane().add(btnBlockedHost);

		final JFormattedTextField proxyPortField = new JFormattedTextField(
				NumberFormat.getInstance());
		proxyPortField.setBounds(165, 12, 123, 19);
		frame.getContentPane().add(proxyPortField);
		proxyPortField.setText(new String(ProxySettings.getInstance()
				.getProxyPort() + ""));

		JButton btnSave = new JButton("Save");
		btnSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// TODO: SAVE SETTINGS
				frame.dispose();
			}
		});
		btnSave.setBounds(165, 236, 117, 25);
		frame.getContentPane().add(btnSave);

		JRadioButton rbtnLoggingEnabled = new JRadioButton("");
		rbtnLoggingEnabled.setSelected(ProxySettings.getInstance()
				.isLoggingEnabled());
		rbtnLoggingEnabled.setBounds(165, 57, 149, 23);
		frame.getContentPane().add(rbtnLoggingEnabled);

		JRadioButton rbtnFilteringEnabled = new JRadioButton("");
		rbtnFilteringEnabled.setSelected(ProxySettings.getInstance()
				.isFilteringEnabled());
		rbtnFilteringEnabled.setBounds(165, 80, 149, 23);
		frame.getContentPane().add(rbtnFilteringEnabled);

		JLabel lblLogFile = new JLabel("Log file");
		lblLogFile.setBounds(12, 115, 142, 15);
		frame.getContentPane().add(lblLogFile);

		JLabel lblBlockedHostFile = new JLabel("Blocked host file");
		lblBlockedHostFile.setBounds(12, 143, 142, 15);
		frame.getContentPane().add(lblBlockedHostFile);

		JLabel lblFilterFile = new JLabel("Filter file");
		lblFilterFile.setBounds(12, 170, 142, 15);
		frame.getContentPane().add(lblFilterFile);

		JButton btnFilterFile = new JButton("Select");
		btnFilterFile.setBounds(165, 165, 117, 25);
		frame.getContentPane().add(btnFilterFile);

		JLabel lblBufferSize = new JLabel("Buffer size");
		lblBufferSize.setBounds(12, 39, 70, 15);
		frame.getContentPane().add(lblBufferSize);

		JFormattedTextField maxBufferField = new JFormattedTextField(
				(Format) null);
		maxBufferField.setText(new String(ProxySettings.getInstance()
				.getMaxBuffer() + ""));
		maxBufferField.setBounds(165, 37, 123, 19);
		frame.getContentPane().add(maxBufferField);

		JLabel lblLogPath = new JLabel("");
		lblLogPath.setBounds(300, 120, 136, 15);
		frame.getContentPane().add(lblLogPath);
		lblLogPath.setText(ProxySettings.getInstance().getPathToLog());

		JLabel blbBlockedFilePath = new JLabel("");
		blbBlockedFilePath.setBounds(300, 143, 136, 15);
		frame.getContentPane().add(blbBlockedFilePath);
		blbBlockedFilePath.setText(ProxySettings.getInstance()
				.getPathToBlocked());

		JLabel lblFilterFilePath = new JLabel("");
		lblFilterFilePath.setBounds(300, 170, 136, 15);
		frame.getContentPane().add(lblFilterFilePath);
		lblFilterFilePath
				.setText(ProxySettings.getInstance().getPathToFilter());

		txtError = new JTextField();
		txtError.setEditable(false);
		txtError.setBounds(12, 208, 424, 19);
		frame.getContentPane().add(txtError);
		txtError.setColumns(10);
	}
}
