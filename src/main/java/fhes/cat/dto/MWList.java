package fhes.cat.dto;

import java.util.List;

import fhes.cat.enums.Modality;
import fhes.cat.enums.Ubicacion;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MWList {

	private String nombre;
	private Modality modality;
	private Ubicacion ubicacion; //Todos los dispositivos de una modalidad y la ubicacion indicada
	private String nombreDispositivo; //Solo para el dispositivo con el nombre coincidente
	private List<PacienteDTO> pacientes;

}
