import java.io.*;
import java.net.*; 
import java.io.File;
import java.nio.*;

import java.security.KeyStore;
import javax.net.*;
import javax.net.ssl.*;
import javax.security.cert.X509Certificate;
/**
 * 
 * The JavaServer server responds to the following commands from a client
 * -a filename  add or replace a file on the JavaServer server 
 * -c number    provide the required circumference (length) of a circle of trust
 * -f filename  fetch an existing file from the JavaServer server (simply sent to stdout)
 * -l   list all stored files and how they are protected
 * -n name  require a circle of trust to involve the named person (i.e. their certificate)
 * -u certificate   upload a certificate to the JavaServer server
 * -v filename certificate  vouch for the authenticity of an 
 *    existing file in the JavaServer server using the indicated certificate
 *    
 *Implements Runnable interface for threading
 *The Runnable interface allows an object to be run as a thread
 *It calls the run() method, which contains the code to run in the thread
 * 
 * 
 * @Cooper 
 * 
 * References:
 * http://docs.oracle.com/javase/1.5.0/docs
 * /guide/security/jsse/samples/sockets/server/ClassFileServer.java
 * 
 * http://docs.oracle.com/javase/1.5.0/docs/guide/security/jsse
 * /samples/sockets/server/ClassServer.java throughout
 * 
 * @version 2.00
 * 
 */
public class JavaServer implements Runnable
{
    // Global variables for pathnames and port number
    private static String docroot = "/Users/Cooper/Desktop/";   //where the server has its files
    private static String keystorepath = "/Users/Cooper/Desktop/testkeys/keystore.jks";
    private static char[] keystorepassword = "secretpassword".toCharArray();
    private static int defaultServerPort = 7777;

    private ServerSocket server = null;

    /**
     * Main method to create the class server that reads files. 
     * 
     */
    public static void main(String args[])
    {
        int port = defaultServerPort;        
        String type = "TLS";    // Transport Layer Security (SSL)

        try
        {
            // Create SSL socket
            ServerSocketFactory ssf = JavaServer.getServerSocketFactory(type);        
            ServerSocket ss = ssf.createServerSocket(port);            
            new JavaServer(ss);
        }
        catch (IOException e)
        {
            System.out.println("Unable to start JavaServer: " + e.getMessage() );
            e.printStackTrace();
        }
    }  

    
    /**
     * http://docs.oracle.com/javase/1.5.0/docs
     * /guide/security/jsse/samples/sockets/server/ClassFileServer.java
     * 
     * Required for threading
     * Constructs an instance of JavaServer with ServerSocket ss
     *
     */
    protected JavaServer(ServerSocket ss)
    {
        server = ss;
        newListener(); 
    }

    
    /**
     * This method parses the clients request by reading the first line of the data as a string
     * and then does the appropriate steps dependant on the header type
     * 
     * Returns null if something goes wrong
     */
    private String respondToClient( InputStream inputStream, PrintWriter out, DataOutputStream dataOut )
    throws IOException
    {
        BufferedReader in = new BufferedReader( new InputStreamReader( inputStream ) ); // for reading client input
        DataInputStream inFile = new DataInputStream( inputStream );   // provides other methods for reading client input
        String path = "";   // to hold filename

        // Format client's request
        String line = in.readLine();    // read request of client
        line = line.toLowerCase();
        String splitDelimiter = " ";
        String[] arguments = line.split( splitDelimiter );  // split client's request for parsing

        int numberOfArguments = arguments.length;
        for(int i = 0; i< numberOfArguments; i++)   // print arguments for server user to see
        {
            System.out.println("Argument " + i + ": " + arguments[i] );
        }

        // if client wants to add a file
        if( arguments[0].equals("-a") )     
        {
            System.out.println("Client is adding file: " + arguments[1]);
            File newFile = new File( docroot + arguments[1] );  // create the file to be written to just to check existence

            if ( newFile.createNewFile() == false)  // check if file exists already
            {
                System.out.println("File already exists");
                out.print("Overwriting existing file.");    // tells the client they are overwriting
            }

            try
            {                
                // Buffered saving of client's bytes to a new file
                // Next 7 lines based on online tutorial that can't be found
                FileOutputStream fos = new FileOutputStream(docroot + arguments[1]);  // creates the file
                byte[] buffer = new byte[1024];
                while ( inFile.read(buffer) > 0 )   // while there are bytes left to read from client
                {
                    fos.write( buffer );            // write those bytes to the file
                }
                fos.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return null;
            }
            System.out.println("File saved to server.");

            return null;
        }

        // if client wants a list of the files held by server
        else if( arguments[0].equals("-l") )    
        {
            File newFile = new File( docroot );
            newFile.createNewFile();                // allows accessing file methods at the directory 
            File[] fileList = newFile.listFiles();  // get file[] of files at the directory
            int fileListLength = fileList.length;

            System.out.println("Files on server:");
            for(int i = 0; i < fileListLength; i++) // iterate over all files in file[] and print their name
            {
                if( fileList[i] != null )
                {
                    out.print("file " + i + ": " + fileList[i].getName() + ", " );
                    System.out.println( "file " + i + ": " + fileList[i].getName() );
                }
            }
            return null;
        }

        // if client wants to fetch/download a file
        else if( arguments[0].equals("-f") )    
        {
            System.out.println("Client is fetching file: " + arguments[1]);
            path = arguments[1];

            try
            {
                byte[] bytecodes = getBytes(path);     // get the bytes of the file at the specified path
                dataOut.write(bytecodes);       // send them to the client
                dataOut.flush();   
                System.out.println("File sent to client.");
            } catch (IOException ie)
            {
                ie.printStackTrace();
                return null;
            }  

        }

        // if client wants to vouch for a file
        else if( arguments[0].equals("-v") )
        {
            String filename = arguments[1];
            String certificateName = arguments[2];
            byte[] signtoverify=null;

            doVouching( filename, certificateName, signtoverify );  // Call Anirban's code

        }

        else
        {
            System.out.println("invalid argument from client: invalid header");
            out.print("Invalid arguments to server");
            return null;
        }      

        return null;
    }

    
    /**
     * http://docs.oracle.com/javase/1.5.0/docs
     * /guide/security/jsse/samples/sockets/server/ClassFileServer.java
     * 
     * Create an instance of the thread class
     * Call its start() method which calls the run() method containing
     * the code we want to be executed in a thread.
     */
    private void newListener()
    {
        ( new Thread(this) ).start();
    }

    
    public void run()   // this is all the code executed when a thread is created
    {
        Socket socket;

        try
        {
            System.out.println("Accepting connections...");
            socket = server.accept();
        } catch (IOException e)
        {
            System.out.println( "Class Server not working: " + e.getMessage() );
            e.printStackTrace();
            return;
        }

        System.out.println("CONNECTED");        
        newListener();  // create a new thread to accept the next connection

        try
        {
            // Create input/output streams for receiving/sending to client
            PrintWriter out = new PrintWriter( new BufferedWriter( new OutputStreamWriter(socket.getOutputStream()) ) );
            DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream() );  
            InputStream inputStream = socket.getInputStream();

            try
            {                               
                // Respond to the client's request with method respondToClient()
                // pass the method streams for reading client input, and sending data/messages to the client
                String path = respondToClient( inputStream, out, dataOut ); 

                if (path == null)   // respondToClient() returns null if something went wrong
                {
                    // Potentially do something if not handled already
                }

            } catch (Exception e)
            {
                e.printStackTrace();
                //out.println("" + e.getMessage() + "\r\n");
                //out.flush();
            }

        } catch (IOException ex)
        {
            System.out.println( "error writing response: " + ex.getMessage() );
            ex.printStackTrace();

        }

