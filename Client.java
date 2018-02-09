
/*
CLIENT- A client side java program to implement basic features of File Transfer Protocol .
Author: Abhijith SatheeshKumar
Date:11/2/17
 */

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Client {

	private static int clientPort;
	private static String serverName;
	BufferedReader in;
	DataInputStream datain;
	DataOutputStream dataout;
	private  String rootDirectory="user.dir";

	/*Declaration of main()
	 *arguments- portNumber, ipaddress
	 */
	public static void main(String[] args) throws IOException {
		Client clientObject = new Client();

		if (args.length != 2) {
			System.out.println("Please provide the valid port number and IP address as arguments to the program");
			System.exit(0);
		}

		if (clientObject.establishConnection(args[0], args[1])) {
			System.out.println("Client is connected to the Server.");
			System.out.println("---------------------------------------------------------------");
			clientObject.clientActionMenu();
		} else {
			System.out.println("Failed to establish connection between Client and Server.");
			System.exit(0);
		}
	}

	/*
	 * establishConnection()-Method to set up connection with server
	 * arguments - portNumber, Ip address
	 * return values: nil
	 */
	public boolean establishConnection(String portNumber, String address) {
		boolean flag = false;
		try {
			clientPort = Integer.parseInt(portNumber);
			serverName = address;
			InetAddress serverIP = InetAddress.getByName(serverName);
			Socket clientSocket = new Socket(serverIP, clientPort);
			System.out.println("Client started and is waiting for server to accept connection.");
			System.out.println("---------------------------------------------------------------");
			in = new BufferedReader(new InputStreamReader(System.in));
			datain = new DataInputStream(clientSocket.getInputStream());
			dataout = new DataOutputStream(clientSocket.getOutputStream());
			flag = true;
		} catch (Exception e) {
			e.printStackTrace();
			flag = false;
		}
		return flag;
	}
	
/*
* clientActionMenu()-Method to invoke client actions or commands
* arguments - nil
* return values: nil
* */
	public void clientActionMenu() throws IOException {
		boolean quit = true;
		System.out.println("\nPlease select an action from the FTP action Menu as given below:");
		while (quit) {
			System.out.println("-------------------------------------------------");
			System.out.println("\nMENU" );
			System.out.println("----");
			System.out.println("1.Send a file to Server \n" + "2.Download a file from Server\n"
				+ "3.List the files on Server directory\n" + "4.View present working directory of Server\n"
				+ "5.Delete a file on Server directory\n" + "6.List files locally on client side\n"
				+ "7.Change the directory\n" + "8.Exit\n");
			Scanner scanner = new Scanner(System.in);
			int choice = scanner.nextInt();
			switch (choice) {
			case 1:
				dataout.writeUTF("upload");
				uploadFile();
				break;

			case 2:
				dataout.writeUTF("download");
				downloadFile();
				break;

			case 3:
				dataout.writeUTF("listFilesServerSide");
				listFilesServerSide();
				break;

			case 4:
				dataout.writeUTF("pwd");
				presentWorkingDir();
				break;

			case 5:
				dataout.writeUTF("delete");
				deleteFile();
				break;

			case 6:
				ListFilesLocally();
				break;

			case 7:
				dataout.writeUTF("changeDirectory");
				changeDirectory();
				break;

			case 8:
				System.out.println("Connection closed and shutting down FTP application.");
				dataout.writeUTF("exit");
				quit = false;
				break;
			}
		}
	}

	/*
	 * changeDirectory()-Method to implement client action to change directory.
	 * arguments - nil
	 * return values: nil
	 * */
	private void changeDirectory() {
		String dirPath = null;
		System.out.println("Enter '*' to go back the directory path OR Enter new directory name to change to that directory");
		try {
			dirPath = in.readLine();
			dataout.writeUTF(dirPath);
			String previousDirectory = datain.readUTF();
			String currentDirectory=datain.readUTF();

			System.out.println("Directory is changed to: "+currentDirectory);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Exception in change directory method.");
		}
	}
	
	/*
	 *deleteFile()- Method to implement client action to change directory.
	 * arguments - nil
	 * return values: nil
	 * */
	private void deleteFile() {
		StringBuffer deletedStatus = new StringBuffer("deleted");
		StringBuffer NotFoundStatus = new StringBuffer("Not Found");
		System.out.print("Enter the file name to the deleted:");
		try {
			String fileName = in.readLine();
			dataout.writeUTF(fileName);
			String status = datain.readUTF();
			if (status.contentEquals(deletedStatus)) {
				System.out.println("File is deleted successfully.");
			} else if (status.contentEquals(NotFoundStatus))
				System.out.println("File is not found on server's working directory.");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Exception occured in deleteFile method. ");
		}
	}

	/*
	 *ListFilesLocally()- Method to implement client action to list files on the clients directory.
	 * arguments - nil
	 * return: void
	 * */
	private void ListFilesLocally() throws IOException {
		String dirName = System.getProperty(rootDirectory);
		System.out.println(dirName);
		List<Path> fileListName = new ArrayList<>();
		Path presentWorkingDir = Paths.get(dirName);

		try {
			DirectoryStream<Path> stream = Files.newDirectoryStream(presentWorkingDir, path -> path.toFile().isFile());
			for (Path entry : stream) {
				fileListName.add(entry);
			}
			stream.close();
		} catch (DirectoryIteratorException e) {
			throw e.getCause();
		} catch (IOException e) {
			System.out.println("Exception occured in ListFilesLocally method. ");
			e.printStackTrace();
		}
		String[] list = new String[fileListName.size()];
		int i = 0;
		System.out.println("list of files present locally on client side working directory is: ");
		for (Path entry : fileListName) {
			list[i] = String.valueOf(entry.getFileName());
			System.out.println(list[i]);
			i++;
		}
	}

	/*
	 *presentWorkingDir()- Method to implement client action to view current working Directory.
	 * arguments - nil
	 * return: void
	 * */
	private void presentWorkingDir() {
		String dirName = null;
		try {
			dirName = datain.readUTF();
		} catch (IOException e) {
			System.out.println("Exception occured in presentWorkingDir method. ");
			e.printStackTrace();
		}
		System.out.println("Present Working Directory is: " + dirName);

	}
	
	/*
	 *listFilesServerSide()- Method to implement client action to list the files in the servers current Directory.
	 * arguments - nil
	 * return: void
	 * */
	private void listFilesServerSide() throws IOException {
		try {
			String dirName = datain.readUTF();
			String length = datain.readUTF();
//			System.out.println("dirNma" + dirName);
//			System.out.println("length is " + length);
			String[] listOfFiles = new String[Integer.parseInt(length)];
			System.out.println("\nList of files in the directory " + dirName + " is: ");
			for (int i = 0; i < listOfFiles.length; i++) {
				listOfFiles[i] = datain.readUTF();
				System.out.println(listOfFiles[i]);
			}
		} catch (Exception e) {
			System.out.println("Exception occured in listFilesServerSide method. ");
			e.printStackTrace();
		}
	}

	/*
	 *uploadFile()- Method to implement client action to put a file on the server directory.
	 * arguments - nil
	 * return: void
	 * */
	public void uploadFile() throws IOException {
		String fileName;
		System.out.print("Enter the filename to be uploaded: ");
		fileName = in.readLine();
		File uploadFile = new File(fileName);
		boolean fileExists = true;
		try {

			FileInputStream fis = new FileInputStream(uploadFile);
			if(uploadFile.length()>0) {
			dataout.writeUTF("Found");
			System.out.println(fileName + " is found and is not empty.");
			}
			else {
				fileExists=false;
				dataout.writeUTF("Empty");
				System.out.println("File is empty and prevented upload operation.");
				
			}
		} catch (Exception e) {
			fileExists = false;
			dataout.writeUTF("Not Found");
			System.out.println("The file " + fileName + " is not present in the current working directory.");
		}

		if (fileExists) {
			
			System.out.println("FILE UPLOADING>...");
			dataout.writeUTF(fileName);
			String dirName = datain.readUTF();
			byte byteParse[] = new byte[1000];
			int byteRead;
			FileInputStream fis = new FileInputStream(uploadFile);
			int length = (int) uploadFile.length();
			//System.out.println("length: " + length);
			dataout.writeUTF(String.valueOf(length));

			do {
				byteRead = fis.read(byteParse);
				//System.out.println(byteRead);
				if (byteRead != -1) {
					dataout.write(byteParse, 0, byteRead);
				}
			} while (byteRead != -1);
			
			System.out.println("File "+fileName+" uploaded successfully");
			fis.close();
			}
	}

	/*
	 *downloadFile()- Method to implement client action to get a file on the server directory.
	 * arguments - nil
	 * return: void
	 * */
	public void downloadFile() throws IOException {
		String fileName;
		String status = null;
		StringBuffer serverStatusFound = new StringBuffer("Found");
		StringBuffer serverStatusNotFound = new StringBuffer("Not Found");
		StringBuffer serverStatusEmpty = new StringBuffer("Empty");
		System.out.print("Enter the filename to be downloaded: ");

		try {

			fileName = in.readLine();
			System.out.print("File name is: " + fileName + "\n");
			dataout.writeUTF(fileName);
			status = datain.readUTF();
			System.out.print("Status from server is: " + status + "\n");
			if (status.contentEquals(serverStatusFound)) {
				System.out.println("FILE DOWNLOADING>...");
				File recieveFile = new File(fileName);
				FileOutputStream fout = new FileOutputStream(recieveFile);

				if (recieveFile.exists()) {
					int byteRead = 0;
					int dataLength = 0;
					byte byteParse[] = new byte[1000];
					int length = Integer.parseInt(datain.readUTF());
					do {
						byteRead = datain.read(byteParse);
						dataLength += byteRead;
						if (dataLength == length) {
							break;
						}
						if (byteRead != -1) {
							fout.write(byteParse, 0, byteRead);
						}
					} while (byteRead != -1);
					System.out.println("File successfully downloaded.");
					fout.close();
				}
			} else if (status.contentEquals(serverStatusNotFound)) {
				System.out.println("Requested file: " + fileName + "is not found on the server side");
			}
			else if(status.contentEquals(serverStatusEmpty)) {
				System.out.println("Requested file: " +fileName+ " is empty and so prevented download operation");
			}
		} catch (Exception e) {
			System.out.println("Exception occured in dowloadFile method. ");
			e.printStackTrace();
		}
	}
}
