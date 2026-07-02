package fhes.cat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RespostaEnviamentImatgesDicomDTO {
	
    private int id;
    private int imatgesOk;
    private int imatgesError;
    private int numFrames;
    private int numFramesKo;
    private boolean error;
    private String descripcio;
    private String modality;
	private String studyDateTime;
    
	public RespostaEnviamentImatgesDicomDTO() {
		super();
		this.id = 0;
		this.imatgesOk = 0;
		this.imatgesError = 0;
		this.error = false;
		this.descripcio = "";
		this.numFrames = 0;
		this.numFramesKo = 0;
	}
	
	public void addOK() {
		this.imatgesOk++;
	}
	
	public void addKO() {
		this.imatgesError++;
	}
	
	public void addFrames(int num) {
		if (num == 0) num++;
		this.numFrames+=num;
	}
	
	public void addFramesKo(int num) {
		if (num == 0) num++;
		this.numFramesKo+=num;
	}
    
    
}
