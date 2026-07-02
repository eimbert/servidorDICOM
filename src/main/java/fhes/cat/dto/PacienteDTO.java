package fhes.cat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PacienteDTO {
	private String nombre;
	private String primerApellido;
	private String segundoApellido;
	private int idPaciente;
	private String fechaNacieminto;
	private String sexo;
	private String etnia;
	private Estudio estudio;
}
