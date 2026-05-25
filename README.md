# Sistemas-Inteligentes 🤖 
Instrucciones de instalación
- Captura de dependencias necesarias para instalar el proyecto
- Instrucciones de ejecución:
  1. Desde GitHub seleccionar *<> Code* y descargar el zip o realizar un git clone del HTTPS, y descomprimirlo.
  2. En Eclipse, crear un proyecto de Java (*Java Project*) con el nombre SistemasMultiagentes.
  3. Hacer click derecho sobre la carpeta y seleccionar la opción de *import*.
  4. Seleccionar General > File System y pulsar next.
  5. Buscar el directorio donde se ha descargado y descomprimido el proyecto, y seleccionarlo.
  6. Seleccionar todos los recursos actuales y *finish*.
  7. Seleccionar *Yes to All*.
  
<img width="488" height="215" alt="imagen" src="https://github.com/user-attachments/assets/c535f32a-2fd2-4113-b708-da5f318ced3b" />
<img width="488" height="200" alt="imagen" src="https://github.com/user-attachments/assets/d9b4e0e2-1f91-431b-b1ed-639cbde1a3d7" />

Con lo anterior, ya se tendría todo lo necesario instalado.

- Las dependencias se encuentran en la librería lib: Jade.jar, Weka.jar, y commons-codec-1.15.jar.
  En caso de que aparezca error por no haber importado correctamente las librerías, se debería de seleccionar el proyecto de java, seleccionar *properties* > Java Buil Path, seleccionar Classpath y add External JARs y seleccionar los 3 .jar que se encuentran en la carpeta lib.
  
  <img width="488" height="215" alt="imagen" src="https://github.com/user-attachments/assets/7ba652fb-fc7e-48a4-9e6c-4c210e71a527" />

## Ejecución de la aplicación
1. En la interfaz de Eclipse, en la barra de herramientas superior, elegir *Run Configurations* > *Java Application*.
2. Ponerle un nombre identificativo (Agente, por ejemplo)
3. En *Project*, seleccionar la carpeta del proyecto (que habíamos llamado SistemasMultiagentes).
4. En *Main class*, poner la clase *Main* (es.upm.trading.Main).
5. Ir la pestaña de *Arguments* y escribir en VM arguments lo siguiente para evitar que salten errores en las predicciones: --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED
6. 

## Datos de Ejemplo
No se tienen datos de ejemplo debido a que se descargan periódicamente desde la API Externa CoinGecko en tiempo real.

- Un diagrama de la arquitectura del sistema
- Una declaración de IA, indicando cómo se ha utilizado en el proyecto.
