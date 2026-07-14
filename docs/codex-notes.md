# Notas De Continuidad Para Codex

Estas notas sirven para abrir el proyecto desde otro PC y que Codex tenga contexto sin depender de la conversacion local anterior.

## Resumen Corto

El proyecto es un servidor/intermediario DICOM + HL7 para FHES. Recibe, guarda, modifica, envia, busca y recupera estudios DICOM. La configuracion funcional se intenta cargar desde `ConfiguracioAplicacions` y ahora dispone de fallback JSON local.

## Conversaciones Previas Relevantes

- Se hizo un respaldo inicial en GitHub en `eimbert/servidorDICOM.git`.
- Se reviso el proyecto y se identificaron las clases principales.
- Se acordo no sincronizar conversaciones completas de Codex entre PCs.
- Se acordo documentar el contexto dentro del repo.
- Se implemento el fallback JSON para configuracion si falla la carga desde BBDD/servicio remoto.
- Se documento el objetivo real del proyecto: consola DICOM Angular, editor controlado, anonimizador e IA experimental para observaciones preliminares revisadas por medico.
- Se implemento un primer modulo IA aislado bajo `fhes.cat.ai.*` para una unica imagen renderizada o ECG.

## Implementacion IA Actual

- Endpoint: `POST /ServidorDicomFHES/ai/opinion` con `multipart/form-data`.
- Entrada: PNG renderizado, tipo `IMAGE` o `ECG`, pregunta y flag `burnedInAnnotation`.
- No recibe ni envia el DICOM original.
- Usa OpenAI Responses API con `store=false`.
- Modelo por defecto: `gpt-5.6-terra`, reemplazable mediante `OPENAI_MODEL`.
- La clave se lee exclusivamente de `OPENAI_API_KEY`; nunca debe guardarse en properties, JSON o Git.
- Valida firma PNG, tamano maximo y tipos admitidos.
- Rechaza automaticamente `BurnedInAnnotation=YES`.
- La UI exige ademas una comprobacion visual porque el tag puede faltar o ser incorrecto.
- El resultado siempre se presenta como contenido preliminar que requiere validacion facultativa.

Configuracion principal en `application.properties`:

```properties
ai.openai.api-key=${OPENAI_API_KEY:}
ai.openai.model=${OPENAI_MODEL:gpt-5.6-terra}
ai.openai.enabled=${OPENAI_ENABLED:true}
```

## Fallback De Configuracion Implementado

1. Se intenta cargar la configuracion remota actual.
2. Si falla, devuelve error o no contiene registros, se carga `CONFIG_VALORS_FILE`.
3. El JSON conserva la lista plana compatible con `ConfigValorDTO`.
4. Si ambas fuentes fallan, el arranque se detiene para no levantar servidores DICOM incompletos.
5. Antes de clasificar una nueva configuracion se limpian las listas globales para evitar duplicados.

Ejemplo saneado: `config/config-valors.example.json`.

## Pendientes Posibles

- Desglosar la Fase 1 de `docs/objetivos-proyecto.md` en tareas tecnicas.
- Disenar modelo de indice de estudios en BBDD.
- Definir politica de tags editables/no editables.
- Evaluar CornerstoneJS frente a render backend PNG/JPEG para el MVP.
- Crear validaciones de configuracion obligatoria antes de arrancar servidores DICOM.
- Evitar `return null` en endpoints REST ante errores de parseo.
- Revisar uso de hilos manuales en endpoints y valorar `@Async` o executor controlado.
- Revisar mezcla de dcm4che2 y dcm4che3.
- Revisar lugares donde se crean servicios con `new` en vez de inyeccion Spring.
- Conseguir/instalar `cat.fhes:core.fhes.library:4.0.6` en el repositorio Maven corporativo o local. Sin este artefacto Maven no puede compilar ni ejecutar las pruebas preparadas.
- Probar una llamada real con una API key nueva configurada solo como variable de entorno.
- Incorporar OCR o limpieza automatica de texto quemado antes de eliminar la confirmacion visual.

## Git Y Trabajo Multiequipo

Flujo recomendado:

```bash
git pull
git status
git add .
git commit -m "Mensaje claro"
git push
```

No copiar toda la carpeta personal de Codex entre PCs. Si hay contexto nuevo importante, anadirlo a `docs/` y versionarlo.
