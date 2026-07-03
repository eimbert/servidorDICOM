# Contexto Del Proyecto

`ServidorDicomFHES` es una pieza de integracion clinica. Hace de puente entre modalidades/equipos DICOM, PACS, rsyncbridge y aplicaciones backend como RIS/ConfiguracioAplicacions.

## Arranque

La aplicacion arranca desde `fhes.cat.IntegracionsDICOM`, con `@SpringBootApplication` y `@ComponentScan(basePackages = "fhes.cat.**")`.

Al iniciar, `fhes.cat.services.Configuracio` carga una lista de valores externos (`ConfigValorDTO`) y rellena estructuras globales en `APIConstants`.

Con esos valores crea servidores DICOM/HL7:

- `SERVER_STORAGE_INTERN`: storage DICOM interno.
- `SERVER_STORAGE_EXTERN`: storage DICOM externo.
- `SERVER_STORAGE_C-MOVE`: destino local para recuperaciones C-MOVE.
- `SERVER_STORAGE_C-MOVE_PDF`: destino para ECG/PDF recuperado del PACS.
- `SERVER_FIND`: servidor para C-FIND/worklist.
- `SERVER_MPPS`: servidor MPPS.
- `SERVER_HL7`: servidor HL7.

## Flujo De Recepcion DICOM

La recepcion por C-STORE se gestiona en `BasicCStoreSCPImpl`.

Flujo resumido:

1. Recibe dataset DICOM.
2. Lee tags principales como `Modality`, `StudyInstanceUID`, `StationName`, `AccessionNumber`, `PatientID`.
3. Normaliza `StudyDescription` si supera cierta longitud.
4. Decide carpeta de almacenamiento usando `APIConstants.pathDicomStorage + StudyInstanceUID`.
5. Si es la primera imagen del estudio, notifica al backend.
6. Guarda el fichero como `<SOPInstanceUID>.dcm`.
7. Ignora algunos casos `NOHC3` cuando vienen desde el PACS.

## Flujo De Envio Al PACS O Rsyncbridge

Los endpoints REST invocan `EnvioImagenesToPacsImpl`.

Flujo resumido:

1. Recibe un `StoredToPacs` con ruta de imagenes, datos adicionales y URL de respuesta.
2. Lee los ficheros DICOM de la carpeta.
3. Clasifica por modalidad/tipo: imagenes, SR, PR, KO.
4. Aplica mapeos de tags definidos por configuracion (`APIConstants.listaMapeosTags`).
5. Envia por C-STORE usando `SendDicomToPACSImpl`.
6. Acumula OK/KO y frames.
7. Notifica resultado a la URL recibida.

## Busqueda Y Recuperacion

- C-FIND se lanza desde `SendDicomToPACSImpl.sendCFind`.
- C-MOVE se lanza desde `SendDicomToPACSImpl.sendCMove`.
- Las respuestas DIMSE se procesan en `DimseRSPHandlerImpl`, que llama al backend configurado en `URL_RESULTADO_C-FIND`.

## Worklist

`WorkListCustom` responde peticiones C-FIND de worklist. Consulta el backend configurado en `URL_MWL` y transforma el resultado en atributos DICOM.

Para ECG, rellena campos adicionales como `OtherPatientIDs` y `StudyDescription`.

## HL7

`HL7Server` levanta un servidor HAPI HL7 simple. Registra un handler para todos los mensajes (`*`, `*`), loguea el mensaje recibido y responde ACK.

## Configuracion Actual

La configuracion llega como registros con estos campos:

- `id`
- `idApp`
- `camp`
- `valor1`
- `valor2`
- `descripcio`
- `encriptat`
- `tipus`

La misma estructura sirve para:

- puertos y AETitles de servidores;
- PACS/rsyncbridge;
- rutas locales;
- URLs backend;
- capacidades de transferencia;
- presentation contexts;
- mapeos de tags por modalidad.
