import java.rmi.Remote;
import java.rmi.RemoteException;

public interface FileSharingInterface extends Remote{

    /**
     * Shutdown the server
     * @return true if operation succeeded, false otherwise 
     * @throws RemoteException remote communication exception 
     */
    public void shutdown() throws RemoteException;

    /***
     * List the contents of a directory
     * @param path server's file path 
     * @return string array of file names, null if path is invalid 
     * @throws RemoteException remote communication exception 
     */
    public String[] dir(String path) throws RemoteException;

    /**
     * Create a new directory 
     * @param path filepath to the new directory
     * @return true if operation succeeded, false otherwise 
     * @throws RemoteException remote communication exception 
     */
    public boolean mkdir(String path) throws RemoteException;

    /**
     * Remove a directory
     * @param path filepath to the directory to be removed
     * @return true if operation succeeded, false otherwise 
     * @throws RemoteException remote communication exception
     */
    public boolean rmdir(String path) throws RemoteException;

    /***
     * Remove a file
     * @param path filepath to the file to be removed
     * @return true if operation succeeded, false otherwise 
     * @throws RemoteException remote communication exception
     */
    public boolean rm(String path) throws RemoteException;

    /**
     * Check if a filepath is valid 
     * @param path Server's filepath 
     * @return true if the path is valid, false otherwise 
     * @throws RemoteException remote communication exception
     */
    public boolean filepathValid(String path) throws RemoteException;

    /**
     * Check if a file exists
     * @param path Server's filepath 
     * @return true if the file exists, false otherwise 
     * @throws RemoteException
     */
    public boolean fileExists(String path) throws RemoteException;

    /**
     * Get the length of a file on the server
     * @param path Server's filepath 
     * @return length of the file
     * @throws RemoteException remote communication exception
     */
    public long getFileLength(String path) throws RemoteException;

    /**
     * Write bytes to a file
     * @param path filepath to the file 
     * @param buffer bytes to be written 
     * @param bytes the number of bytes to write
     * @return true if operation succeeded, false otherwise 
     * @throws RemoteException remote communication exception
     */
    public boolean write(String path, byte[] buffer, int bytes) throws RemoteException;


    /**
     * Read bytes from a file
     * @param path filepath to the file
     * @param skip number of bytes to skip
     * @return bytes read from the file
     * @throws RemoteException remote communication exception
     */
    public byte[] read(String path, long skip) throws RemoteException;
    
}
