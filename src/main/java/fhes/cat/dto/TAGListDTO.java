package fhes.cat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TAGListDTO {
	
	private String nombreTag;
	private int valor;
	
	public TAGListDTO(String nombreTag, Object valor) {
		super();
		try {
			this.nombreTag = nombreTag;
			this.valor = Integer.parseInt(valor+"");
			
		}catch(Exception e) {
			
		}
	}

	
}
