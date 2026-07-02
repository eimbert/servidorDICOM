package fhes.cat.dmdcm4che3.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.PDVInputStream;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.BasicCStoreSCP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fhes.cat.config.APIConstants;
import fhes.cat.dmdcm4che3.utilities.DisplayTagVisitor;
import fhes.cat.services.NotificarEstats;
import fhes.cat.services.impl.NotificacioEstatImpl;
import services.BaseService;
import services.SocketService;
import services.impl.BaseServiceImpl;
import services.impl.SocketServiceImpl;



public class BasicCStoreSCPImpl extends BasicCStoreSCP {

	static byte saltoLinea = (char) 13;
	static byte cr = (char) 10;
	
	
	NotificarEstats notificaEstat = new NotificacioEstatImpl();
	
	private static final Logger log = LoggerFactory.getLogger(BasicCStoreSCPImpl.class);
	private boolean guardarDicom;
	private boolean reenviarDicom;
	private int origen;
	
	BaseService baseService = new BaseServiceImpl();
	
	private SocketService socketService = new SocketServiceImpl();
	
	
	public BasicCStoreSCPImpl(boolean guardarDicom, boolean reenviarDicom, String... sopClasses) {
		super(sopClasses);
		this.guardarDicom = guardarDicom;
		this.reenviarDicom = reenviarDicom;
		this.origen = APIConstants.IMATGE_INTERNA;
	}
	
	public BasicCStoreSCPImpl(boolean guardarDicom, boolean reenviarDicom, int origen) {
		super("*");
		this.guardarDicom = guardarDicom;
		this.reenviarDicom = reenviarDicom;
		this.origen = origen;
	}
	

