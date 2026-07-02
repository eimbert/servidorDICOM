package fhes.cat.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fhes.cat.config.APIConstants;
import fhes.cat.dto.NotificacioEstatDTO;
import fhes.cat.enums.Estados;
import fhes.cat.services.NotificarEstats;
import lombok.extern.slf4j.Slf4j;
import restapiresponse.RestApiResponse;
import services.SocketService;
import services.impl.BaseServiceImpl;
import services.impl.SocketServiceImpl;

@Slf4j
@Service
public class NotificacioEstatImpl implements NotificarEstats{

	private BaseServiceImpl baseService = new BaseServiceImpl();
	ObjectMapper mapper = new ObjectMapper();
	
	private SocketService socketService = new SocketServiceImpl();
	
	public void enviarNotificacioEstat(NotificacioEstatDTO estat) {
		
		try {
			log.info("Notificació: {}", mapper.writeValueAsString(estat));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		
		
		RestTemplate restTemplate = new RestTemplate();	
		String url = APIConstants.urlNewImg;
		RestApiResponse response = null;
		try {
			response = baseService.mapToObj(RestApiResponse.class, restTemplate.postForObject(url, estat, Object.class));
		}catch(Exception e) {
			socketService.notificarMissatge(SocketServiceImpl.MISSATGE_INFO, "Error SDFHES", "Error enviando notificación ["+ url +"] DICOM - Storage "+ e.getMessage());
		}
			
		if(response != null || response.getExitCode()==0) {
			log.info("Notificación enviada correctamente");
		}else {
			log.info("Error enviando Notificación");
		}
				
	}
	
	@Override
	public void notificaEstat(String affectedSOPInstanceUID, String patientID, String modality, String estado, String studyInstanceUID, String device,
							  String aEAsociado, String patientName, String studyDescription) {
		NotificacioEstatDTO estat = new NotificacioEstatDTO();
			
		estat.setModality(modality);
		estat.setEstat(convertEstados(estado));
		estat.setStudyInstanceUid(studyInstanceUID);
		estat.setAffectedSOPInstanceUID(affectedSOPInstanceUID);
		estat.setNhc(patientID);
		estat.setAETitle(aEAsociado);
		estat.setDevice(device);
		estat.setStudyDescription(studyDescription);
				
		enviarNotificacioEstat(estat);
	}

	@Override
	public void notificaEstat(String requestedSOPInstanceUID, String estado, String patientName, String studyDescription) {
		NotificacioEstatDTO estat = new NotificacioEstatDTO();
		
		estat.setAffectedSOPInstanceUID(requestedSOPInstanceUID);
		estat.setEstat(convertEstados(estado));
		estat.setStudyDescription(studyDescription);
		enviarNotificacioEstat(estat);
	}

	@Override
	public void notificaEstat(String modality, String device, String accessionNumber, String studyInstanceUid,
							  String nhc, int origen, String nomCentre, String pathImages, String affectedSOPInstanceUID, 
							  String aEAsociado, String patientName, String idCita, String informe, String studyDate, String studyTime, String studyDescription){
		
				
		NotificacioEstatDTO estat = new NotificacioEstatDTO();
		estat.setEstat(origen == APIConstants.IMATGE_RECUPERADA_DEL_PACS ? convertEstados("RECUPERAT") :convertEstados("FIRST IMAGE"));
		estat.setModality(modality);
		estat.setDevice(device);
		estat.setAETitle(aEAsociado);
		estat.setAccessionNumber(accessionNumber);
		estat.setStudyInstanceUid(studyInstanceUid);
		estat.setAffectedSOPInstanceUID(affectedSOPInstanceUID);
		estat.setNhc(nhc);
		estat.setOrigen(origen);
		estat.setPathImages(pathImages);
		estat.setNomCentre(nomCentre);
		estat.setIdCita(idCita);
		estat.setInforme(informe);
		estat.setStudyDate(studyDate);
		estat.setStudyDescription(studyDescription);
		estat.setNomPacient(patientName);
		
		try {
			estat.setAcquisitionDate(studyDate+studyTime);
			
		}catch(Exception e) {
			log.info("Error en acquisitionTime, studyDate: {}, studyTime", studyDate, studyTime);
		}
		
		enviarNotificacioEstat(estat);
	}

	private String convertEstados(String incomingValue) {
        for (Estados status : Estados.values()) {
            if (incomingValue.equals(status.getOriginalValue())) {
                return status.getAbbreviation();
            }
        }
        return "Unknown"; // Valor no reconocido
    }
}
