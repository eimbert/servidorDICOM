package fhes.cat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DicomServerDTO {

	private int portNumber;
	private String serverAET;
	private String host;
	private String descripción;
}
