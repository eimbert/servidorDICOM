package fhes.cat.dto;

import fhes.cat.enums.Modality;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DispositivoDTO {	
	private Modality modality;
	private String nombre;
	private String descripción;
}
