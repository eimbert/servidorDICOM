# Instrucciones Para Codex

Este repositorio contiene el servicio `ServidorDicomFHES`, una aplicacion Spring Boot empaquetada como WAR que actua como servidor/intermediario DICOM para FHES.

## Contexto Del Proyecto

- Aplicacion Java 17 con Spring Boot 2.5.2-SNAPSHOT.
- Empaquetado WAR para despliegue en servidor de aplicaciones o ejecucion Spring Boot.
- Usa dcm4che para servicios DICOM y HAPI para HL7.
- La configuracion funcional se carga actualmente desde `ConfiguracioAplicacions`.
- La clase central de arranque es `fhes.cat.IntegracionsDICOM`.
- La carga de configuracion vive en `fhes.cat.services.Configuracio`.

## Reglas De Trabajo

- Antes de modificar comportamiento DICOM, leer las clases afectadas y las notas en `docs/`.
- Mantener cambios pequenos y verificables.
- No versionar credenciales, historiales locales de Codex, logs, `target/`, `.svn/` ni configuraciones reales de produccion.
- Si se anade configuracion local, documentar el formato con ejemplos saneados.
- Preferir compatibilidad hacia atras: este servicio depende de equipos, PACS, RSYNC, RIS y configuracion hospitalaria.

## Archivos De Contexto

- `README.md`: resumen rapido para abrir el proyecto.
- `docs/contexto-proyecto.md`: arquitectura y flujo funcional.
- `docs/decisiones-tecnicas.md`: decisiones tomadas o propuestas.
- `docs/codex-notes.md`: notas de continuidad para trabajar desde varios equipos.
- `config/config-valors.example.json`: ejemplo saneado del fallback JSON propuesto.

## Idea Pendiente Principal

Mantener la carga desde BBDD como fuente principal y anadir un fallback a JSON local si la llamada a configuracion falla. El JSON deberia tener la misma forma que `ConfigValorDTO` para minimizar cambios iniciales.
