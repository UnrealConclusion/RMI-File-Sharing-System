import java.io.*;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;

public class server extends UnicastRemoteObject implements FileSharingInterface{

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
        if (args[0].equals("start")){
            if (args.length != 2){
                System.err.println("server: the \"" + args[0]  +"\" command takes exactly 1 argument!");
                return false;
            }
        }
        else{
            System.err.println("server: command not supported!");
            return false;
        }
        return true;
    }

    //-------------------------------------------------- Server Methods & Variables --------------------------------------------------
    private String name;

    protected server(String name) throws RemoteException {
        super();
        this.name = name;
    }

    /**
     * Ask the server to shutdown
     * @return true if successful, false otherwise
     */
    @Override
    public void shutdown() throws RemoteException {
        try {
            UnicastRemoteObject.unexportObject(this, true);
            Naming.unbind(this.name);
            System.out.println("server: shutdown");
        } catch (MalformedURLException | NotBoundException e) {
            System.err.println("server: error shuting down + " + e);
        }
    }

    /***
     * List the contents of a directory
     * @param path server's file path 
     * @return string array of file names, 
     * an empty array will be returned if the directory contains no files,
     * null will be returned if the path is not valid 
     * @throws RemoteException remote communication exception 
     */
    @Override
    public String[] dir(String path) throws RemoteException {
        File directory = new File(path);
        return directory.list();
    }

    /**
     * Create a new directory 
     * @param path filepath to the new directory
     * @return true if operation succeeded, false otherwise 
     * @throws RemoteException remote communication exception 
     */
    @Override
    public boolean mkdir(String path) throws RemoteException {
        // try to make the directory
        File directory = new File(path);
        if (directory.mkdir()){ 
            return true;
        }
        else{
            return false;
        }
    }

    /**
     * Remove a directory
     * @param path filepath to the directory to be removed
     * @return true if operation succeeded, false otherwise 
     * @throws RemoteException remote communication exception
     */
    @Override
    public boolean rmdir(String path) throws RemoteException {
        // try to delete the directory
        File directory = new File(path);
        if (directory.isDirectory() && directory.delete()){
            return true;
        }
        else{
            return false;
        }    
    }

    /***
     * Remove a file
     * @param path filepath the the file to be removed
     * @return true if operation succeeded, false otherwise 
     * @throws RemoteException remote communication exception
     */
    @Override
    public boolean rm(String path) throws RemoteException {
        // try to delete the directory and let the client know whether the operation succeded or not
        File file = new File(path);
        if (file.isFile() && file.delete()){
            return true;
        }
        else{
            return false;
        }          
    }

    /**
     * Check if a filepath is valid 
     * @param path Server's filepath 
     * @return true if the path is valid, false otherwise 
     * @throws RemoteException remote communication exception
     */
    @Override
    public boolean filepathValid(String path) throws RemoteException {
        File file = new File(path);
        if ((file.getParentFile() == null || file.getParentFile().isDirectory()) && !file.isDirectory()){
            return true;
        }
        else{
            return false;
        }
    }

    /**
     * Check if a file exists
     * @param path Server's filepath 
     * @return true if the file exists, false otherwise 
     * @throws RemoteException
     */
    @Override
    public boolean fileExists(String path) throws RemoteException {
        File file = new File(path);
        return file.isFile();
    }

    /**
     * Get the length of a file on the server
     * @param path Server's filepath 
     * @return length of the file
     * @throws RemoteException remote communication exception
     */
    @Override
    public long getFileLength(String path) throws RemoteException {
        File file = new File(path);
        return file.length();
    }

    /**
     * Write bytes to a file
     * @param path filepath to the file 
     * @param buffer bytes to be written 
     * @param bytes the number of bytes to write
     * @return true if operation succeeded, false otherwise 
     * @throws RemoteException remote communication exception
     */
    @Override
    public boolean write(String path, byte[] buffer, int bytes) throws RemoteException {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(path, true);
            fileOutputStream.write(buffer,0, bytes);
            fileOutputStream.close();
            return true;
        } catch (IOException e) {
            System.err.println("server: error writing to file " + e);
            return false;
        } 
    }

    /**
     * Read bytes from a file
     * @param path filepath to the file
     * @param skip number of bytes to skip
     * @return bytes read from the file
     * @throws RemoteException remote communication exception
     */
    @Override
    public byte[] read(String path, long skip) throws RemoteException {
        try{
            FileInputStream fileInputStream = new FileInputStream(path);
            fileInputStream.skip(skip);

            // read up to 1024 bytes from the file
            byte[] buffer = new byte[1024];
            int bytes = fileInputStream.read(buffer);

            fileInputStream.close();
            return Arrays.copyOfRange(buffer, 0, bytes);
        } catch (IOException e) {
            System.err.println("server: error reading file " + e);
            return null;
        }
    }

    public static void main(String[] args) {

        // check that the command is valid and the correct number of arguments are provided
        if (!checkCommand(args)){
            System.exit(1);
        }
 
        // put together the URL
        String url = "rmi://localhost:" + args[1] + "/file-sharing";

        try {
            LocateRegistry.createRegistry(Integer.parseInt(args[1]));
            server server = new server(url);
            Naming.rebind(url, server);
            System.out.println("server is running at " + url);
        } catch (RemoteException e) {
            System.err.println("server: registry could not be contacted " + e);
        } catch (MalformedURLException e) {
            System.err.println("server: name is not an appropriately formatted URL " + e);
        }
    }
}
