package fhes.cat.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoredToPacs {

	private int id;
	private List<String> origens;
	private String pathImagen;
	private Object dadesAddicionals;
	private Boolean imatgeExterna;
	private Boolean borrarFicheroDicom;
}
