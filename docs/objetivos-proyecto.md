# Objetivos Del Proyecto

Este documento resume el objetivo real del proyecto a medio/largo plazo. Sirve como contexto compartido para trabajar desde distintos equipos y para que Codex entienda hacia donde debe evolucionar el repositorio.

## Vision

Evolucionar el motor DICOM actual hacia una plataforma interna de gestion, revision y apoyo operativo sobre estudios DICOM.

El sistema actual ya recibe estudios desde dispositivos conectados, como electros, TACs y otras modalidades, los almacena en disco, expone worklist y permite enviar estudios al PACS. La evolucion deseada es construir sobre esa base:

- una consola web Angular para visualizar estudios y metadatos;
- un editor controlado de metadatos DICOM;
- herramientas de correccion/reconciliacion antes de enviar al PACS;
- un anonimizador DICOM robusto;
- una integracion experimental con IA para generar observaciones preliminares no diagnosticas, siempre revisadas por personal medico.

## Principios

- No sustituir al medico ni generar diagnosticos autonomos.
- Mantener trazabilidad completa de cambios, envios y resultados.
- Separar claramente original, corregido y anonimizado.
- Evitar edicion libre e indiscriminada de tags DICOM.
- No enviar datos identificables a servicios externos.
- Empezar por valor operativo interno antes de incorporar IA.
- Documentar decisiones y contexto en el repositorio para trabajar desde cualquier PC.

## Producto 1: Consola DICOM Interna

Este es el primer producto recomendable.

Objetivos:

- listar estudios recibidos;
- ver detalle de estudio, series e instancias;
- consultar metadatos DICOM relevantes;
- mostrar todos los tags DICOM de forma navegable;
- visualizar imagenes;
- corregir metadatos administrativos permitidos;
- reenviar manualmente al PACS;
- marcar errores de identificacion;
- consultar estados de proceso;
- auditar cambios.

Estados posibles:

- recibido;
- pendiente de revision;
- corregido;
- enviado al PACS;
- error de envio;
- anonimizado;
- procesado por IA;
- descartado.

Campos candidatos a edicion controlada:

- `PatientName`;
- `PatientID`;
- `PatientBirthDate`;
- `PatientSex`;
- `AccessionNumber`;
- `StudyDescription`;
- `SeriesDescription`;
- `ReferringPhysicianName`;
- `InstitutionName`, si aplica;
- campos internos de trazabilidad.

Campos que requieren especial cautela:

- `StudyInstanceUID`;
- `SeriesInstanceUID`;
- `SOPInstanceUID`;
- `Modality`;
- fechas tecnicas del estudio/adquisicion;
- `PixelData`;
- `TransferSyntaxUID`;
- tags privados;
- referencias entre objetos.

Politica propuesta: permitir correcciones administrativas, pero no modificar informacion tecnica o diagnostica sin un proceso validado.

## Producto 2: Anonimizador DICOM

La anonimizacion es requisito previo para cualquier integracion con IA externa y tambien aporta valor para soporte, pruebas, formacion y exportaciones.

Objetivos:

- eliminar o pseudonimizar tags identificativos;
- revisar texto libre;
- tratar tags privados;
- pseudonimizar UIDs cuando sea necesario;
- detectar riesgos de informacion identificable en imagen;
- generar una version anonimizada separada del original;
- validar automaticamente campos obvios antes de permitir exportar o enviar a IA.

Campos y zonas sensibles:

- `PatientName`;
- `PatientID`;
- `PatientBirthDate`;
- `PatientAddress`;
- telefonos;
- `AccessionNumber`;
- `StudyID`;
- `InstitutionName`;
- medicos referentes o realizadores;
- operadores;
- identificadores de dispositivo, segun politica;
- fechas;
- UIDs;
- tags privados;
- comentarios libres;
- burned-in annotations dentro del pixel;
- overlays;
- PDFs encapsulados;
- Secondary Capture;
- Structured Reports.

Riesgo principal: no basta con borrar tags. Algunos equipos queman nombre, NHC, fecha u otros datos directamente en la imagen. En esos casos se necesita deteccion adicional, por ejemplo OCR o reglas por modalidad/equipo.

## Producto 3: Asistente IA Para Observaciones Preliminares

La IA se plantea como piloto controlado, no como diagnostico automatico.

Nombre funcional recomendado:

- "Asistente de lectura preliminar no validada";
- "Borrador de observaciones para revision medica";
- "Observaciones preliminares generadas automaticamente".

Mensaje obligatorio en UI:

> Contenido generado automaticamente. No tiene valor diagnostico hasta revision y validacion por facultativo.

Objetivos:

- enviar solo estudios o imagenes anonimizadas;
- preparar imagenes renderizadas o representativas;
- enviar metadatos minimos y no identificativos;
- recoger respuesta del modelo;
- guardar prompt, modelo, version, entrada y salida;
- permitir revision medica obligatoria;
- registrar decision del medico.

No se debe enviar el DICOM original completo directamente al modelo.

Entrada recomendada a IA:

