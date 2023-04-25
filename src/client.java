import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class client {
    
    //-------------------------------------------------- Methods to Check Arguments --------------------------------------------------

    /**
     * Check that the command is valid and that the correct amount of arguments was provided 
     * note that file paths are not validated here 
     * @param args arguements from main 
     * @return true if the command is valid and the number of arguments is correct, otherwise return false
     */
    private static boolean checkCommand(String[] args) {

        // check that a command was provided 
        if (args.length == 0){
            System.err.println("client: no command was given!");
            return false;
        }

        // check that the command is supported and the correct number of arguments was provided 
        if (args[0].equals("shutdown")){
            if (args.length > 1){
                System.err.println("client: the \"shutdown\" command does not take any arguments!");
                return false;
            }
        }
        else if (args[0].equals("dir")){ 
            if (args.length > 2){
                System.err.println("client: the \"dir\" command takes a maximum of 1 argument!");
                return false;
            }
        }
        else if (args[0].equals("mkdir") || args[0].equals("rmdir") || args[0].equals("rm")){
            if (args.length != 2){
                System.err.println("client: the \"" + args[0]  +"\" command takes exactly 1 argument!");
                return false;
            }
        }
        else if (args[0].equals("upload") || args[0].equals("download")){
            if (args.length != 3){
                System.err.println("client: the \"" + args[0] + "\" command takes exactly 2 argument!");
                return false;
            }
        }
        else{ // all other commands are not supported 
            System.err.println("client: command not supported!");
            return false;
        }

        return true;
    }

    /**
     * Alter paths that start at the root directory (/) to start from the current working directory (./) instead
     * @param path path to be altered 
     * @return path that starts in the current working directory
     */
    private static String sanitizePath(String path){
        if (path.charAt(0) == File.separatorChar || path.charAt(0) == '/'){
            path = "." + path;
        }
        return path;
    }
    //-------------------------------------------------- Client Methods & Variables --------------------------------------------------
    private FileSharingInterface FSI;
    
    public client(String host, String port){
        try {
            this.FSI = (FileSharingInterface) Naming.lookup("rmi://localhost:8000" + "/file-sharing");
        } catch (MalformedURLException | RemoteException | NotBoundException e) {
            System.err.println("client: cannot connect to server (" + e + ")");
            System.exit(1);
        }
    }

    private boolean shutdownServer() {
        try {
            this.FSI.shutdown();
            System.out.println("shutdown: server has shutdown");
        } catch (RemoteException e) {
            System.err.println("client: error shutting down server (" + e + ")");
        }
        return true;
    }

    /**
     * Asks server for the content of a directory 
     * @param path server's filepath to the directory 
     * @return true if successful, false otherwise
     */
    public boolean dir(String path){
        String[] contents = null;

        try {
            contents = this.FSI.dir(path);
        } catch (RemoteException e) {
            System.err.println("client: dir error (" + e + ")");
        }

        // contents will be null if directory does not exists 
        if (contents == null){
            System.err.println("dir: no such directory");
            return false;
        }
        else{ 
            if (contents.length == 0){
                System.out.println("<Empty Directory>");
            }
            for (int i=0; i<contents.length; i++){
                System.out.println(contents[i]);
            }
            return true;
        }
    }

    /**
     * Ask the server to make a directory
     * @param path server's filepath to the new directory
     * @return true if successful, false otherwise
     */
    public boolean mkdir(String path){
        boolean OK = false;
        try{
            OK = this.FSI.mkdir(path);
            if (OK){
                System.out.println("mkdir: new directory created at " + path);
            }
            else{
                System.err.println("mkdir: " + path + " is an invalid path or directory already exists");
            }
        } catch (RemoteException e) {
            System.err.println("client: mkdir error (" + e + ")");
        }
        return OK;
    }

    /**
     * Ask the server to remove a directory
     * @param path server's filepath 
     * @return true if successful, false otherwise
     */
    public boolean rmdir(String path){
        boolean OK = false;
        try{
            OK = this.FSI.rmdir(path);
            if (OK){
                System.out.println("rmdir: " + path + " is removed");
            }
            else{
                System.err.println("rmdir: " + path + " is an invalid path, not a directory, or is not empty");
            }
        } catch (RemoteException e) {
            System.err.println("client: rmdir error (" + e + ")");
        }
        return OK;
    }

    /**
     * Ask the server to remove a file
     * @param path server's filepath 
     * @return true if successful, false otherwise
     */
    public boolean rm(String path){
        boolean OK = false;
        try{
            OK = this.FSI.rm(path);
            if (OK){
                System.out.println("rm: " + path + " is removed");
            }
            else{
                System.err.println("rm: " + path + " is an invalid path or not a file");
            }
        } catch (RemoteException e) {
            System.err.println("client: rm error (" + e + ")");
        }
        return OK;
    }

    /**
     * Upload a file to the server 
     * @param clientPath path to the client file 
     * @param serverPath path to the file server
     * @return true if successful, false otherwise
     */
    public boolean upload(String clientPath, String serverPath){
        Boolean OK = false;

        try {

            // check that the client's filepath is valid 
            File clientFile = new File(clientPath);
            if (!clientFile.isFile()){ 
                System.err.println("upload: client path " + clientPath + " is invalid or not a file");
            }

            // check that the server's filepath is valid 
            else if (!this.FSI.filepathValid(serverPath)){
                System.err.println("upload: server path " + serverPath + " is invalid");
            }

            else{
            
                FileInputStream fileInputStream = new FileInputStream(clientFile);

                int bytes = 0; // number of bytes that was read from the file 
                byte[] buffer = new byte[1024]; // buffer to hold the bytes that was read

                // skip the bytes the server already has
                long bytesUploaded = this.FSI.getFileLength(serverPath); // the number of bytes the server has recieved 
                if (bytesUploaded > 0 && bytesUploaded < clientFile.length()){
                    fileInputStream.skip(bytesUploaded); // skip the bytes the server already has
                    System.out.println("upload: resuming upload");
                }

                // overwrite the file if the length is equal or greater 
                else if (bytesUploaded >= clientFile.length()){ 
                   this.FSI.rm(serverPath);
                   bytesUploaded = 0;
                }

                System.out.println("upload: " + Long.toString(bytesUploaded) + " / " + Long.toString(clientFile.length())); // print the progress
                
                bytes = fileInputStream.read(buffer); 
                while (bytesUploaded != clientFile.length()){
                    if (this.FSI.write(serverPath, buffer, bytes)){
                        bytesUploaded += bytes;
                        System.out.println("upload: " + Long.toString(bytesUploaded) + " / " + Long.toString(clientFile.length())); // print the progress
                        bytes = fileInputStream.read(buffer);
                    }
                }

                fileInputStream.close();
                OK = true;
            }
        } catch (IOException e) {
            System.err.println("client: upload error (" + e + ")");
        } 
        return OK;
    }

    private boolean download(String serverPath, String clientPath) {
        Boolean Ok = false;

        try{

            // check that the client's filepath is valid 
            File clientFile = new File(clientPath);
            if (!((clientFile.getParentFile() == null || clientFile.getParentFile().isDirectory()) && !clientFile.isDirectory())){
                System.err.println("download: client path " + clientPath + " is invalid");
            }

            // check that the server's filepath is valid 
            else if (!this.FSI.fileExists(serverPath)) {
                System.err.println("download: server path " + serverPath + " is invalid or not a file");
            }

            else{
                FileOutputStream fileOutputStream;

                long fileSize = this.FSI.getFileLength(serverPath); 
                long bytesDownloaded = clientFile.length();
            
                // check if we need to resume download 
                if (clientFile.exists() && clientFile.length() < fileSize){
                    fileOutputStream = new FileOutputStream(clientFile, true);
                    System.out.println("download: resuming download");
                }
                else{
                    bytesDownloaded = 0;
                    fileOutputStream = new FileOutputStream(clientFile);
                }

                System.out.println("download: " + Long.toString(bytesDownloaded) + " / " + Long.toString(fileSize));

                while(bytesDownloaded != fileSize){
                    byte[] buffer = this.FSI.read(serverPath, bytesDownloaded);
                    fileOutputStream.write(buffer);
                    bytesDownloaded += buffer.length;
                    System.out.println("download: " + Long.toString(bytesDownloaded) + " / " + Long.toString(fileSize));
                }
    
                fileOutputStream.close();
            }
        }
        catch (IOException e) {
            System.err.println("client: download error (" + e + ")");
        } 

        return Ok;
    }

    //-------------------------------------------------- Main Method --------------------------------------------------
    public static void main(String[] args) {

        // check that the command is valid and the correct number of arguments are provided
        if (!checkCommand(args)){
            System.exit(1);
        }

        // check that environment variable PA2_SERVER is set 
        if (System.getenv("PA2_SERVER") == null){
            System.err.println("client: need to export PA2_SERVER=<computername:portnumber>");
            System.exit(1);
        }

        // obtain the server's host & port # from PA1_SERVER
        String[] PA2_SERVER = System.getenv("PA2_SERVER").split(":"); 

        // setup the client 
        client myClient = new client(PA2_SERVER[0], PA2_SERVER[1]);

        // attempt to execute the command 
        boolean success = false;
        switch(args[0]){
            case "shutdown":
                success = myClient.shutdownServer();
                break;
            case "dir":
                success = (args.length == 1) ? myClient.dir(sanitizePath("/")) : myClient.dir(sanitizePath(args[1])); 
                break;
            case "mkdir":
                success = myClient.mkdir(sanitizePath(args[1]));
                break;
            case "rmdir":
                success = myClient.rmdir(sanitizePath(args[1]));
                break;
            case "rm":
                success = myClient.rm(sanitizePath(args[1]));
                break;
            case "upload":
                success = myClient.upload(sanitizePath(args[1]), sanitizePath(args[2]));
                break;
            case "download":
                success = myClient.download(sanitizePath(args[1]), sanitizePath(args[2]));
        }

        // check if the command was executed successfully 
        if (!success){
            System.exit(1);
        }
        else{
            System.exit(0);
        }
    }
}
