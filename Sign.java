import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;

import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;


// Written by Anirban
class Sign {
    private String file;
    private String cert;
    private HashMap<File, ArrayList<File>> listofsignatures;

    // SIMPLY STORES THE ABSOLUTE PATH TO THE FILE AND THE CERTIFICATE
    public Sign(String filename,String Certificatename)
    {
        listofsignatures=new HashMap<File, ArrayList<File>>();

        this.file=filename;
        this.cert=Certificatename;

    }
      
    //PICKS UP THE CERTIFICATE AND PULLS OUT ITS SIGNATURE AND RETURNS IT
    public byte[] getSingature( String certName) {
        byte []signature=null;
        File certFile = new File( certName);

        FileInputStream sigfis;

        InputStream inStream = null;
        PublicKey pubKey = null;

        try {
            inStream = new FileInputStream(certFile);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            //X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);
            X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);
            pubKey = cert.getPublicKey();
            signature=cert.getSignature();

        } 
        catch(Exception e)
        {
            e.printStackTrace();

        }
        finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return signature;
    }

    // COMPARES THE SIGNATURE SENT BY THE CLIENT TO THE SIGNATURE IT HAS JUST PULLED OUT FROM THE CERTIFCATE THAT THE CLIENT IS USING TO VOUCH
    public boolean checkCertusingSignature(byte[] sigToVerify ,String certName) {
        boolean fileVerified = false;

        File certFile = new File( certName);

        FileInputStream sigfis;

        InputStream inStream = null;
        PublicKey pubKey = null;

        try {
            inStream = new FileInputStream(certFile);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);
            pubKey = cert.getPublicKey();

            Signature sig = Signature.getInstance("SHA1withRSA");
            sig.initVerify(pubKey);
            fileVerified = sig.verify(sigToVerify);

        }
        catch(Exception e)
        {
            e.printStackTrace();

        }
        finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("WHETHER FILE COULD BE VOUCHED    "+ fileVerified);
        return fileVerified;
    }


    // VOUCHES FOR THE FILE USING THE CERTIFICATE PROVIDED

    public boolean vouchFileUsingCert(String fileName, String certName){
        boolean successfulAssociation = false;

        File file = new File(fileName);
        File cert = new File(certName);

        FileExaminer fileEx2 = new FileExaminer(fileName, '/', '.');
        FileExaminer fileEx = new FileExaminer(certName, '/', '.');

        if (fileName.equals(certName)) {
            System.out.println("NOT POSSIBLE!!!! Client tried to verify itself....");
            return successfulAssociation;
        }

        if(fileEx.extension().equals("cert")){
            if(!this.listofsignatures.containsKey(fileName)){

                ArrayList<File> list1 = new ArrayList<File>();

                list1.add(cert);
                this.listofsignatures.put(file, list1);
                successfulAssociation = true;

            }
            if(this.listofsignatures.containsKey(fileName) && !this.listofsignatures.get(fileName).contains(certName)){

                this.listofsignatures.get(file).add(cert);
                successfulAssociation = true;
            }
        } else{
            System.out.println("Error! Tried to associate file with non-certificate file. Vouching operation cancelled");
        }
        return successfulAssociation;				
    }  

    // CHECKS IF THE FILE ACTUALLY EXISTS OR NOT
    public boolean fileExists(String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            return true;
        } else {
            return false;
        }
    }


}

/* taken from the following
 * http://stackoverflow.com/questions/3481828/how-to-split-a-string-in-java
 * 
 */
class FileExaminer {
    private String fullPath;
    private char pathSeparator, extensionSeparator;

    public FileExaminer(String str, char sep, char ext) {
        fullPath = str;
        pathSeparator = sep;
        extensionSeparator = ext;
    }

    public String extension() {
        int dot = fullPath.lastIndexOf(extensionSeparator);
        return fullPath.substring(dot + 1);
    }

    public String filename() { // gets filename without extension
        int dot = fullPath.lastIndexOf(extensionSeparator);
        if (dot == -1) {
            dot = fullPath.length();
        }
        int sep = fullPath.lastIndexOf(pathSeparator);
        return fullPath.substring(sep + 1, dot);
    }

    public String path() {
        int sep = fullPath.lastIndexOf(pathSeparator);
        return fullPath.substring(0, sep);
    }

}
