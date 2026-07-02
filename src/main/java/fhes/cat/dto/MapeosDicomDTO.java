package fhes.cat.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MapeosDicomDTO {

	private int id;
	@JsonProperty("Modality")
	private String modality;
	private String tag;
	private int tagValue;
	@JsonProperty("VR")
	private String VR;

	
	
//	public static List<MapeosDicomDTO> getterPruebas(){
//		List<MapeosDicomDTO> lista = new ArrayList<MapeosDicomDTO>();
//		
//		lista.add(new MapeosDicomDTO(1, "RM","AccessionNumber", 524368, "SH"));
//		lista.add(new MapeosDicomDTO(2, "RM","PatientID", 1048608, "LO"));
//		
//		return lista;
//	}



	public MapeosDicomDTO(int id, String mR, String tag, int tagValue, String vR) {
		super();
		
		this.id = id;
		this.modality = mR;
		this.tag = tag;
		this.tagValue = tagValue;
		this.VR = vR;
	}



	public MapeosDicomDTO() {
		super();
		
	}
}
