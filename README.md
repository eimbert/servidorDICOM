# ServidorDicomFHES

Servicio Spring Boot/WAR que integra operaciones DICOM y HL7 para FHES.

## Que Hace

- Levanta servidores DICOM de storage, C-FIND/worklist y MPPS segun configuracion externa.
- Recibe imagenes DICOM por C-STORE y las guarda por `StudyInstanceUID`.
- Notifica al backend cuando llega la primera imagen de un estudio.
- Envia estudios almacenados hacia PACS o rsyncbridge.
- Recupera estudios desde PACS usando C-MOVE.
- Busca estudios en PACS usando C-FIND.
- Genera worklist consultando un backend REST.
- Expone endpoints REST bajo `/ServidorDicomFHES/dicom`.
- Levanta un servidor HL7 simple que recibe mensajes y responde ACK.

## Entradas Principales

- Arranque: `src/main/java/fhes/cat/IntegracionsDICOM.java`
- Configuracion: `src/main/java/fhes/cat/services/Configuracio.java`
- Servidores DICOM: `src/main/java/fhes/cat/dmdcm4che3/DicomServer.java`
- Endpoints REST: `src/main/java/fhes/cat/controller/SendStoredDicomToPacs.java`
- Envio/recepcion DICOM: `src/main/java/fhes/cat/services/impl/EnvioImagenesToPacsImpl.java`
- Conexion con PACS: `src/main/java/fhes/cat/services/impl/SendDicomToPACSImpl.java`

## Configuracion

Actualmente la configuracion se obtiene desde una llamada a `ConfiguracioAplicacions`. La idea acordada es mantener esa fuente como principal y anadir un fallback a JSON local para que el servicio pueda arrancar si la BBDD o el servicio de configuracion no estan disponibles.

Ver:

- `docs/objetivos-proyecto.md`
- `docs/decisiones-tecnicas.md`
- `config/config-valors.example.json`

La ruta del fallback se configura con `CONFIG_VALORS_FILE`. Si la carga remota falla, esta lista JSON se carga con la misma estructura de `ConfigValorDTO`; si ambas fuentes fallan, el servicio no levanta servidores DICOM con valores incompletos.

## Opinion IA Experimental

El endpoint `POST /ServidorDicomFHES/ai/opinion` recibe una unica imagen PNG renderizada y anonimizada, su tipo (`IMAGE` o `ECG`) y una pregunta sin datos identificativos. Nunca recibe el DICOM original.

Configuracion:

```properties
ai.openai.api-key=${OPENAI_API_KEY:}
ai.openai.model=${OPENAI_MODEL:gpt-5.6-terra}
ai.openai.enabled=${OPENAI_ENABLED:true}
```

No se debe escribir ni versionar la clave real en `application.properties` o en el JSON de fallback.

## Trabajo Desde Varios Equipos

El proyecto debe sincronizarse mediante Git:

```bash
git pull
git push
```

No se recomienda sincronizar toda la carpeta personal de Codex (`~/.codex`) entre PCs, porque puede contener credenciales, estado local e historiales. El contexto portable vive en este repositorio: `AGENTS.md`, `README.md` y `docs/`.