        finally
        {
            try
            {
                socket.close();
            } catch (IOException e)
            {

            }
        }

    }

    /*
     * http://docs.oracle.com/javase/1.5.0/docs
     * /guide/security/jsse/samples/sockets/server/ClassFileServer.java
     * 
     * Returns a ServerSocketFactory which can be used to create a ServerSocket
     * @param type  specifies if ServerSocketFactory should use TLS or default
     */
    private static ServerSocketFactory getServerSocketFactory(String type) 
    {
        if (type.equals("TLS")) {
            SSLServerSocketFactory ssf = null;
            try {
                // set up key manager to do server authentication
                SSLContext ctx;
                KeyManagerFactory kmf;
                KeyStore ks;
                // char[] passphrase = "passphrase".toCharArray();

                ctx = SSLContext.getInstance("TLS");
                kmf = KeyManagerFactory.getInstance("SunX509");
                ks = KeyStore.getInstance("JKS");

                ks.load(new FileInputStream( keystorepath ), keystorepassword );
                kmf.init( ks, keystorepassword );
                ctx.init(kmf.getKeyManagers(), null, null);

                ssf = ctx.getServerSocketFactory();
                return ssf;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return ServerSocketFactory.getDefault();
        }
        return null;
    } 

    
    /**
     * http://docs.oracle.com/javase/1.5.0/docs
     * /guide/security/jsse/samples/sockets/server/ClassFileServer.java
     * 
     * Returns an array of bytes containing the bytes for
     * the file given by path
     *
     * @return the bytes for the file
     * @exception FileNotFoundException if the file could not be loaded.
     */    
    public static byte[] getBytes(String path)
    throws IOException
    {
        System.out.println("reading: " + path);
        File f = new File(docroot + File.separator + path);     // gets the specified file
        int length = (int)(f.length());
        if (length == 0) 
        {
            return null;
            // throw new IOException("File length is zero: " + path);
        } else 
        {
            FileInputStream fin = new FileInputStream(f);   // for reading from the file
            DataInputStream in = new DataInputStream(fin);  // for reading from the file
            byte[] bytecodes = new byte[length];    // create byte[] the same size as the file
            in.readFully(bytecodes);                // read the bytes from the file into the byte[] 
            return bytecodes;                       // return the byte[] now containing the file's bytes
        }
    }

    
    /*
     * Contents of this method found online
     */
    public static void doVouching( String filename, String certificateName, byte[] signtoverify )
    {
        Sign voucher=new Sign(filename, certificateName); // creates an instance of the Sign class

        if (voucher.checkCertusingSignature(signtoverify, certificateName)) // compares client cert to stored cert
        {
            if (voucher.fileExists(filename)) {
                if (voucher.fileExists(certificateName)) {
                    voucher.vouchFileUsingCert(filename, certificateName);
                } else {
                    System.out.println("Certificate " + certificateName + " does not exist on the server");
                }
            } else {
                System.out.println("File " + filename + " does not exist on the server");
            }
        } else {
            System.out.println("The digital signature doesn't match the certificate, aborting vouch.");
        }	
    }
}

