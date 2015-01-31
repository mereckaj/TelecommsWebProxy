package com.mereckaj.webproxy.gui;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class ProxyGUI {

	private JFrame frmWebProxy;
	final JFileChooser fc = new JFileChooser();
	private boolean liveFeed;
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
		liveFeed = true;
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
					liveFeed = false;
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
		mnFile.add(mntmClose);

		JMenu mnEdit = new JMenu("Edit");
		menuBar.add(mnEdit);

		JMenuItem mnConfig = new JMenuItem("Config");
		mnEdit.add(mnConfig);

		JMenu mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);

		JMenuItem mntmAbout = new JMenuItem("About");
		mntmAbout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFrame frmAbout = new JFrame("About HTTP Proxy");
				int X = frmWebProxy.getWidth()/2;
				int Y = frmWebProxy.getHeight()/2;
				frmAbout.setBounds(X,Y, 400, 200);
				frmAbout.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				frmAbout.setResizable(false);
				frmAbout.setVisible(true);
			}
		});
		mnHelp.add(mntmAbout);
	}

}
