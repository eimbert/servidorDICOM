package fhes.cat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ResponseBuscarDicomDTO {

	private int idBusqueda;
	private String patientName;
	private String patientID;
	private String patientBirthDate;
	private String studyInstanceUID;
	private String accessionNumber;
	private String studyDate;
	private String modality;
	private int status;
	
	
}
