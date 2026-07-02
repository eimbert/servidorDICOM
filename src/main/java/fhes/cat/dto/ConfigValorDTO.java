package fhes.cat.dto;


import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class ConfigValorDTO {
		
	@JsonProperty("id")
	private Integer id;
	
	@JsonProperty("camp")
	private String camp;
	
	@JsonProperty("valor1")
	private String valor1;
	
	@JsonProperty("valor2")
	private String valor2;
	
	@JsonProperty("tipus")
	private String tipus;
	
	@JsonProperty("descripcio")
	private String descripcio;
	
	@JsonProperty("encriptat")
	private Integer encriptat;
	
	public ConfigValorDTO() {

	}
	
}
