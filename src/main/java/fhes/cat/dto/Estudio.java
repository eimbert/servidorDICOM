package fhes.cat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Estudio {

	private String fechaEstudio;
	private String horaEstudio;
	private String accesionNumber;
	private String facultativo;
	private String descripcionEstudio;
	private String idInstanciaEstudio; //Identificador único para el estudio. Usando DECG prefix 1.3.46.670589.32 y Time Clock Random Number
}
