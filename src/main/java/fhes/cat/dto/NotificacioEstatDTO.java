package fhes.cat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificacioEstatDTO  {
	
	private String estat;
	private String modality;
	private String device;
	
	@JsonProperty("AETitle")
	private String AETitle;
	private String accessionNumber;
	private String studyInstanceUid;
	private String nhc;
	private int origen;
	private String pathImages;
	private String nomCentre;
	private String affectedSOPInstanceUID;
	private String idCita;
	private String informe;
	private String studyDate;
	private String acquisitionDate;
	
	@JsonProperty("nomPacient")
	private String nomPacient;
	
	@JsonProperty("studyDescription")
	private String studyDescription;
	
	
}
