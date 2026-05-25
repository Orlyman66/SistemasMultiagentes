# Sistemas-Inteligentes 🤖 

## Instrucciones de la Instalación
  1. Desde GitHub seleccionar *<> Code* y descargar el zip o realizar un git clone del HTTPS, y descomprimirlo.
  2. En Eclipse, crear un proyecto de Java (*Java Project*) con el nombre SistemasMultiagentes.
  3. Hacer click derecho sobre la carpeta y seleccionar la opción de *import*.
  4. Seleccionar General > File System y pulsar next.
  5. Buscar el directorio donde se ha descargado y descomprimido el proyecto, y seleccionarlo.
  6. Seleccionar todos los recursos actuales y *finish*.
  7. Seleccionar *Yes to All*.
  
<img width="488" height="215" alt="imagen" src="https://github.com/user-attachments/assets/c535f32a-2fd2-4113-b708-da5f318ced3b" />

<img width="488" height="215" alt="imagen" src="https://github.com/user-attachments/assets/d9b4e0e2-1f91-431b-b1ed-639cbde1a3d7" />

<img width="488" height="215" alt="imagen" src="https://github.com/user-attachments/assets/a6811696-6dca-4caf-bd44-9fd67a6bdf9e" />

<img width="488" height="215" alt="imagen" src="https://github.com/user-attachments/assets/e1007490-50d1-4af4-8b42-122f104218e7" />

<img width="488" height="215" alt="imagen" src="https://github.com/user-attachments/assets/839149ea-368b-4b1d-88e6-286b47c06b83" />

<img width="488" height="215" alt="imagen" src="https://github.com/user-attachments/assets/836060be-6887-41c5-b145-87a048006665" />

Con lo anterior, ya se tendría todo lo necesario instalado.

## Instalación de dependencias
Las dependencias se encuentran en la librería lib: Jade.jar, Weka.jar, y commons-codec-1.15.jar.
En caso de que aparezca error por no haber importado correctamente las librerías, se debería de seleccionar el proyecto de java, seleccionar *properties* > Java Buil Path, seleccionar Classpath y add External JARs y seleccionar los 3 .jar que se encuentran en la carpeta lib.
  
  <img width="488" height="215" alt="imagen" src="https://github.com/user-attachments/assets/7ba652fb-fc7e-48a4-9e6c-4c210e71a527" />

## Instrucciones de Ejecución de la aplicación
1. En la interfaz de Eclipse, en la barra de herramientas superior, elegir *Run Configurations* > *Java Application*.
2. Ponerle un nombre identificativo (Agente, por ejemplo)
3. En *Project*, seleccionar la carpeta del proyecto (que habíamos llamado SistemasMultiagentes).
4. En *Main class*, poner la clase *Main* (es.upm.trading.Main).
5. Ir la pestaña de *Arguments* y escribir en VM arguments lo siguiente para evitar que salten errores en las predicciones: --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED
6. Aplicar los cambios y ejecutarlo.

<img width="800" height="300" alt="imagen" src="https://github.com/user-attachments/assets/ee617b3e-caf6-4c8d-85fc-fb3242f16d4b" />
<img width="800" height="419" alt="imagen" src="https://github.com/user-attachments/assets/696a9d5c-55df-46dd-9bca-aab3431f65bb" />

## Datos de Ejemplo
No se tienen datos de ejemplo debido a que se descargan periódicamente desde la API Externa CoinGecko en tiempo real.

## Diagrama arquitectónico
<img width="1600" height="1143" alt="imagen" src="https://github.com/user-attachments/assets/ccd6a523-f56b-4fc7-96ef-eb26934bdd15" />


## Declaración de IA
En este trabajo se ha utilizado IA principalmente para agilizar la escritura de código debido al escaso tiempo y gran carga de trabajo que teníamos en el grupo respecto a otras asignaturas.
Principalmente se ha usado para generar código para generar clases simples que no mantienen relación con el temario de la asignatura y para clases específicas como la carpeta ml para implementar funciones de  entrenamiento de  modelos, y cosas que no se han visto en esta parte de la asignatura.
