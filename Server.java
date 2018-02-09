/*
 * SERVER: A server side java program to handle the basic FTP actions taken by the clients File Transfer Requests.
 * Author: Abhijith SatheeshKumar	
 * Date:11/2/2017
 */
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;


public class Server {
    private static int serverPort;
    DataInputStream datain;
    DataOutputStream dataout;
    private  String rootDirectory="user.dir";

/*Method main()
 * arguments-portno
 * return- nill
 */
    public static void main(String[] args) {

        try {
            if (args.length != 1) {
                System.out.println("Please provide the valid port number to the program and IP address.");
                System.exit(0);
            }
            Server serverObject = new Server();
            if (serverObject.acceptConnection(args[0])) {
                System.out.println("Server is connected to the client");
                serverObject.serverActionMenu();
            } else {
                System.out.println("Failed to establish connection between Client and Server.");
                System.exit(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /*
     * acceptConnection()-Method to establish connection with client
	 * arguments - portNumber
	 * return values: nil
     */

    public boolean acceptConnection(String serverPortNumber) {
        boolean flag = false;
        try {
            serverPort = Integer.parseInt(serverPortNumber);
            ServerSocket serverSocket = new ServerSocket(serverPort);
            System.out.println("Server started and is waiting for client to join.");
            System.out.println("-------------------------------------------------");
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connection is accepted.");
            System.out.println("-------------------------------------------------");
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
     * serverActionMenu()-Method to carry out client commands
	 * arguments - nil
	 * return values: nil
     */

    public void serverActionMenu() throws IOException {
        boolean quit = true;
        String clientCommand = null;
        while (quit) {
        	System.out.println("\n----------------------------------------------------");
            //System.out.println("List options");
            clientCommand = datain.readUTF();
            switch (clientCommand) {

                case "upload":
                    uploadFile();
                    break;

                case "download":
                    //System.out.println("Download called");
                    downloadFile();
                    break;

                case "listFilesServerSide":
                    listFilesServerSide();
                    break;

                case "pwd":
                    presentWorkingDir();
                    break;

                case "delete":
                    deleteFile();
                    break;

                case "changeDirectory":
                    changeDirectory();
                    break;

                case "exit":
                    quit = false;
                    break;
            }
        }
    }

    /*
     * changeDirectory()-Method to change server directory
	 * arguments - nil
	 * return values: nil
     */
    private void changeDirectory() {

        String dirName = null;
        StringBuffer previousDir= new StringBuffer("*");
        String currentDirectory;

        try {
              dirName = datain.readUTF();
            currentDirectory=presentWorkingDir();

            if (dirName.contentEquals(previousDir)){
                //System.out.println("Current Directory:"+currentDirectory);
                String  temp[] = currentDirectory.split("\\\\");
                int newLength=temp.length-1;
                String temp2[] = new String[newLength];
                for (int i=0;i<newLength;i++){
                    temp2[i]=temp[i];
                }
                StringBuilder strBuilder = new StringBuilder();
                for(String value : temp2) {
                    strBuilder.append(value);
                    if (newLength>1)
                        strBuilder.append(File.separator);
                    newLength--;
                }
                String newPath = strBuilder.toString();

                System.out.println("Current Working directory is changed to:  " +newPath);
                System.setProperty(rootDirectory, String.valueOf(Paths.get(newPath)));
                dataout.writeUTF(newPath);



            }
            else{
                String newDirectory= currentDirectory+File.separator+dirName;
                System.out.println(newDirectory);
                Files.createDirectories(Paths.get(newDirectory));
                System.setProperty(rootDirectory,newDirectory);
                dataout.writeUTF(newDirectory);

            }

        } catch (IOException e) {
            System.out.println("Exception occurred in change directory method");
            e.printStackTrace();
        }

    }
    
    /*
     * deleteFile()-Method to delete a file in server directory
	 * arguments - nil
	 * return values: nil
     */

    private void deleteFile() {

        String fileName = null;
        try {
            fileName = datain.readUTF();
            File deleteFile = new File(fileName);
            Path path = FileSystems.getDefault().getPath(System.getProperty(rootDirectory), String.valueOf(deleteFile));
            if (Files.deleteIfExists(path)) {
                dataout.writeUTF("deleted");
                System.out.println("File "+fileName+" deleted successfully.");
            } else {
                dataout.writeUTF("Not Found");
                System.out.println("File to be delete "+fileName+" is not found on the current directory.");
            }
        } catch (IOException e) {
            System.out.println("Exception occurred in deleteFile method");
            e.printStackTrace();
        }
    }
    /*
     * presentWorkingDir()-Method to print the current working server directory
	 * arguments - nil
	 * return values: dirName
     */

    private String presentWorkingDir() throws IOException {

        String dirName = null;
        try {
            dirName = System.getProperty(rootDirectory);
            dataout.writeUTF(dirName);
        } catch (IOException e) {
            System.out.println("Exception occurred in present working directory method");
            e.printStackTrace();
        }
        System.out.println("Present working directory of server is send to client.");
        return dirName;
    }
    
    /*
     * listFilesServerSide()-Method to list files in the current working server directory
	 * arguments - nil
	 * return values: nil
     */

    public void listFilesServerSide() throws IOException {

        List<Path> fileListName = new ArrayList<>();
        String directoryName = presentWorkingDir();
        Path presentWorkingDir = Paths.get(directoryName);
        System.out.println(presentWorkingDir);
        try {
                DirectoryStream<Path> stream = Files.newDirectoryStream(presentWorkingDir);

            for (Path entry : stream) {
                fileListName.add(entry);
            }
            stream.close();
        } catch (DirectoryIteratorException e) {
            System.out.println("Exception occurred in listFilesServerSide method");
            throw e.getCause();
        }
        String[] list = new String[fileListName.size()];
        int i = 0;
        dataout.writeUTF(String.valueOf(list.length));

        for (Path entry : fileListName) {
            list[i] = String.valueOf(entry.getFileName());
            dataout.writeUTF(list[i]);
            i++;
        }
        System.out.println("List of files on server directory is send to client.");
    }

    /*
     * uploadFile()-Method to accept a file uploaded by the client and store in server directory
     * arguments - nil
	 * return values: nil
     */
    public void uploadFile() throws IOException {
        StringBuffer clientStatusFound = new StringBuffer("Found");
        StringBuffer clientStatusEmpty = new StringBuffer("Empty");
        StringBuffer clientStatusNotFound = new StringBuffer("Not Found");

        String status = datain.readUTF();

        try {
            if (status.contentEquals(clientStatusFound)) {
                String fileName = datain.readUTF();
                System.out.print("Uploading filename is: " + fileName + "\n");
                File recieveFile = new File(presentWorkingDir()+File.separator+fileName);
                FileOutputStream fout = new FileOutputStream(recieveFile);
                if (recieveFile.exists()) {

                    byte byteParse[] = new byte[1000];
                    System.out.println("FILE UPLOADING>...");
                    int byteRead;
                    int dataLength=0;
                    int length = Integer.parseInt(datain.readUTF());
                    do {
                        byteRead = datain.read(byteParse);
                        dataLength +=byteRead;
                        //System.out.println(byteRead);
                        if(dataLength == length) {
                        	break;
                        }
                        if (byteRead != -1) {
                            fout.write(byteParse, 0, byteRead);
                        }
                    } while (byteRead != -1);
                    fout.close();
                    System.out.println("File uploaded successfully.");
                }
            } else if (status.contentEquals(clientStatusNotFound)) {
                System.out.println("The file is not found on the client side directory.");
            }
            else if(status.contentEquals(clientStatusEmpty)) {
            	System.out.println("The file is empty and prevented upload operation.");
            }

        } catch (Exception e) {
            System.out.println("Exception occurred in uploadFile method");
            e.printStackTrace();
        }

    }
    /*
     * uploadFile()-Method to send a file requested by the client from server directory
     * arguments - nil
	 * return values: nil
     */

    public void downloadFile() throws IOException {

        String fileName;
        fileName = datain.readUTF();
        File downloadFile = new File(fileName);
        //System.out.println(fileName);
        boolean fileExists = true;
        try {

            FileInputStream fis = new FileInputStream(downloadFile);
            if(downloadFile.length() > 0) {
            dataout.writeUTF("Found");
            System.out.println(fileName+" is found and is not empty.");
            }
            else {
            	fileExists=false;
            	dataout.writeUTF("Empty");
            	System.out.println("File is empty and prevented download operation.");
            }
            System.out.println(fileName + " is found.");
        } catch (Exception e) {
            fileExists = false;
        	dataout.writeUTF("Not Found");
            System.out.println("The file " + fileName + " is not present in the current working directory.");
        }
        if (fileExists) {
            System.out.println("FILE DOWNLOADING>...");
            byte byteParse[] = new byte[1000];
            int byteRead;
            FileInputStream fis = new FileInputStream(downloadFile);
            int length=(int)downloadFile.length();
            dataout.writeUTF(String.valueOf(length));
            do {
                byteRead = fis.read(byteParse);
                if (byteRead != -1) {
                    dataout.write(byteParse, 0, byteRead);
                }
            } while (byteRead != -1);
            fis.close();
            System.out.println("File downloaded successfully");
        } 
    }
}
