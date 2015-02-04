package com.mereckaj.webproxy.gui;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.mereckaj.webproxy.ProxyTrafficFilter;

public class ProxyGUI {

	private JFrame frmWebProxy;
	final JFileChooser fc = new JFileChooser();
	private JTextField txtAbout;
	private JTextField txtInfoScreen;
	private JTextField infoField;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException
				| IllegalAccessException | UnsupportedLookAndFeelException e1) {
			e1.printStackTrace();
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ProxyGUI window = new ProxyGUI();
					window.frmWebProxy.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public ProxyGUI() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmWebProxy = new JFrame();
		frmWebProxy.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				int confirmed = JOptionPane.showConfirmDialog(null,
						"Are you sure you want to exit the program?",
						"Exit Program Message Box", JOptionPane.YES_NO_OPTION);

				if (confirmed == JOptionPane.YES_OPTION) {
					frmWebProxy.dispose();
				} else {
					frmWebProxy
							.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				}
			}
		});
		frmWebProxy.setTitle("HTTP Proxy Manager");
		frmWebProxy.setBounds(100, 100, 800, 400);
		frmWebProxy.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmWebProxy.setResizable(false);

		JMenuBar menuBar = new JMenuBar();
		frmWebProxy.setJMenuBar(menuBar);

		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);

		JMenuItem mntmOpenFile = new JMenuItem("Open File");
		mntmOpenFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int returnVal = fc.showOpenDialog(null);

				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
				} else {
					fc.setEnabled(false);
				}
			}
		});
		mnFile.add(mntmOpenFile);

		JMenuItem mntmClose = new JMenuItem("Close");
		mntmClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int confirmed = JOptionPane.showConfirmDialog(null,
						"Are you sure you want to exis?", "Exit Program ?",
						JOptionPane.YES_NO_OPTION);

				if (confirmed == JOptionPane.YES_OPTION) {
					frmWebProxy.dispose();
				} else {
					frmWebProxy
							.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				}
			}
		});
		mnFile.add(mntmClose);

		JMenu mnEdit = new JMenu("Edit");
		menuBar.add(mnEdit);

		JMenuItem mnConfig = new JMenuItem("Config");
		mnConfig.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

			}
		});
		mnEdit.add(mnConfig);

		JMenu mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);

		JMenuItem mntmAbout = new JMenuItem("About");
		mntmAbout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFrame frmAbout = new JFrame("About HTTP Proxy");
				int X = frmWebProxy.getWidth() / 2;
				int Y = frmWebProxy.getHeight() / 2;
				frmAbout.setBounds(X, Y, 300, 50);
				frmAbout.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				frmAbout.setResizable(false);
				txtAbout = new JTextField();
				txtAbout.setText("Created By Julius Mereckas for CS3031 -2015");
				frmAbout.getContentPane().add(txtAbout, BorderLayout.CENTER);
				txtAbout.setEditable(false);
				txtAbout.setColumns(10);
				frmAbout.setVisible(true);
			}
		});
		mnHelp.add(mntmAbout);
		frmWebProxy.getContentPane().setLayout(null);

		JButton btnBlockHost = new JButton("Block Host");
		btnBlockHost.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String hostToBlock = infoField.getText();
				infoField.setText("");
				int choice = JOptionPane.showConfirmDialog(null,
						"Are you sure you want block: " + hostToBlock,
						"Block Host", JOptionPane.YES_NO_OPTION);
				if (choice == JOptionPane.YES_OPTION) {
					ProxyTrafficFilter.getInstance()
							.addBlockedHost(hostToBlock);
				}
			}
		});
		btnBlockHost.setBounds(12, 12, 155, 25);
		frmWebProxy.getContentPane().add(btnBlockHost);

		JButton btnBlockip = new JButton("Block IP");
		btnBlockip.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String ipToBlock = infoField.getText();
				infoField.setText("");
				int choice = JOptionPane.showConfirmDialog(null,
						"Are you sure you want block: " + ipToBlock,
						"Block IP", JOptionPane.YES_NO_OPTION);
				if (choice == JOptionPane.YES_OPTION) {
					ProxyTrafficFilter.getInstance()
							.addBlockedIP(ipToBlock);
				}
			}
		});
		btnBlockip.setBounds(12, 49, 155, 25);
		frmWebProxy.getContentPane().add(btnBlockip);

		JButton btnBlockPhrase = new JButton("Block phrase");
		btnBlockPhrase.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String phraseToBlock = infoField.getText();
				phraseToBlock.trim();
				infoField.setText("");
				int choice = JOptionPane.showConfirmDialog(null,
						"Are you sure you want block: " + phraseToBlock,
						"Block phrase", JOptionPane.YES_NO_OPTION);
				if (choice == JOptionPane.YES_OPTION) {
					ProxyTrafficFilter.getInstance()
							.addBlockedPhrase(phraseToBlock);
				}
			}
		});
		btnBlockPhrase.setBounds(12, 86, 155, 25);
		frmWebProxy.getContentPane().add(btnBlockPhrase);

		JButton btnUnblockHost = new JButton("Unblock Host");
		btnUnblockHost.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String hostToUnblock = infoField.getText();
				infoField.setText("");
				int choice = JOptionPane.showConfirmDialog(null,
						"Are you sure you want unblock: " + hostToUnblock,
						"Unlock Host", JOptionPane.YES_NO_OPTION);
				if (choice == JOptionPane.YES_OPTION) {
					if(ProxyTrafficFilter.getInstance()
							.removeBlockedHost(hostToUnblock)){
						txtInfoScreen.setText("Unblocked: " + hostToUnblock);
					}else{
						txtInfoScreen.setText("Unable to unblock: " + hostToUnblock);						
					}
				}
			}
		});
		btnUnblockHost.setBounds(179, 12, 155, 25);
		frmWebProxy.getContentPane().add(btnUnblockHost);

		JButton btnUnblockIp = new JButton("Unblock Ip");
		btnUnblockIp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String ipToUnblock = infoField.getText();
				infoField.setText("");
				int choice = JOptionPane.showConfirmDialog(null,
						"Are you sure you want unblock: " + ipToUnblock,
						"Unlock IP", JOptionPane.YES_NO_OPTION);
				if (choice == JOptionPane.YES_OPTION) {
					if(ProxyTrafficFilter.getInstance()
							.removeBlockedIP(ipToUnblock)){
						txtInfoScreen.setText("Unblocked: " + ipToUnblock);
					}else{
						txtInfoScreen.setText("Unable to unblock: " + ipToUnblock);						
					}
				}
			}
		});
		btnUnblockIp.setBounds(179, 49, 155, 25);
		frmWebProxy.getContentPane().add(btnUnblockIp);

		JButton btnUnblockPhrase = new JButton("Unblock phrase");
		btnUnblockPhrase.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String phraseToUnblock = infoField.getText();
				phraseToUnblock.trim();
				infoField.setText("");
				int choice = JOptionPane.showConfirmDialog(null,
						"Are you sure you want unblock: " + phraseToUnblock,
						"Unlock phrase", JOptionPane.YES_NO_OPTION);
				if (choice == JOptionPane.YES_OPTION) {
					if(ProxyTrafficFilter.getInstance()
							.removeBlockedPhrase(phraseToUnblock)){
						txtInfoScreen.setText("Unblocked: " + phraseToUnblock);
					}else{
						txtInfoScreen.setText("Unable to unblock: " + phraseToUnblock);						
					}
				}
			}
		});
		btnUnblockPhrase.setBounds(179, 86, 155, 25);
		frmWebProxy.getContentPane().add(btnUnblockPhrase);

		JButton btnListHost = new JButton("List");
		btnListHost.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String s ="";
				List<String> list = ProxyTrafficFilter.getInstance().getBlockedHostList();
				for(int i = 0; i < list.size();i++){
					s+=list.get(i)+"\n";
				}
				txtInfoScreen.setText(s);
			}
		});
		btnListHost.setBounds(346, 12, 155, 25);
		frmWebProxy.getContentPane().add(btnListHost);

		JButton btnListIP = new JButton("List");
		btnListIP.setBounds(346, 49, 155, 25);
		frmWebProxy.getContentPane().add(btnListIP);

		JButton btnListPhrase = new JButton("List");
		btnListPhrase.setBounds(346, 86, 155, 25);
		frmWebProxy.getContentPane().add(btnListPhrase);

		txtInfoScreen = new JTextField();
		txtInfoScreen.setEditable(false);
		txtInfoScreen.setBounds(513, 15, 273, 325);
		frmWebProxy.getContentPane().add(txtInfoScreen);
		txtInfoScreen.setColumns(10);

		infoField = new JTextField();
		infoField.setBounds(12, 123, 489, 32);
		frmWebProxy.getContentPane().add(infoField);
		infoField.setColumns(10);

	}
}
