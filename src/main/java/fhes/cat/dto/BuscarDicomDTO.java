package fhes.cat.dto;

import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BuscarDicomDTO {

	private int idBusqueda;
	private String accessionNumber;
	private String studyDate;
	private String patientID;
	private String patientName;
	private String patientBirthDate;
	private String modality;
}
