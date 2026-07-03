# Notas De Continuidad Para Codex

Estas notas sirven para abrir el proyecto desde otro PC y que Codex tenga contexto sin depender de la conversacion local anterior.

## Resumen Corto

El proyecto es un servidor/intermediario DICOM + HL7 para FHES. Recibe, guarda, modifica, envia, busca y recupera estudios DICOM. La configuracion funcional vive fuera del codigo y actualmente se carga desde una llamada REST a `ConfiguracioAplicacions`.

## Conversaciones Previas Relevantes

- Se hizo un respaldo inicial en GitHub en `eimbert/servidorDICOM.git`.
- Se reviso el proyecto y se identificaron las clases principales.
- Se acordo no sincronizar conversaciones completas de Codex entre PCs.
- Se acordo documentar el contexto dentro del repo.
- Se propuso anadir fallback JSON para configuracion si falla la carga desde BBDD.
- Se documento el objetivo real del proyecto: consola DICOM Angular, editor controlado, anonimizador e IA experimental para observaciones preliminares revisadas por medico.

## Pendientes Posibles

- Implementar fallback de configuracion desde JSON.
- Desglosar la Fase 1 de `docs/objetivos-proyecto.md` en tareas tecnicas.
- Disenar modelo de indice de estudios en BBDD.
- Definir politica de tags editables/no editables.
- Evaluar CornerstoneJS frente a render backend PNG/JPEG para el MVP.
- Crear validaciones de configuracion obligatoria antes de arrancar servidores DICOM.
- Evitar `return null` en endpoints REST ante errores de parseo.
- Revisar uso de hilos manuales en endpoints y valorar `@Async` o executor controlado.
- Revisar mezcla de dcm4che2 y dcm4che3.
- Revisar lugares donde se crean servicios con `new` en vez de inyeccion Spring.
- Anadir tests unitarios para clasificacion de configuracion y fallback JSON.

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
