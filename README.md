# Sistemas-Inteligentes 🤖 
Instrucciones de instalación
- Captura de dependencias necesarias para instalar el proyecto
- Instrucciones de ejecución:
  1. Desde la aplicación seleccionar *<> Code* y descargar el zip o realizar un git clone del HTTPS.
  2. Desde Eclipse, crear el proyecto de java / carpeta con el nombre SistemasMultiagentes.
  3. Hacer click derecho sobre la carpeta y seleccionar la opción de import.
  4. Seleccionar General > File System, pulsar next buscar el directorio donde se ha descargado y descomprimido el proyecto, y seleccionarlo.
  5. Seleccionar todos los recursos actuales y finalizar.
  6. Seleccionar "Yes to All".
  
<img width="488" height="215" alt="imagen" src="https://github.com/user-attachments/assets/c535f32a-2fd2-4113-b708-da5f318ced3b" />
<img width="488" height="200" alt="imagen" src="https://github.com/user-attachments/assets/d9b4e0e2-1f91-431b-b1ed-639cbde1a3d7" />

Ya se tendría todo lo necesario instalado.

- Las dependencias se encuentran en la librería lib: Jade.jar, Weka.jar, y commons-codec-1.15.jar.
  En caso de que aparezca error debido a que no se han importado correctamente las librerías, se debería de seleccionar el proyecto de java, seleccionar las properties > Java Buil Path, seleccionar Classpath y add External JARs y seleccionas los 3 .jar que se encuentran en la carpeta lib.
  
  <img width="488" height="215" alt="imagen" src="https://github.com/user-attachments/assets/7ba652fb-fc7e-48a4-9e6c-4c210e71a527" />

## Ejecución de la aplicación
- Desde la interfaz de eclipse en la barra herramientas superior elegir run configurations > Java Application > Seleccionar un nombre (el que se quiera), en project debe ser la carpeta del proyecto (SistemasMultiagentes), Main class debe de ser la clase main (es.upm.trading.Main), ahora ir a la pestaña de arguments y incluir en VM arguments (para que no salte ningun error en las predicciones):

--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED

## Datos de Ejemplo
No se tienen datos de ejemplo puesto que se descargan periodicamente desde la API Externa CoinGecko en tiempo real.

- Un diagrama de la arquitectura del sistema
- Una declaración de IA, indicando cómo se ha utilizado en el proyecto.
