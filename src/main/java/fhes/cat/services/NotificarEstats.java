package fhes.cat.services;

import fhes.cat.dto.NotificacioEstatDTO;

public interface NotificarEstats {

	public void enviarNotificacioEstat(NotificacioEstatDTO estat);
	public void notificaEstat(String affectedSOPInstanceUID, String patientID, String modality, String estado, String StudyInstanceUID, String device,String aEAsociado, String patientName, String studyDescription);
	public void notificaEstat(String requestedSOPInstanceUID, String estado, String patientName, String studyDescription);
	public void notificaEstat(String modality, String device, String accessionNumber, String studyInstanceUid, String nhc, int origen, 
							  String nomCentre, String pathImages, String affectedSOPInstanceUID, String aEAsociado, String patientName, String idCita, 
							  String informe, String studyDate, String studyTime, String studyDescription);
}
