 package fhes.cat.services.impl;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomInputStream;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.context.support.UiApplicationContextUtils;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fhes.cat.config.APIConstants;
import fhes.cat.dto.BajarDicomExtDTO;
import fhes.cat.dto.BuscarDicomDTO;
import fhes.cat.dto.MapeosDicomDTO;
import fhes.cat.dto.RespostaEnviamentImatgesDicomDTO;
import fhes.cat.dto.StoredToPacs;
import fhes.cat.services.EnvioImagenesToPacs;
import fhes.cat.services.SendDicomToPACS;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import lombok.extern.slf4j.Slf4j;
import restapiresponse.RestApiResponse;
import services.BaseService;
import services.SocketService;
import services.impl.BaseServiceImpl;
import services.impl.SocketServiceImpl;

@Slf4j
@Service
public class EnvioImagenesToPacsImpl implements EnvioImagenesToPacs {

	private SocketService socketService = new SocketServiceImpl();
	
	@Autowired
	SendDicomToPACS sendDicom;

	BaseService baseService;
	ObjectMapper mapper;
	
	
	public EnvioImagenesToPacsImpl() {
		baseService = new BaseServiceImpl();
		mapper = new ObjectMapper();
	}

	
	@Override
	public void crearLista(int horaInicio  , int delayEntreImagenes, StoredToPacs storedToPacs, Map<String, String> mapa, boolean isForPacs, boolean esExterna) {
		final String urlResposta = storedToPacs.getOrigens().get(storedToPacs.getOrigens().size()-1);	
		RespostaEnviamentImatgesDicomDTO resultat = new RespostaEnviamentImatgesDicomDTO();
		
		resultat.setImatgesError(0);
		resultat.setImatgesOk(0);
		
		resultat.setId(storedToPacs.getId());

		String storedToPacsString = null;
		try {
			storedToPacsString = mapper.writeValueAsString(storedToPacs);
		} catch (JsonProcessingException e1) {
			socketService.notificarMissatge(SocketServiceImpl.MISSATGE_INFO, "Error DICOM - Error en el parse de  storedToPacs", " en "+this.getClass().getName());
		}
		
		//log.info("Busco los ficheros en: {}", storedToPacs.getPathImagen());
		File carpeta = new File(storedToPacs.getPathImagen());
		File[] archivos = carpeta.listFiles();
	
		
		if(archivos == null) {
			comunicaResultat(false, "Error DICOM - Ruta no encontrada "+ storedToPacs.getPathImagen(), urlResposta, resultat);
			socketService.notificarMissatge(SocketServiceImpl.MISSATGE_INFO, "Error DICOM - Ruta no encontrada ", storedToPacsString +" en "+this.getClass().getName());
			return;
		}
		
		log.info("Nº de ficheros encontrados: {}", archivos.length);
		
		
		Observable<File> intervalObservable = Observable.interval(horaInicio, delayEntreImagenes, TimeUnit.MILLISECONDS) //500
                .take(archivos.length) // Limita el número de emisiones al número de archivos
                .map(index -> archivos[index.intValue()]); // Mapea el índice al archivo correspondiente
		
		List<File> srFiles = new ArrayList<>();
		List<File> prFiles = new ArrayList<>();
		List<File> koFiles = new ArrayList<>(); //key object
		List<File> nonSrFiles = new ArrayList<>();
		
		intervalObservable.subscribe(new Observer<File>() {
			
            @Override
            public void onSubscribe(Disposable d) {
                //Inicio subcripción
            }

            @Override
            public void onNext(File archivo) {
            	try {
	                DicomInputStream dicomInputStream = new DicomInputStream(new FileInputStream(archivo));
	             	String tsuid = dicomInputStream.readFileMetaInformation().getString(Tag.TransferSyntaxUID);
	             	Attributes attrs = new Attributes();
	             	attrs = dicomInputStream.readDataset();
	             		
	             	String modality = attrs.getString(Tag.Modality);
	             		
	             	// Si modality es "SR", añadir archivo a la lista srFiles
	                if ("SR".equals(modality)) 		srFiles.add(archivo);
	                else if ("PR".equals(modality)) prFiles.add(archivo);
	                else if ("KO".equals(modality)) koFiles.add(archivo);
	                else {
	                	nonSrFiles.add(archivo);
	                	resultat.setModality(modality);
	                }
	                
	                dicomInputStream.close();
            		
            	}catch(FileNotFoundException e) {
                	socketService.notificarMissatge(SocketServiceImpl.MISSATGE_INFO, "Error DICOM", "Archivo no encontrado "+  e.getMessage() +" en "+this.getClass().getName());
                    log.info("Archivo no encontrado: {}", e);
                } catch (IOException e) {
                	socketService.notificarMissatge(SocketServiceImpl.MISSATGE_INFO, "Error DICOM", "IOException "+ archivo.getName() +" en "+this.getClass().getName());
                    log.info("Error de lectura: {}", e);
                    
                }
            }
            
            @Override
            public void onError(Throwable e) {
            	log.info("Error Observable<File> intervalObservable: {}", e.getMessage());
            	socketService.notificarMissatge(SocketServiceImpl.MISSATGE_INFO, "Error enviar imágenes PACS", "Error Observable<File>: " + e.getMessage());
            	
            	//comunicaResultat(true, e.getMessage(), urlResposta, resultat);
                
            }
            
            @Override
            public void onComplete() {
                processFiles(nonSrFiles, mapa, isForPacs, resultat, true, urlResposta, storedToPacs.getBorrarFicheroDicom()); //true porque es imagen, false si es tipo informe
                processFiles(srFiles, mapa, isForPacs, resultat, false, urlResposta, storedToPacs.getBorrarFicheroDicom());
                processFiles(prFiles, mapa, isForPacs, resultat, false, urlResposta, storedToPacs.getBorrarFicheroDicom());
                processUnwantedFiles(koFiles, resultat, urlResposta);
                comunicaResultat(false, "", urlResposta, resultat);
            }
		});
            
	}         
		
	
	private void mostrarValoreswaveAnnotationModuleLog(Attributes attrs) {
		int numComentaris = attrs.getSequence(Tag.WaveformAnnotationSequence).size();
		log.info("\n Longitud de la sequencia WaveformAnnotationSequence: {}\n", numComentaris);
		for(int x = 0; x < numComentaris ; x++) {
			if(attrs.getSequence(Tag.WaveformAnnotationSequence).get(x) != null) {
				log.info("Posición: {}\n", x);
				
				if(attrs.getSequence(Tag.WaveformAnnotationSequence).get(x).getSequence(Tag.MeasurementUnitsCodeSequence)!= null) {
					Attributes measurementUnitsCode = new Attributes(attrs.getSequence(Tag.WaveformAnnotationSequence).get(x).getSequence(Tag.MeasurementUnitsCodeSequence).get(0));
						log.info(">>Code Value: {}\n", measurementUnitsCode.getString(Tag.CodeValue));
						log.info(">>Coding Scheme Designator: {}\n", measurementUnitsCode.getString(Tag.CodingSchemeDesignator));
						log.info(">>Coding Scheme Version: {}\n", measurementUnitsCode.getString(Tag.CodingSchemeVersion));
						log.info(">>Code Meaning: {}\n",measurementUnitsCode.getString(Tag.CodeMeaning));
				}
					 
				
				if(	attrs.getSequence(Tag.WaveformAnnotationSequence).get(x).getSequence(Tag.ConceptNameCodeSequence) != null &&
					attrs.getSequence(Tag.WaveformAnnotationSequence).get(x).getSequence(Tag.ConceptNameCodeSequence).get(0).getSequence(Tag.ModifierCodeSequence) != null){
					
					Attributes modifierCode = attrs.getSequence(Tag.WaveformAnnotationSequence).get(x).getSequence(Tag.ConceptNameCodeSequence).get(0).getSequence(Tag.ModifierCodeSequence).get(0);
						//Sequence modifierCodeSequence =  attrs.getSequence(Tag.WaveformAnnotationSequence).get(0).getSequence(Tag.ModifierCodeSequence);
						log.info(">>>Code Value: {}\n", modifierCode.getString(Tag.CodeValue));
						log.info(">>>Coding Scheme Designator: {}\n", modifierCode.getString(Tag.CodingSchemeDesignator));
						log.info(">>>Coding Scheme Version: {}\n",modifierCode.getString(Tag.CodingSchemeVersion));
						log.info(">>>Code Meaning: {}\n", modifierCode.getString(Tag.CodeMeaning));
				}
				
				log.info(">Referenced Waveform Channels: {}\n", attrs.getSequence(Tag.WaveformAnnotationSequence).get(x).getString(Tag.ReferencedWaveformChannels));
				log.info(">Referenced Sample Positions: {}\n", attrs.getSequence(Tag.WaveformAnnotationSequence).get(x).getString(Tag.ReferencedSamplePositions));
				log.info(">Annotation Group Number: {}\n", attrs.getSequence(Tag.WaveformAnnotationSequence).get(x).getString(Tag.AnnotationGroupNumber));
				log.info(">Numeric Value: {}\n", attrs.getSequence(Tag.WaveformAnnotationSequence).get(x).getString(Tag.NumericValue));
				log.info(">Unformatted Text Value: {}\n", attrs.getSequence(Tag.WaveformAnnotationSequence).get(x).getString(Tag.UnformattedTextValue));
			}
		}
	}
	