- imagenes renderizadas anonimizadas;
- modalidad;
- region anatomica si esta disponible;
- grupo etario o edad aproximada si es necesario y legalmente aceptable;
- sexo si es necesario y legalmente aceptable;
- motivo de exploracion anonimizado, si existe;
- pregunta clinica concreta.

## Arquitectura Objetivo

```text
Dispositivo / Modalidad
        |
        v
Motor DICOM Java 17
        |
        +--> Almacenamiento original bruto
        |
        +--> Indice de estudios en BBDD
        |
        +--> Consola Angular interna
        |
        +--> Editor controlado de metadatos
        |
        +--> Version corregida / reconciliada
        |
        +--> Envio a PACS
```

Flujo IA:

```text
Estudio original
        |
        v
Anonimizacion DICOM
        |
        v
Validacion de anonimato
        |
        v
Extraccion de imagenes / series representativas
        |
        v
Modelo IA
        |
        v
Observaciones preliminares
        |
        v
Revision medica obligatoria
        |
        v
Informe validado por facultativo
```

## Backend Java 17

La base Java 17 encaja con:

- Spring Boot;
- dcm4che para lectura, modificacion, C-STORE, C-FIND, C-MOVE y MWL;
- HAPI HL7 para mensajes HL7;
- almacenamiento filesystem organizado por `StudyInstanceUID`;
- BBDD para indice operativo;
- auditoria de cambios;
- cola de procesamiento para anonimizacion, IA y envio PACS;
- endpoints REST para Angular.

No conviene depender solo del filesystem. Debe existir un indice en BBDD.

Campos minimos sugeridos para indice:

- id interno;
- patient id;
- patient name;
- accession number;
- study instance uid;
- study date;
- modality;
- numero de series;
- numero de instancias;
- estado;
- ruta en disco;
- enviado a PACS;
- anonimizado;
- procesado por IA;
- fecha de recepcion.

## Frontend Angular

Objetivos iniciales:

- listado de estudios;
- filtros por estado, fecha, modalidad, paciente y accession;
- detalle study/series/instances;
- visor basico;
- panel de tags;
- formulario de edicion controlada;
- diff antes/despues;
- boton de reenvio al PACS;
- panel de errores y auditoria.

Para visualizacion DICOM:

- no implementar render DICOM desde cero;
- valorar CornerstoneJS para visor real;
- para MVP, valorar renderizar PNG/JPEG desde backend y mostrarlo en Angular.

## Fases Recomendadas

### Fase 1 - Consola DICOM

- listado de estudios recibidos;
- detalle de metadatos;
- visor basico;
- edicion controlada de identificacion;
- auditoria;
- reenvio manual a PACS;
- estados operativos.

Prioridad: muy alta.

### Fase 2 - Anonimizador

- perfil de anonimizado configurable;
- eliminacion/pseudonimizacion de tags sensibles;
- gestion de UIDs;
- deteccion de burned-in annotation cuando sea posible;
- exportacion de estudio anonimizado;
- validacion automatica.

Prioridad: muy alta antes de IA.

### Fase 3 - IA Experimental

- solo pruebas;
- solo estudios anonimizados;
- comenzar con pocas modalidades;
- generar observaciones preliminares;
- revision medica obligatoria;
- guardar prompt, modelo, entrada, salida y decision.

Prioridad: piloto controlado.

### Fase 4 - Produccion Controlada

- contrato y entorno API adecuados;
- validacion clinica;
- analisis de riesgos;
- comite interno;
- base juridica/consentimiento si aplica;
- evaluacion regulatoria si tiene finalidad clinica;
- trazabilidad completa.

Prioridad: solo tras validacion.

## Riesgos

Riesgos tecnicos:

- romper coherencia DICOM al editar tags criticos;
- fallos de transferencia syntax o SOP classes;
- estudios volumetricos complejos;
- imagenes con texto identificativo quemado;
- PDFs o SR con datos sensibles;
- rendimiento con estudios grandes.

Riesgos IA:

- alucinaciones;
- omision de hallazgos;
- mala interpretacion por window/level incorrecto;
- falta de contexto clinico;
- diferencias fuertes entre modalidades;
- salida convincente pero incorrecta.

Riesgos clinico-regulatorios:

- uso como soporte a decision clinica;
- necesidad de validacion;
- posible encaje como software sanitario;
- privacidad, retencion, contrato y residencia de datos;
- auditoria y responsabilidad profesional.

## Prioridades De Trabajo

| Bloque | Factibilidad | Riesgo | Prioridad |
| --- | --- | --- | --- |
| Angular visor estudios | Alta | Bajo/medio | Muy alta |
| Editor metadatos DICOM | Alta | Medio | Alta |
| Reenvio PACS desde UI | Alta | Medio | Alta |
| Anonimizacion DICOM | Media/alta | Alto si se hace mal | Muy alta |
| IA para observaciones preliminares | Media | Alto | Piloto |
| IA con uso clinico real | Factible, complejo | Muy alto | Solo con validacion |

## Decision De Enfoque

Primero construir una consola DICOM potente. Despues anadir anonimizacion. Solo despues conectar IA en modo laboratorio.

No saltar directamente de "recibo DICOM" a "IA informa estudios". Antes hacen falta trazabilidad, anonimizacion, validacion y control medico.
