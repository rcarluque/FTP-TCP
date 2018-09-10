La aplicación está diseñada para conectarse a un servidor FTP, mediante la inserción de los datos de usuario en un jTextField en el JFrame.
Para este proyecto hemos usado la librería de ftp de apache commons, incluida en la carpeta lib del proyecto. Posteriomente la hemos agregado a la librería del proyecto.
Una vez introducidos los datos de conexión se mostrará un mensaje de conexión satisfactoria o en caso de que no, de conexión insatisfactoria.
Si al pasar 3 minutos intentas realizar una acción mostrará un mensaje informando de que se ha cerrado la conexión (3 cerrar a minutos de inactividad, por defecto).
Con esta aplicación podemos conectarnos y desconectarnos a un servidor, subir archivos, descargarnos un archivo del servidor, crear un directorio o eliminar un archivo. En caso de no seleccionar un archivo del jTree para hacer una operación también mostrará un mensaje de error.
También mostrará un mensaje de error si ha fallado el tiempo de conexión al realizar una operación.
Los datos del servidor se recogen y se muestran mediante un jTree el cual muestra una jerarquía de carpetas.