	@Override
	public void store(Association as, PresentationContext pc, Attributes rq, PDVInputStream data, Attributes rsp)  {
		

		String tsuid = pc.getTransferSyntax();
		String iuid = rq.getString(Tag.AffectedSOPInstanceUID);
		String cuid = rq.getString(Tag.AffectedSOPClassUID);
        
		InputStream targetStream = null;
		DicomInputStream dis = null;
		List<Byte> byteList = new ArrayList<>();
		int byteValue;

		try {
			while ((byteValue = data.read()) != -1) {
				byteList.add((byte) byteValue);
			}
			byte[] byteArray = new byte[byteList.size()];
			for (int i = 0; i < byteList.size(); i++) {
				byteArray[i] = byteList.get(i);
			}
			targetStream = new ByteArrayInputStream(byteArray);
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			dis = new DicomInputStream(targetStream);
		} catch (IOException e) {
			e.printStackTrace();
		}

		Attributes attrDicom = null;
		try {
			attrDicom = dis.readDataset();
			//log.info("\n\n################################################# El uiuid: {} StudyDate: {} \n\n", iuid, attrDicom.getString(Tag.ContentDate));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		log.info("^^^^^^^^^^^^^^^^^^^^^^ DICOM recibido modalidad: {}", attrDicom.getString(Tag.Modality));
	
		String value = attrDicom.getString(Tag.StudyDescription);
		log.info("Comprobar longitud campo StudyDescription");
		if (value != null && value.length() > 62) {
		    log.info("Se cambia la descripción StudyDescription \n{} por \n{}", value, value.substring(0, 62));
			value = value.substring(0, 62);
			attrDicom.setString(Tag.StudyDescription, VR.LO, value);
		}
		
		String maquina = attrDicom.getString(Tag.StationName);

		String carpeta = APIConstants.pathDicomStorage + attrDicom.getString(Tag.StudyInstanceUID);
		
		log.info("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ Maquina: {} ", maquina);
		log.info("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ Carpeta: {} ", carpeta);
		
		//no enviar a HC3 las imagenes que vengan de una mauina que incluya en su nombre NOHC3 y vengan del PACS
		String aeTitleAsociado = APIConstants.obtenerAETitleasociado(as + "");
		//log.info("El AeTitle del PACS es: [{}] AeTitle Asociado [{}] y StationName [{}] contiene [{}].", APIConstants.aTitlePacs,aeTitleAsociado,maquina,APIConstants.maquinasQueContenganEnElNombreParaNoEnviarHC3);
		
		if (APIConstants.aTitlePacs.equals(aeTitleAsociado) && maquina != null 
		    && maquina.toUpperCase().contains(APIConstants.maquinasQueContenganEnElNombreParaNoEnviarHC3.toUpperCase())
		) {
		    log.info(
		        "DICOM ignorado. Origen PACS [{}] y StationName [{}] contiene [{}]. SOPInstanceUID [{}]",
		        aeTitleAsociado,
		        maquina,
		        APIConstants.maquinasQueContenganEnElNombreParaNoEnviarHC3,
		        iuid
		    );
		    return;
		}
				
				
		
		
		if(maquina ==null || maquina.trim().length() == 0) {
			try {
				Pattern pattern = Pattern.compile(".*<-([^\\(]+)");
				Matcher matcher = pattern.matcher(as+"");
			
					if (matcher.find()) {
			            String textoExtraido = matcher.group(1).trim();
			            maquina = textoExtraido.toUpperCase();
			        } else {
			        	maquina = ""; 
			        }
			}catch(Exception e) {
				socketService.notificarMissatge(SocketServiceImpl.MISSATGE_INFO, "Error SDFHES", "Error obteniendo AETasociado ["+ as +"] DICOM - Storage "+ e.getMessage());
			}
		}
				
		if(this.guardarDicom) {
			try {
				if(!comprobarCarpeta(carpeta)) {
					notificaEstat.notificaEstat(attrDicom.getString(Tag.Modality), maquina, attrDicom.getString(Tag.AccessionNumber), attrDicom.getString(Tag.StudyInstanceUID), 
							attrDicom.getString(Tag.PatientID), origen, attrDicom.getString(Tag.InstitutionName), carpeta, attrDicom.getString(Tag.SOPInstanceUID), 
							APIConstants.obtenerAETitleasociado(as+""), attrDicom.getString(Tag.PatientName), 
							attrDicom.getString(Tag.OtherPatientIDs), attrDicom.getString(Tag.StudyDescription), 
							attrDicom.getString(Tag.StudyDate), attrDicom.getString(Tag.StudyTime), attrDicom.getString(Tag.StudyDescription));
				}
			}catch(Exception e) {
				socketService.notificarMissatge(SocketServiceImpl.MISSATGE_CRITICAL, "Error SDFHES", "Error comunicando estado DICOM - Storage "+e.getMessage());
				log.info("Error comunicando estado {}", e.getMessage());
			}
			
			try {
								
				String modality = attrDicom.getString(Tag.Modality);
				if(modality == null || modality.isBlank()){
					log.info("Dicom sin modalidad informada, le ponemos OT");
					socketService.notificarMissatge(SocketServiceImpl.MISSATGE_INFO, "SDFHES", "Ha arribat un DICOM sense informar modalitat - Storage. AccesionNumber:" + attrDicom.getString(Tag.AccessionNumber));
					attrDicom.setString(Tag.Modality, VR.CS, "OT");
				}
				saveFile(convertAttributesToByteArray(attrDicom, tsuid, cuid, iuid), iuid+ ".dcm", carpeta);
			} catch (IOException e) {
				socketService.notificarMissatge(SocketServiceImpl.MISSATGE_CRITICAL, "Error SDFHES", "Error al escribir el archivo DICOM - Storage"+e.getMessage());
				log.info("^^^^^^^^^^^^^^^^^^^^^^ Error al escribir el archivo. {}", e.getMessage());
			}	
		}
		
	}

	private void saveFile(byte[] byteArray, String fileName, String carpeta) {
				
		try (FileOutputStream fos = new FileOutputStream(carpeta+"/"+fileName)) {
            fos.write(byteArray);
            fos.flush();
            //log.info("^^^^^^^^^^^^^^^^^^^^^^ ByteArray escrito en el archivo exitosamente.");
        } catch (IOException e) {
        	socketService.notificarMissatge(SocketServiceImpl.MISSATGE_CRITICAL, "Error SDFHES", "Error al escribir el archivo DICOM - Storage"+e.getMessage());
            log.info("^^^^^^^^^^^^^^^^^^^^^^ Error al escribir el archivo. {}", e.getMessage());
        }
	}
	

	private boolean comprobarCarpeta(String nomCarpeta) {
	File carpeta = new File(nomCarpeta);
	if (carpeta.exists()) {
		log.info("No se creo la ruta, NO es primera imagen");
        return true;
    } else {
        boolean creacionExitosa = carpeta.mkdirs();
        if (creacionExitosa) {
        	log.info("Se creo la ruta, ES primera imagen");
        	return false;
        } else {
        	log.info("Error al intentar crar la primera ruta en la primera imagen");
        	socketService.notificarMissatge(SocketServiceImpl.MISSATGE_CRITICAL, "Error SDFHES", "No s'ha pogut crear la carpeta "+ nomCarpeta +" en DICOM - l");
            return false;
        }
    }
}
//	private boolean comprobarCarpeta(String nomCarpeta, String modality) {
//		File carpeta = new File(nomCarpeta);
//		if (carpeta.exists()) {
//			if(!noSeInformoLaImagenAnterior) {
//				log.info("No se creo la ruta, NO es primera imagen");
//            	return true;
//			}else {
//				log.info("No se creo la ruta pero es primera imagen, anteriormente se habia recibido un informe");
//				noSeInformoLaImagenAnterior = false;
//            	return false;
//			}
//        } else {
//            boolean creacionExitosa = carpeta.mkdirs();
//            if (creacionExitosa) {
//            	if(modality.equals("SR")) {
//            		noSeInformoLaImagenAnterior = true;
//            		log.info("Se creo la ruta, Pero no es primera imagen, es un informe");
//            		return true;
//            	}else {
//            		log.info("Se creo la ruta, ES primera imagen");
//            		return false;
//            	}
//            } else {
//            	log.info("Error al intentar crar la primera ruta en la primera imagen");
//            	socketService.notificarMissatge(SocketServiceImpl.MISSATGE_CRITICAL, "Error SDFHES", "No s'ha pogut crear la carpeta "+ nomCarpeta +" en DICOM - l");
//                return false;
//            }
//        }
//	}
	
	private byte[] convertAttributesToByteArray(Attributes attributes, String tsuid, String cuid, String iuid) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DicomOutputStream dicomOutputStream = new DicomOutputStream(out, tsuid);
          
        try {
            dicomOutputStream.writeDataset(Attributes.createFileMetaInformation(iuid, cuid, tsuid), attributes);
            dicomOutputStream.finish();
            dicomOutputStream.close();
        } catch (IOException e) {
        	log.info("^^^^^^^^^^^^^^^^^^^^^^ Error conevertir Attributes to ByteArray {}", e.getMessage());
            e.printStackTrace();
        }

        return out.toByteArray();
    }
	
}