	private void comunicaResultat(boolean resultatSafisfactori, String descripcioError, String urlResposta, RespostaEnviamentImatgesDicomDTO resultat) {
		RestTemplate restTemplate = new RestTemplate();
		
		resultat.setError(resultatSafisfactori);
		resultat.setDescripcio(descripcioError);
		RestApiResponse resultado = baseService.mapToObj(RestApiResponse.class, restTemplate.postForObject(urlResposta, resultat, Object.class));
        log.info("Resultado del envio respuesta a {}, es {}", urlResposta, resultado.getExitCode());
        if(resultado.getExitCode() != 0)
        	socketService.notificarMissatge(SocketServiceImpl.MISSATGE_INFO, "Error enviar imágenes PACS", "Error enviando la respuesta de enviar imágenes a RisFhes");
	}


	@Override
	public void bajarEstudioDelPacs(String ID, String localAETitle) {
		Attributes query = new Attributes();

		query.setString(Tag.StudyInstanceUID, VR.PN, ID);	
		query.setString(Tag.QueryRetrieveLevel, VR.CS, "STUDY"); //IMAGE STUDY
		sendDicom.sendCMove(query, localAETitle);

	}

	@Override
	public void bajarEstudioDelPacs(BajarDicomExtDTO attrDicom, String localAETitle) {
		Attributes query = new Attributes();

		if(attrDicom.getStudyDate() != null && attrDicom.getStudyDate().trim().length() > 0) {
			query.setString(Tag.StudyDate, VR.DA, attrDicom.getStudyDate());
		}
		if(attrDicom.getStudyTime() != null && attrDicom.getStudyTime().trim().length() > 0) {
			query.setString(Tag.StudyTime, VR.TM, attrDicom.getStudyTime());
		}
		query.setString(Tag.QueryRetrieveLevel, VR.CS, "STUDY"); //IMAGE STUDY
		query.setString(Tag.StudyInstanceUID, VR.PN, attrDicom.getStudyInstanceUid());														
		sendDicom.sendCMove(query, localAETitle);

	}

//	@Override
//	public void leerTags(String pathImatge) {
//						
//		log.info("Busco los ficheros en: {}", pathImatge);
//		File carpeta = new File(pathImatge);
//		File[] archivos = carpeta.listFiles();
//		
//		
//		if(archivos == null) {
//			log.info("NO hay mail!!! {}", archivos);
//			//socketService.notificarMissatge(SocketServiceImpl.MISSATGE_INFO, "Error DICOM - Ruta no encontrada ", storedToPacsString +" en "+this.getClass().getName());
//			return;
//		}
//		
//		int horaInicio = 0;
//		int delayEntreImagenes = 0;
//		Observable<File> intervalObservable = Observable.interval(horaInicio, delayEntreImagenes, TimeUnit.MILLISECONDS) 
//                .take(archivos.length) // Limita el número de emisiones al número de archivos
//                .map(index -> archivos[index.intValue()]); // Mapea el índice al archivo correspondiente
//		
//		intervalObservable.subscribe(new Observer<File>() {
//			
//            @Override
//            public void onSubscribe(Disposable d) {
//                //Inicio subcripción
//            }
//
//            @Override
//            public void onNext(File archivo) {
//            	try {
//                	DicomInputStream dicomInputStream = new DicomInputStream(new FileInputStream(archivo));
//             		//String tsuid = dicomInputStream.readFileMetaInformation().getString(Tag.TransferSyntaxUID);
//             		Attributes attrs = new Attributes();
//             		
//             		dicomInputStream.readAllAttributes(attrs);
//             		try {
//             			log.info("lista atributos \n {}\n", attrs);
//             			log.info("lista atributos \n {}\n", dicomInputStream.readCommand());
//             			log.info("lista atributos \n {}\n", dicomInputStream.readDataset());
//             			log.info("lista atributos \n {}\n", dicomInputStream.readDatasetUntilPixelData());
//             		}catch(Exception e) {}
//             		
//             		
//             		//og.info("**** Lista de atributos ***** \n {}", attrs);
//             		             		             		             		             		             		
//                }catch(FileNotFoundException e) {
//                	socketService.notificarMissatge(SocketServiceImpl.MISSATGE_INFO, "Error DICOM", "Archivo no encontrado "+  e.getMessage() +" en "+this.getClass().getName());
//                    log.info("Archivo no encontrado: {}", e.getMessage());
//                } catch (IOException e) {
//                	socketService.notificarMissatge(SocketServiceImpl.MISSATGE_INFO, "Error DICOM", "IOException "+ archivo.getName() +" en "+this.getClass().getName());
//                    log.info("Error de lectura: {}", e);
//                    
//                }
//            }
//
//            @Override
//            public void onError(Throwable e) {
//                e.printStackTrace();
//            }
//
//            @Override
//            public void onComplete() {
//            	log.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^" );
//            }
//        });
//		
//	}

	
	public void buscarImagenesDelPacs(BuscarDicomDTO dicom) {
		sendDicom.sendCFind(dicom);
		
	}


