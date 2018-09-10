/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package FTP;

import java.io.BufferedOutputStream;
import java.io.File;
import org.apache.commons.net.ftp.FTPClient;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.swing.JFileChooser;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.io.CopyStreamAdapter;

/**
 *
 * @author Rafa
 */
public class Controller {
    private JFMain vista;
    private FTPClient client;
    private DefaultMutableTreeNode rootServer, childServer, rootLocal;
    private DefaultTreeModel modeloServer, modeloLocal;
    private CopyStreamAdapter streamListener;
    private File fRuta;
    
    public Controller(JFMain vista){
        this.vista = vista;
    }
    
    /**
     * Método para conectarse al servidor.
     * @throws IOException 
     */
    public void connect() throws IOException{
        String server = vista.getServer();
        String user = vista.getUser();
        String pass = vista.getPass();
        int port = Integer.parseInt(vista.getPort());      
        
        client = new FTPClient();
        client.connect(server, port);
        // Comprobamos el código de respuesta al conectar y comprobamos que se haya podido conectar.
        int codRespuesta = client.getReplyCode();
        if (!FTPReply.isPositiveCompletion(codRespuesta)) {
            vista.falloConectar();
            return;
        }
        
        boolean login = client.login(user, pass);
        if (login) {
            vista.logMensaje();
               
            initServerDirsJTree(); 
        } else {
            vista.falloLog();
        }     
    }
    
    /**
     * Método para desconectarse del servidor. Deslogea al cliente y luego lo desconecta. Al final muestra un mensaje.
     * @throws IOException 
     */
    public void desLogear() throws IOException{
        boolean logout = client.logout();
        client.disconnect();
        if (logout) {
            vista.logoutMen(); 
        }
    }
    
    /**
     * Método que inicializa el Jtree a nulo.
     */
    public void initJTree() {
        // Recogemos en una variable el directorio que ha de listarServidor, que será el WorkingDirectory
        String dirToList = "";  
        // Lo añadimos al node de jTree
        rootLocal = new DefaultMutableTreeNode(dirToList);
        // Creamos el modeloServer del jTree con el nodo rootServer
        modeloLocal = new DefaultTreeModel(rootLocal);
        // le decimos el modeloServer que queremos al jTree
        vista.getjTreeServer().setModel(modeloLocal);
    }
    
    /**
     * Método que pinta en el jTree las carpetas del servidor.
     * También es llamado para repintar el jtree cuando se borran, actualizan o se añaden archivos.
     * @throws IOException 
     */
    public void initServerDirsJTree() throws IOException {
        // Recogemos en una variable el directorio que ha de listarServidor, que será el WorkingDirectory
            String dirToList = client.printWorkingDirectory();  
            // Lo añadimos al node de jTree
            rootServer = new DefaultMutableTreeNode(dirToList);
            // Creamos el modeloServer del jTree con el nodo rootServer
            modeloServer = new DefaultTreeModel(rootServer);
            // Llamamos al método para que nos liste los archivos, en caso de que haya más archivos llamará a otro submétodo
            // que volverá a listarServidor
            listarServidor(dirToList);
            // le decimos el modeloServer que queremos al jTree
            vista.getjTreeServer().setModel(modeloServer);
    }
    
    /**
     * Método que lista las carpetas y archivos del servidor y los añade al jtree
     * @param directorio 
     * @throws java.io.IOException 
     */
    public void listarServidor(String directorio) throws IOException{   
            FTPFile[] files = client.listFiles(directorio);
            if (files != null && files.length > 0) {
                for (FTPFile file : files) {
                    String nombreFile = file.getName();

                    // skip parent directory and directory itself
                    // Omite los directorios padre y el propio directorio.
                    if (nombreFile.equals(".") || nombreFile.equals("..")) continue;
                    
                    if (file.isDirectory()) {
                        childServer = new DefaultMutableTreeNode(nombreFile);
                        rootServer.add(childServer);                       
                        listarSubServidor(directorio, nombreFile, childServer);                                     
                    } else {
                        childServer = new DefaultMutableTreeNode(nombreFile);
                        rootServer.add(childServer);
                    }
                    
                }
            }
    }
    
    /**
     * Método para crear subNodos en el jTree. 
     * Si el archivo es un directorio, añade al padre el nodo y vuelve a llamarse recursivamente para volver a comprobar
     * si en esa carpeta existen más carpetas o sólo existen archivos.
     * @param dirPadre
     * @param dirActual
     * @param padre
     * @throws IOException 
     */
    
    public void listarSubServidor(String dirPadre, String dirActual, DefaultMutableTreeNode padre) throws IOException{
        // Si el directorio actual no es "" concatenamos ese directorio al que anterior
        if (!dirActual.equals("")) {
            dirPadre += "/" + dirActual;
        }
        
        FTPFile[] subFiles = client.listFiles(dirPadre);
        if (subFiles != null && subFiles.length > 0) {
            for (FTPFile subFile : subFiles) {
                String nombreSubFile = subFile.getName();
                
                DefaultMutableTreeNode subChild = new DefaultMutableTreeNode(nombreSubFile);
                
                if (nombreSubFile.equals(".") || nombreSubFile.equals("..")) continue;
                
                if (subFile.isDirectory()) {
                    padre.add(subChild);

                    listarSubServidor(dirPadre, nombreSubFile, subChild);                                
                } else {
                    padre.add(subChild);
                }                              
            }
        }     
    }
    
