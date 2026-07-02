package fhes.cat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BusquedaPacienteDTO {
	String modality;
	String device;
	String accessionNumber; //(0008,0050) SH []
	String patientName; //(0010,0010) PN []
	String patientFirstSurname; 
	String patientSecondSurname; 
	String patientID; //(0010,0020) LO [] 
	String patientBirthDate; //(0010,0030) DA [] 
	String patientSex; //(0010,0040) CS [] 
	String studyInstanceUID;
	String idCita;
	int informar;
	
	public BusquedaPacienteDTO() {
		
	}
	public BusquedaPacienteDTO(String modality, String device) {
		this.modality = modality;
		this.device = device;
	}
}