	@Override
	public void borrarImagenDicom() {
		sendDicom.deleteDicomImage("", "");
		
	}
	
	@Override
	public String convertTomoDicomToBase64(File dicomFile) throws IOException {
		// 1) Leer el DICOM
	    Attributes dicomAttributes;
	    String base64Pdf = null;
	    
	    try (DicomInputStream dis = new DicomInputStream(dicomFile)) {
	    	dicomAttributes = dis.readDataset();
	    }
	    
	    try (DicomInputStream dis = new DicomInputStream(new FileInputStream(dicomFile))) {
            byte[] pdfBytes = dicomAttributes.getBytes(Tag.EncapsulatedDocument);

            if (pdfBytes != null) {
                base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);
            } else {
                log.info("No se encontró ningún documento embebido.");
            }
        }
	    return base64Pdf;
	}
	
	@Override
	public String convertECGDicomToBase64(File dicomFile) throws IOException {
		
	    // 1) Leer el DICOM
	    Attributes dataset;
	    try (DicomInputStream dis = new DicomInputStream(dicomFile)) {
	        dataset = dis.readDataset();
	    }

	    // 2) Extraer la secuencia de Waveform
	    List<Attributes> waveforms = dataset.getSequence(Tag.WaveformSequence);
	    if (waveforms == null || waveforms.isEmpty()) {
	        throw new RuntimeException("No se encontró la secuencia de Waveform en el DICOM");
	    }
	    Attributes ecgWaveform = waveforms.get(0);
	    //*******************
	 // 3) Leer la Waveform Annotation Sequence (0040,B020)
	    List<Attributes> annotations = dataset.getSequence(Tag.WaveformAnnotationSequence);
        String heartRate, prInterval, qrsDuration, qtInterval, qtcInterval, pAxis, qrsAxis, tAxis;
        heartRate = prInterval = qrsDuration = qtInterval = qtcInterval = pAxis = qrsAxis = tAxis = null;
	    if (annotations != null) {
	        // Variables para guardar resultados

	        
	        String bp = null; // si es un valor complejo, puede requerir parsing extra

	        for (Attributes annItem : annotations) {
	            // 4) Cada Item tiene un ConceptNameCodeSequence (0040,A043)
	            List<Attributes> conceptSeq = annItem.getSequence(Tag.ConceptNameCodeSequence);
	            if (conceptSeq == null || conceptSeq.isEmpty()) continue;

	            Attributes concept = conceptSeq.get(0);
	            String codeMeaning = concept.getString(Tag.CodeMeaning); 
	            // Por ej.: "Heart rate", "PR Interval", "QRS Duration", etc.

	            // 5) Leer el valor numérico (0040,A30A)
	            String numericValue = annItem.getString(Tag.NumericValue);

	            // 6) Comparar para saber qué medición es
	            if ("Ventricular Heart Rate".equalsIgnoreCase(codeMeaning)) {
	                heartRate = numericValue;
	            } else if ("PR interval".equalsIgnoreCase(codeMeaning)) {
	                prInterval = numericValue;
	            } else if ("QRS duration".equalsIgnoreCase(codeMeaning)) {
	                qrsDuration = numericValue;
	            } else if ("QT interval".equalsIgnoreCase(codeMeaning)) {
	                qtInterval = numericValue;
	            }else if ("QTC interval".equalsIgnoreCase(codeMeaning)) {
		                qtcInterval = numericValue;
	            } else if ("P Axis".equalsIgnoreCase(codeMeaning)) {
	                pAxis = numericValue;
	            } else if ("QRS Axis".equalsIgnoreCase(codeMeaning)) {
	                qrsAxis = numericValue;
	            } else if ("T Axis".equalsIgnoreCase(codeMeaning)) {
	                tAxis = numericValue;
	            } 

	            // 7) Para BP (ejemplo: "Blood Pressure"), a veces no es un solo valor,
	            //    sino dos (sistólica/diastólica) o más. El fabricante puede guardarlo distinto.
	            //    Aquí necesitarías revisar si se guarda en NumericValue o en un sub-Item.
	        }

	        // 8) Ahora heartRate, prInterval, etc. tienen los valores leídos (o null si no estaban).
	        System.out.println("Heart Rate: " + heartRate + " bpm");
	        System.out.println("PR Interval: " + prInterval + " ms");
	        System.out.println("QRS Duration: " + qrsDuration + " ms");
	        System.out.println("QT/QTc Interval: " + qtInterval + "/" + qtcInterval + " ms");
	        System.out.println("P/R/T Axes: " + pAxis + "/" + qrsAxis + "/" + tAxis +"º");
	        // ...
	    }
	    //*******************
	    
	    
	    
	    // 3) Obtener parámetros clave
	    int numberOfChannels = ecgWaveform.getInt(Tag.NumberOfWaveformChannels, 1);
	    int numberOfSamples = ecgWaveform.getInt(Tag.NumberOfWaveformSamples, 0);
	    String accessionNumber = dataset.getString(Tag.AccessionNumber);
	    String studyDate = dataset.getString(Tag.StudyDate);
	    String patientId = dataset.getString(Tag.PatientID);
	    
	    log.info("Número de canales: {}", numberOfChannels);
	    log.info("Número de muestras: {}", numberOfSamples);

	    if (numberOfSamples == 0) {
	        throw new RuntimeException("No se encontraron muestras en la señal ECG");
	    }

	    float samplingFrequency = ecgWaveform.getFloat(Tag.SamplingFrequency, 500.0f);

	    // 4) Extraer los datos (16 bits, little endian)
	    byte[] waveformData = ecgWaveform.getBytes(Tag.WaveformData);
	    if (waveformData == null || waveformData.length == 0) {
	        throw new RuntimeException("No se encontraron datos de la señal en el DICOM");
	    }
	    int totalSamples = numberOfSamples * numberOfChannels;
	    if (waveformData.length < totalSamples * 2) {
	        throw new RuntimeException("Los datos de la señal están incompletos");
	    }
	    short[] ecgValues = new short[totalSamples];
	    for (int i = 0; i < totalSamples; i++) {
	        int index = i * 2;
	        ecgValues[i] = (short) ((waveformData[index] & 0xFF) | (waveformData[index + 1] << 8));
	    }

	    // Factor de calibración
	    double factorCalibracion = 0.005;

	    // 5) Crear series para todos los canales encontrados
	    XYSeries[] seriesArray = new XYSeries[numberOfChannels];
	    for (int c = 0; c < numberOfChannels; c++) {
	        seriesArray[c] = new XYSeries("Canal " + (c + 1));
	    }
	    double channelOffset = 3.0; // Separación entre canales en mV

	    for (int i = 0; i < numberOfSamples; i++) {
	        double timeSec = i / samplingFrequency;
	        for (int c = 0; c < numberOfChannels; c++) {
	            short rawValue = ecgValues[(i * numberOfChannels) + c];
	            double voltageMv = rawValue * factorCalibracion;
	            // Invertir el orden de los canales para que el Canal 1 esté arriba
	            double yValue = voltageMv + ((numberOfChannels - 1 - c) * channelOffset);
	            seriesArray[c].add(timeSec, yValue);
	        }
	    }

	    // Unir todas las series en un único dataset
	    XYSeriesCollection datasetChart = new XYSeriesCollection();
	    for (int c = 0; c < numberOfChannels; c++) {
	        datasetChart.addSeries(seriesArray[c]);
	    }

	    // 6) Crear el gráfico
	    JFreeChart chart = ChartFactory.createXYLineChart(
	            "ECG - " + numberOfChannels + " canales -  accessionNumber: " + accessionNumber + " StudyDate: " + studyDate + " PatientId: " + patientId ,
	            "Tiempo (s)",
	            "Voltaje (mV) + offset",
	            datasetChart,
	            PlotOrientation.VERTICAL,
	            false,   
	            false,  
	            false   
	    );

	    XYPlot plot = chart.getXYPlot();
	    plot.setBackgroundPaint(Color.WHITE);

	    // Configurar TODOS los canales en negro y reducir el grosor a 0.75f
	    for (int c = 0; c < numberOfChannels; c++) {
	        plot.getRenderer().setSeriesPaint(c, Color.BLACK);
	        plot.getRenderer().setSeriesStroke(c, new BasicStroke(0.75f));
	    }

	    // 7) Ajustar ejes y cuadrícula
	    double minTime = 0.0;
	    double maxTime = 10.0;  
	    double minMv = -1.5;
	    double maxMv = 1.5 + ((numberOfChannels - 1) * channelOffset);
	    
	    NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
	    domainAxis.setRange(minTime, maxTime);
	    NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
	    rangeAxis.setRange(minMv, maxMv);

	    domainAxis.setTickUnit(new NumberTickUnit(0.2));
	    rangeAxis.setTickUnit(new NumberTickUnit(0.5));
	    plot.setDomainGridlinesVisible(true);
	    plot.setRangeGridlinesVisible(true);
	    plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
	    plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

	 
	    
	 // Aquí creas el subtítulo con la información que desees mostrar
	    //heartRate = prInterval = qrsDuration = qtInterval = qtcInterval = pAxis = qrsAxis = tAxis = null;
	    String info = "Vent Rate: "+ heartRate +" BPM \n"
	    			+ "PR: "+ prInterval +" ms\n"
	    			+ "QRS: "+ qrsDuration +" ms\n"
	                + "QT/QTc: "+ qtInterval +"/"+ qtcInterval +" ms\n"
	                + "P/R/T Axes: "+ pAxis +"/"+ qrsAxis +"/"+tAxis  +"°\n\n";
	    TextTitle subtitle = new TextTitle(info);

	    // Opcionalmente, puedes cambiar la fuente, tamaño, color, etc.
	    subtitle.setFont(new Font("SansSerif", Font.PLAIN, 11));
	    subtitle.setPaint(Color.BLACK);
	    //subtitle.setHorizontalAlignment(HorizontalAlignment.LEFT);
	    subtitle.setPadding(new RectangleInsets(0.0, 100.0, 0.0, 0.0));

	    subtitle.setHorizontalAlignment(HorizontalAlignment.LEFT);
	    subtitle.setTextAlignment(HorizontalAlignment.LEFT);
	    
	    // Finalmente, añades el subtítulo al chart
	    chart.addSubtitle(subtitle);
	    
	    Font annotationFont = new Font("SansSerif", Font.BOLD, 12);
	    // 8) Agregar etiquetas de canal al inicio del gráfico
        
	    for (int c = 0; c < numberOfChannels; c++) {
	        double yPos = ((numberOfChannels - 1 - c) * channelOffset) + 1.0;
	        XYTextAnnotation annotation = new XYTextAnnotation("Canal " + (c + 1), 0.5, yPos );
	        annotation.setFont(annotationFont);
	        annotation.setPaint(Color.BLACK);
	        plot.addAnnotation(annotation);
	    }

	    // 9) Ajustar tamaño dinámico del gráfico en píxeles
//	    double mmToPx = 4.0;
//	    double speedMmS = 25.0;
	    double gainMmMv = 10.0;
//	    double totalTimeSec = maxTime - minTime;
	    double totalMvRange = maxMv - minMv;             
	    int widthPx = 1400; // Ancho aumentado para más claridad
	    double totalMmRange = totalMvRange * gainMmMv;
	    int heightPx = (int) Math.round(totalMmRange * 3); // El 3 es la separacion entre ondas


	    // Renderizar imagen y convertir a Base64
	    BufferedImage bufferedImage = chart.createBufferedImage(widthPx, heightPx);
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    ImageIO.write(bufferedImage, "png", baos);
	    byte[] imageBytes = baos.toByteArray();
	    return Base64.getEncoder().encodeToString(imageBytes);
	}


	
	
	private void processUnwantedFiles(List<File> files, RespostaEnviamentImatgesDicomDTO resultat, String urlResposta) {
		files.forEach(f ->{
			resultat.addOK();
			boolean sePudoBorrar = f.delete();
 			if(!sePudoBorrar) {
 				socketService.notificarMissatge(SocketServiceImpl.MISSATGE_INFO, "Error SDFHES", "NO se pudo borrar el ficehro "+ f.getName() +" en "+this.getClass().getName());
 			}
		});
	}
	
	private void processFiles(List<File> files, Map<String, String> mapa, boolean isForPacs, RespostaEnviamentImatgesDicomDTO resultat, boolean esImagen, String urlResposta, boolean eliminarArchivo) {
	
        for (File archivo : files) {
        	try {
            	DicomInputStream dicomInputStream = new DicomInputStream(new FileInputStream(archivo));
         		String tsuid = dicomInputStream.readFileMetaInformation().getString(Tag.TransferSyntaxUID);
         		Attributes attrs = new Attributes();
         		attrs = dicomInputStream.readDataset();
        	
         		String cuid = attrs.getString(Tag.SOPClassUID); 
         		String iuid = attrs.getString(Tag.SOPInstanceUID);
         		
         		
         		
         		log.info("\n SOPInstanceUID: {} \n", attrs.getString(Tag.SOPInstanceUID));
         		log.info("MediaStorageSOPInstanceUID: {} \n", attrs.getString(Tag.MediaStorageSOPInstanceUID));

        		if(esImagen) {
        			log.info("\n ************  StudyDate: {} StudyTime: {}\n", attrs.getString(Tag.StudyDate), attrs.getString(Tag.StudyTime) );
        			resultat.setStudyDateTime(attrs.getString(Tag.StudyDate)+attrs.getString(Tag.StudyTime));
        		}
        		
         		String studyInstanceUID = attrs.getString(Tag.StudyInstanceUID);
         		
         		log.info("\n*** Study Instance UID de la imagen: {} ***\n", studyInstanceUID);
         		
         		for (MapeosDicomDTO reg : APIConstants.listaMapeosTags) {
         			//log.info("\n****** attrs.setString({}, {}, {})\n", reg.getTagValue(), VR.valueOf(reg.getVR()), mapa.get(reg.getTag()) != null ? mapa.get(reg.getTag()) : attrs.getString(reg.getTagValue()));
         			if(reg.getModality().equals(attrs.getString(Tag.Modality))){
         				//log.info("attrs.setString({}, {}, {})", reg.getTagValue(), VR.valueOf(reg.getVR()), mapa.get(reg.getTag()) != null ? mapa.get(reg.getTag()) : attrs.getString(reg.getTagValue()));
         				String valor = mapa.get(reg.getTag());
         				if (reg.getTag().equals("UnformattedTextValue") && valor != null && !valor.isBlank()) { //si la etiqueta es de comentarios y hay comentarios
         					Instant now = Instant.now();
         					String numeracionImg = attrs.getString(Tag.StudyInstanceUID)+"."+now.getNano();
         					attrs.setString(Tag.MediaStorageSOPInstanceUID, VR.UI,  numeracionImg);
         					attrs.setString(Tag.SOPInstanceUID, VR.UI,  numeracionImg);
         					iuid = numeracionImg; //nuevo id para identificar la imagen que se quiere añadir
         					
         					if(isForPacs == false){ //cambio la hora para que en el visor hc3 aparezca como informado en primer lugar. 
         						String horaAtributoAdicional = null;
         						String fechaAtributoAdicional = null;
         						try {
         							horaAtributoAdicional = mapa.get("StudyTime");
             						fechaAtributoAdicional = mapa.get("StudyDate");
         						}catch(Exception e) {
         							socketService.notificarMissatge(SocketServiceImpl.MISSATGE_INFO, "Error DICOM Server", "NO se han encontrado las claves StudyDate StudyTime en el mapa de cambios");
         						}
         						
         						String horaEstudio = horaAtributoAdicional == null  ? attrs.getString(Tag.StudyTime): horaAtributoAdicional;
             					String fechaEstudio= fechaAtributoAdicional== null  ? attrs.getString(Tag.StudyDate) : fechaAtributoAdicional;
             					
             					//resultat.setStudyDateTime(fechaAtributoAdicional+horaAtributoAdicional);
             					
             			        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HHmmss"); 	// Crear un formatter para convertir la cadena a LocalTime
             			        LocalTime time = LocalTime.parse(horaEstudio, formatter); 				// Convertir la cadena a LocalTime
             			        LocalTime newTime = time.minusMinutes(10);              			    // Restar 10 minutos
             			        String resultado = newTime.format(formatter);           			    // Convertir de nuevo a cadena
             			       
             			        attrs.setString(Tag.StudyTime, VR.TM, resultado);
             			        attrs.setString(Tag.SeriesTime, VR.TM, resultado);
             			        attrs.setString(Tag.InstanceCreationTime, VR.TM, resultado);
             			        attrs.setString(Tag.AcquisitionDateTime, VR.DT, fechaEstudio+resultado);
             			        attrs.setString(Tag.ContentTime, VR.TM, resultado);
             			        log.info("Hora estudio inicial: {} hora final: {}", horaEstudio, resultado);
         					}
    			                     				
            				if (attrs.getSequence(Tag.WaveformAnnotationSequence) == null){
            					Sequence annotationSeq = null;
            					annotationSeq = attrs.newSequence(Tag.WaveformAnnotationSequence, 1);
            					Attributes annotationItem = new Attributes();
            					annotationItem.setString(Tag.UnformattedTextValue, VR.ST, mapa.get(reg.getTag()));
            					annotationItem.setString(Tag.AnnotationGroupNumber, VR.US, "3");
            					annotationSeq.add(annotationItem);	
            				}else {
            					
            					String informe =  mapa.get(reg.getTag());
            					if(informe != null && informe.length() > 0) {
            						log.info("\n*****************************************  Inicio comentarios ************************************************\n");
                					List <Attributes> waveAnnotationModule = new ArrayList<>();
                					
                					//guardo todos los atributos
                					attrs.getSequence(Tag.WaveformAnnotationSequence).forEach(atributo ->{
                						if(atributo != null)
                							waveAnnotationModule.add(atributo);
                					});
                					
                					//mostrarValoreswaveAnnotationModuleLog(attrs);                					
                					
                					log.info("Elimino atributos");
                					attrs.getSequence(Tag.WaveformAnnotationSequence).clear();
                					String valorNulo = null;					
                					boolean existeEstructuraComentatio = false;
                					for(Attributes a : waveAnnotationModule) {
                						if(a.getSequence(Tag.MeasurementUnitsCodeSequence)!= null || existeEstructuraComentatio) {
                							a.setString(Tag.UnformattedTextValue, VR.ST, valorNulo);
                							attrs.getSequence(Tag.WaveformAnnotationSequence).add(a);
                						}else {
                							//si ya he añadido un cometario no añadir más, pero no puedo salir del bucle porque hay que añadir el resto
                							a.getString(Tag.AnnotationGroupNumber, "3");
                							a.setString(Tag.UnformattedTextValue, VR.ST, mapa.get(reg.getTag()));
                							attrs.getSequence(Tag.WaveformAnnotationSequence).add(a);
                							existeEstructuraComentatio = true;
                						}                							
                					}
                					
                					if(!existeEstructuraComentatio) {
                						Attributes attributes = new Attributes();
                						
                						attributes.setString(Tag.UnformattedTextValue, VR.ST, mapa.get(reg.getTag()));
                						attributes.setString(Tag.AnnotationGroupNumber, VR.US, "3");
                						attributes.setString(Tag.ReferencedSamplePositions, VR.UL, valorNulo);
                						attributes.setString(Tag.NumericValue, VR.DS, valorNulo);
                						attributes.setInt(Tag.ReferencedWaveformChannels, VR.US, 1);
                						attrs.getSequence(Tag.WaveformAnnotationSequence).add(attributes);
                					}
                					//mostrarValoreswaveAnnotationModuleLog(attrs);
            					}
            				}
            				log.info("\n fin asignacion comentarios \n");
         				}else {
         					if (!reg.getTag().equals("StudyDescription") || isForPacs) { //no informar StudyDescription en los rsync
         						attrs.setString(reg.getTagValue(), VR.valueOf(reg.getVR()),  mapa.get(reg.getTag()) != null ? mapa.get(reg.getTag()) : attrs.getString(reg.getTagValue()));
         					}
         				}
         				
         			}
         			
         		}

//         	    log.info("\nStudyInstanceUID: {}\n", attrs.getString(Tag.StudyInstanceUID));
//         		log.info("\nAtributos que envio:\n {}", attrs);
         		
         		String calledAET, callingAET, ipCalled;
         		int portCalled;
         		
         		log.info("\n is for PACS: {}", isForPacs);
         		
         		if(isForPacs) {
         			calledAET = APIConstants.aTitlePacs;
         			callingAET = APIConstants.localServerSender.getValor2();
         			ipCalled = APIConstants.ipPacs;
         			portCalled = APIConstants.portPacs;
         		}else {
         			calledAET = APIConstants.aTitleRsync;
        			callingAET = APIConstants.aTitlePacs;
        			ipCalled = APIConstants.ipRsync;
        			portCalled = APIConstants.portRsync;
         		}
         		
         		
         		
         		log.info("\n***sendDicom.sendDICOM(tsuid, cuid, iuid,  calledAET, callingAET, ipCalled, portCalled) {} {} {} {} {} {} {}\n***", tsuid, cuid, iuid,  calledAET, callingAET, ipCalled, portCalled);             		
         		if(sendDicom.sendDICOM(attrs, tsuid, cuid, iuid,  calledAET, callingAET, ipCalled, portCalled)) {
         			resultat.addOK();
         			if(esImagen) { //solo contar los frames en pruebas tipo imágen, no en informes
    	         		try {
    	         			int num = attrs.getInt(Tag.NumberOfFrames, 1);
    	         			log.info("\nNumberOfFrames: {}", num);
    	         			resultat.addFrames(num);
    	         		}catch(Exception e) {
    	         			log.info("Error contando NumberOfFrames");
    	         		}
             		}
         			dicomInputStream.close();
         			if(eliminarArchivo) { //quitar para eliminar todas las imágenes, sean para pacs o rsync 
	         			boolean sePudoBorrar = archivo.delete();
	         			if(!sePudoBorrar) {
	         				socketService.notificarMissatge(SocketServiceImpl.MISSATGE_INFO, "Error SDFHES", "NO se pudo borrar el fichero "+ archivo.getName() +" en "+this.getClass().getName());
	         			}
         			}
         		}else {
         			if(esImagen) { //solo contar los frames en pruebas tipo imágen, no en informes
    	         		try {
    	         			int num = attrs.getInt(Tag.NumberOfFrames, 1);
    	         			resultat.addFramesKo(num);
    	         		}catch(Exception e) {
    	         			log.info("Error contando NumberOfFramesKo");
    	         		}
             		}
         			resultat.addKO();
         			dicomInputStream.close();
         		}
	        }catch(FileNotFoundException e) {
	        	//comunicaResultat(true, "Error DICOM - Archivo no encontrado "+  e.getMessage() +" en "+this.getClass().getName(), urlResposta, resultat);
	        	socketService.notificarMissatge(SocketServiceImpl.MISSATGE_INFO, "Error DICOM", "Archivo no encontrado "+  e.getMessage() +" en "+this.getClass().getName());
	            log.info("Archivo no encontrado: {}", e.getMessage());
	            resultat.addKO();
	        } catch (IOException e) {
	        	//comunicaResultat(true, "Error DICOM - IOException "+ archivo.getName() +" en "+this.getClass().getName(), urlResposta, resultat);
	        	socketService.notificarMissatge(SocketServiceImpl.MISSATGE_INFO, "Error DICOM", "IOException "+ archivo.getName() +" en "+this.getClass().getName());
	            log.info("Error de lectura: {}", e);
	            resultat.addKO();
	        }
        }
        log.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^" );
        log.info("^^^^^^^^^^^^^^^^^ Proceso completado. Enviados ok: {} ko: {} frames OK: {} frames KO: {} Modality: {} DateTime: {} ^^^^^^^^^^^^^^^^^", resultat.getImatgesOk(), resultat.getImatgesError(), resultat.getNumFrames(), resultat.getNumFramesKo(), resultat.getModality(), resultat.getStudyDateTime());
        log.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^" );
	}

	private void processFile(File archivo, Map<String, String> mapa, boolean isForPacs, RespostaEnviamentImatgesDicomDTO resultat, boolean esImagen, String urlResposta) {
    
    	try {
        	DicomInputStream dicomInputStream = new DicomInputStream(new FileInputStream(archivo));
     		String tsuid = dicomInputStream.readFileMetaInformation().getString(Tag.TransferSyntaxUID);
     		Attributes attrs = new Attributes();
     		attrs = dicomInputStream.readDataset();
    	
     		String cuid = attrs.getString(Tag.SOPClassUID); 
     		String iuid = attrs.getString(Tag.SOPInstanceUID);
     		log.info("**\n SOPInstanceUID: {} \n", attrs.getString(Tag.SOPInstanceUID));
     		log.info("**\n MediaStorageSOPInstanceUID: {} \n", attrs.getString(Tag.MediaStorageSOPInstanceUID));
     		
     		String studyInstanceUID = attrs.getString(Tag.StudyInstanceUID);
     		
     		log.info("\n*** Study Instance UID de la imagen: {} ***\n", studyInstanceUID);
     		
     		for (MapeosDicomDTO reg : APIConstants.listaMapeosTags) {
     			//log.info("\nattrs.setString({}, {}, {})\n", reg.getTagValue(), VR.valueOf(reg.getVR()), mapa.get(reg.getTag()) != null ? mapa.get(reg.getTag()) : attrs.getString(reg.getTagValue()));
     			if(reg.getModality().equals(attrs.getString(Tag.Modality))){
     				//log.info("attrs.setString({}, {}, {})", reg.getTagValue(), VR.valueOf(reg.getVR()), mapa.get(reg.getTag()) != null ? mapa.get(reg.getTag()) : attrs.getString(reg.getTagValue()));
     				if(reg.getTag().equals("UnformattedTextValue")) {

     					Instant now = Instant.now();
     					String numeracionImg = attrs.getString(Tag.StudyInstanceUID)+"."+now.getNano();
     					attrs.setString(Tag.MediaStorageSOPInstanceUID, VR.UI,  numeracionImg);
     					attrs.setString(Tag.SOPInstanceUID, VR.UI,  numeracionImg);
     					iuid = numeracionImg; //nuevo id para identificar la imagen que se quiere añadir
     					
     					//Cambiar hora				
     					if(isForPacs == false){
     						String horaAtributoAdicional = null;
     						String fechaAtributoAdicional = null;
     						try {
     							horaAtributoAdicional = mapa.get("StudyTime");
         						fechaAtributoAdicional = mapa.get("StudyDate");
     						}catch(Exception e) {
     							socketService.notificarMissatge(SocketServiceImpl.MISSATGE_INFO, "Error DICOM Server", "NO se han encontrado las claves StudyDate StudyTime en el mapa de cambios");
     						}
     						
     						String horaEstudio = horaAtributoAdicional == null  ? attrs.getString(Tag.StudyTime): horaAtributoAdicional;
         					String fechaEstudio= fechaAtributoAdicional== null  ? attrs.getString(Tag.DateTime) : fechaAtributoAdicional;
         					
         			        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HHmmss"); 	// Crear un formatter para convertir la cadena a LocalTime
         			        LocalTime time = LocalTime.parse(horaEstudio, formatter); 				// Convertir la cadena a LocalTime
         			        LocalTime newTime = time.minusMinutes(10);              			    // Restar 10 minutos
         			        String resultado = newTime.format(formatter);           			    // Convertir de nuevo a cadena
         			        
         			        attrs.setString(Tag.StudyTime, VR.TM, resultado);
         			        attrs.setString(Tag.SeriesTime, VR.TM, resultado);
         			        attrs.setString(Tag.InstanceCreationTime, VR.TM, resultado);
         			        attrs.setString(Tag.AcquisitionDateTime, VR.DT, fechaEstudio+resultado);
         			        attrs.setString(Tag.ContentTime, VR.TM, resultado);
         			        log.info("Hora estudio inicial: {} hora final: {}", horaEstudio, resultado);
     					}
			                     				
        				if (attrs.getSequence(Tag.WaveformAnnotationSequence) == null){
        					Sequence annotationSeq = null;
        					annotationSeq = attrs.newSequence(Tag.WaveformAnnotationSequence, 1);
        					Attributes annotationItem = new Attributes();
        					annotationItem.setString(Tag.UnformattedTextValue, VR.ST, mapa.get(reg.getTag()));
        					annotationItem.setString(Tag.AnnotationGroupNumber, VR.US, "3");
        					annotationSeq.add(annotationItem);	
        				}else {
        					
        					String informe =  mapa.get(reg.getTag());
        					if(informe != null && informe.length() > 0) {
        						log.info("\n*****************************************  Inicio comentarios ************************************************\n");
            					List <Attributes> waveAnnotationModule = new ArrayList<>();
            					
            					//guardo todos los atributos
            					attrs.getSequence(Tag.WaveformAnnotationSequence).forEach(atributo ->{
            						if(atributo != null)
            							waveAnnotationModule.add(atributo);
            					});
            					
            					//mostrarValoreswaveAnnotationModuleLog(attrs);                					
            					
            					log.info("Elimino atributos");
            					attrs.getSequence(Tag.WaveformAnnotationSequence).clear();
            					String valorNulo = null;					
            					boolean existeEstructuraComentatio = false;
            					for(Attributes a : waveAnnotationModule) {
            						if(a.getSequence(Tag.MeasurementUnitsCodeSequence)!= null || existeEstructuraComentatio) {
            							a.setString(Tag.UnformattedTextValue, VR.ST, valorNulo);
            							attrs.getSequence(Tag.WaveformAnnotationSequence).add(a);
            						}else {
            							//si ya he añadido un cometario no añadir más, pero no puedo salir del bucle porque hay que añadir el resto
            							a.getString(Tag.AnnotationGroupNumber, "3");
            							a.setString(Tag.UnformattedTextValue, VR.ST, mapa.get(reg.getTag()));
            							attrs.getSequence(Tag.WaveformAnnotationSequence).add(a);
            							existeEstructuraComentatio = true;
            						}                							
            					}
            					
            					if(!existeEstructuraComentatio) {
            						Attributes attributes = new Attributes();
            						
            						attributes.setString(Tag.UnformattedTextValue, VR.ST, mapa.get(reg.getTag()));
            						attributes.setString(Tag.AnnotationGroupNumber, VR.US, "3");
            						attributes.setString(Tag.ReferencedSamplePositions, VR.UL, valorNulo);
            						attributes.setString(Tag.NumericValue, VR.DS, valorNulo);
            						attributes.setInt(Tag.ReferencedWaveformChannels, VR.US, 1);
            						attrs.getSequence(Tag.WaveformAnnotationSequence).add(attributes);
            					}
            					//mostrarValoreswaveAnnotationModuleLog(attrs);
        					}
        				}
        				log.info("\n fin asignacion comentarios \n");
     				}else {
     					attrs.setString(reg.getTagValue(), VR.valueOf(reg.getVR()),  mapa.get(reg.getTag()) != null ? mapa.get(reg.getTag()) : attrs.getString(reg.getTagValue()));
     				}
     				
     			}
     			
     		}

//     	    log.info("\nStudyInstanceUID: {}\n", attrs.getString(Tag.StudyInstanceUID));
//     		log.info("\nAtributos que envio:\n {}", attrs);
     		
     		String calledAET, callingAET, ipCalled;
     		int portCalled;
     		
     		log.info("\n is for PACS: {}", isForPacs);
     		
     		if(isForPacs) {
     			calledAET = APIConstants.aTitlePacs;
     			callingAET = APIConstants.localServerSender.getValor2();
     			ipCalled = APIConstants.ipPacs;
     			portCalled = APIConstants.portPacs;
     		}else {
     			calledAET = APIConstants.aTitleRsync;
    			callingAET = APIConstants.aTitlePacs;
    			ipCalled = APIConstants.ipRsync;
    			portCalled = APIConstants.portRsync;
     		}
     		try {
     			int num = attrs.getInt(Tag.NumberOfFrames, 1);
     			log.info("\nNumberOfFrames: {}", num);
     			resultat.addFrames(num);
     		}catch(Exception e) {
     			log.info("Error contando NumberOfFrames");
     		}
     		
     		log.info("\n***sendDicom.sendDICOM(tsuid, cuid, iuid,  calledAET, callingAET, ipCalled, portCalled) {} {} {} {} {} {} {}\n***", tsuid, cuid, iuid,  calledAET, callingAET, ipCalled, portCalled);             		
     		if(sendDicom.sendDICOM(attrs, tsuid, cuid, iuid,  calledAET, callingAET, ipCalled, portCalled)) {
     			resultat.addOK();
     			dicomInputStream.close();
     			if(isForPacs) {
     				boolean sePudoBorrar = archivo.delete();
     				if(!sePudoBorrar) {
     					socketService.notificarMissatge(SocketServiceImpl.MISSATGE_INFO, "Error SDFHES", "NO se pudo borrar el ficehro "+ archivo.getName() +" en "+this.getClass().getName());
     				}
     			}
     		}else {
     			resultat.addKO();
     			dicomInputStream.close();
     		}
        }catch(FileNotFoundException e) {
        	comunicaResultat(true, "Error DICOM - Archivo no encontrado "+  e.getMessage() +" en "+this.getClass().getName(), urlResposta, resultat);
        	socketService.notificarMissatge(SocketServiceImpl.MISSATGE_INFO, "Error DICOM", "Archivo no encontrado "+  e.getMessage() +" en "+this.getClass().getName());
            log.info("Archivo no encontrado: {}", e.getMessage());
        } catch (IOException e) {
        	comunicaResultat(true, "Error DICOM - IOException "+ archivo.getName() +" en "+this.getClass().getName(), urlResposta, resultat);
        	socketService.notificarMissatge(SocketServiceImpl.MISSATGE_INFO, "Error DICOM", "IOException "+ archivo.getName() +" en "+this.getClass().getName());
            log.info("Error de lectura: {}", e.getMessage());
        }
    
    log.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^" );
    log.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ Proceso completado. Enviados ok: {} ko: {} frames: {} ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^", resultat.getImatgesOk(), resultat.getImatgesError(), resultat.getNumFrames());
    log.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^" );
	}


	


}