    /**
     * Método que devuelve el objeto seleccionado del jTree.
     * Devuelve una cadena con el objeto y la ruta absoluta del servidor del objeto
     * @return 
     */
    public String seleccionado(){
        DefaultMutableTreeNode selectedElement =
                (DefaultMutableTreeNode) vista.getjTreeServer().getSelectionPath().getLastPathComponent();

        Object ob = selectedElement.getUserObject();
        
        // Para listar la ruta
        Object[] o = selectedElement.getUserObjectPath();
        String ruta = "";
        
        for(int i = 0; i < o.length; i++){
            if(o.length > 1){
                ruta += o[i] + "/";
            }
        }

        return ob+";"+ruta;
    }
    
    /**
     * Método para subir un archivo. Comprueba del tipo que es, según el tipo que sea lo sube de una forma u otra
     * para que no haya pérdida de datos.
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public void subirArchivo() throws FileNotFoundException, IOException{       
        JFileChooser fc = new JFileChooser();
        
        int seleccion = fc.showOpenDialog(vista);

        if(seleccion==JFileChooser.APPROVE_OPTION){
            File fichero = fc.getSelectedFile();
            if (fichero.canRead()) {
                fRuta = new File(fichero.getAbsolutePath());
                String fName = fichero.getName();
                if(fichero.getName().endsWith(".txt") || fichero.getName().endsWith(".html") || fichero.getName().endsWith(".rtf") ){
                    FileInputStream fis = null;
                    fis = new FileInputStream(fRuta);
                    boolean res = client.storeFile(fName, fis);
                    
                    if(res == true) {
                        vista.seHaSubido();
                        initServerDirsJTree();
                    } else {
                        vista.noSeHaSubido();
                    }
                    fis.close();
                } else{
                    client.setFileType(FTP.BINARY_FILE_TYPE);
                    InputStream inputStream = new FileInputStream(fRuta);                                 
                    boolean res = client.storeFile(fName, inputStream);
                    if(res == true) {
                        initServerDirsJTree();
                        vista.seHaSubido();
                    } else {
                        vista.noSeHaSubido();
                    }
                    inputStream.close();
                }         
            }
        }    
    }
    
    /**
     * Método para descargar un archivo del servidor.
     * @throws IOException 
     */
    public void descargar() throws IOException{
        // Separamos las dos cadenas devueltas
        String[] elemento = seleccionado().split(";");
        String fichero = elemento[0];
        String ruta = elemento[1].substring(0, elemento[1].length()-1);
        
        // Lanzamos el File Chooser para seleccionar dónde queremos guardar el archivo.
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(fichero));
        fileChooser.setDialogTitle("Selecciona la ruta");   

        int approve = fileChooser.showSaveDialog(vista);
        if (approve == JFileChooser.APPROVE_OPTION) {
            File fileGuardar = fileChooser.getSelectedFile();
                client.setFileType(FTP.BINARY_FILE_TYPE);

                String remoteFile = ruta;      
                File archivoAdescargar = new File(fileGuardar.getAbsolutePath());

                OutputStream os = new BufferedOutputStream(new FileOutputStream(archivoAdescargar));
                boolean success = client.retrieveFile(remoteFile, os);
                os.close();

                if (success) {
                    vista.archivoDescargado();
                } else{
                    vista.archivoNoDescargado();
                }
            
        }
    }
    
    /**
     * Método para descargar un archivo del servidor.
     * @throws IOException 
     */
    public void borrar() throws IOException{
        // Separamos las dos cadenas devueltas
        String[] elemento = seleccionado().split(";");
        String fichero = elemento[0];
        String ruta = elemento[1].substring(0, elemento[1].length()-1);
        // creamos un fichero FTP con la ruta
        FTPFile ftpFile = client.mlistFile(ruta);
        boolean completado;

        if(vista.deseaBorrar().equals("borra")){
            if(ftpFile.isDirectory()){
                completado = client.removeDirectory(ruta);
            } else{
                completado = client.deleteFile(ruta);
            }
            
            if(completado){
                initServerDirsJTree();
                vista.borradoBien();
            } else{
                vista.borradoMal();
            }
        }
        
    }
    
    /**
     * Método para crear un directorio en el servidor.
     * @param nombre
     * @throws IOException 
     */
    public void crearCarpeta(String nombre) throws IOException{
        String dirToCreate = nombre;
        boolean success = client.makeDirectory(dirToCreate);
        if (success) {
            initServerDirsJTree();
        } else {
           vista.falloDirectorio();
        }
    }
    
}

