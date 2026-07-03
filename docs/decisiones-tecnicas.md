# Decisiones Tecnicas

## 2026-07-03 - Contexto Portable Para Codex

Decision: hacer que el repositorio sea autosuficiente para poder trabajar desde varios PCs sin copiar historiales locales de Codex.

Se documenta contexto en:

- `AGENTS.md`
- `README.md`
- `docs/contexto-proyecto.md`
- `docs/decisiones-tecnicas.md`
- `docs/codex-notes.md`

No se debe copiar ni versionar toda la carpeta `~/.codex`, porque puede incluir credenciales, autenticacion, historial local o estado sensible.

## 2026-07-03 - Fallback JSON Para Configuracion

Problema: la configuracion funcional del servicio depende de una llamada concreta a `ConfiguracioAplicacions`, que a su vez depende de BBDD/servicio externo.

Decision propuesta: mantener BBDD como fuente principal y anadir un fallback a JSON local si falla la carga remota.

Flujo deseado:

1. Intentar cargar configuracion desde la URL actual.
2. Si responde correctamente y trae datos, usar esa configuracion.
3. Si hay error, timeout, respuesta vacia o `exitCode` no satisfactorio, cargar un JSON local.
4. Si tambien falla el JSON, registrar error critico y no arrancar servidores DICOM con configuracion incompleta.

## Ubicacion Del JSON

El JSON real de servidor deberia estar fuera del WAR:

```text
/opt/ServidorDicomFHES/config/config-valors.json
```

En Windows/desarrollo:

```text
C:\ServidorDicomFHES\config\config-valors.json
```

La ruta deberia poder configurarse desde `application.properties` y/o variable de entorno:

```properties
config.valors.file=${CONFIG_VALORS_FILE:/opt/ServidorDicomFHES/config/config-valors.json}
```

## Formato Inicial Recomendado

Para reducir riesgo, el JSON inicial debe copiar la forma logica de `ConfigValorDTO`: una lista plana de objetos.

Ventaja: la logica existente de `Configuracio.configurarLlistatsSocket()` puede seguir funcionando casi igual.

Ejemplo saneado:

```json
[
  {
    "id": 27,
    "idApp": 3,
    "camp": "SERVER_STORAGE_INTERN",
    "valor1": "211",
    "valor2": "SDFHES",
    "descripcio": "Servidor storage DICOM",
    "encriptat": null,
    "tipus": "SERVER"
  }
]
```

## Evolucion Posible

Mas adelante se podria pasar a un JSON estructurado por intencion:

- `servers`
- `pacs`
- `rsync`
- `storage`
- `endpoints`
- `dicom.transferCapabilities`
- `dicom.presentationContexts`
- `dicom.tagMappings`

No se recomienda empezar por ahi si el objetivo es solo quitar dependencia critica de BBDD, porque implicaria tocar mas codigo y aumentar el riesgo.

## Recarga En Caliente

No se recomienda implementar recarga en caliente al principio. Cambios de puertos, AETitles o servidores DICOM implican desbindear y volver a bindear conexiones. Mejor cargar al arranque hasta que haya una necesidad clara.
