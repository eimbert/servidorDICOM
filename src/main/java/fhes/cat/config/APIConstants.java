package fhes.cat.config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fhes.cat.dto.ConfigValorDTO;
import fhes.cat.dto.MapeosDicomDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class APIConstants {

	
	//tipos de servidores
	public static final String STORAGE_INT = "SERVER_STORAGE_INTERN";
	public static final String STORAGE_EXT = "SERVER_STORAGE_EXTERN";
	public static final String STORAGE_C_MOVE = "SERVER_STORAGE_C-MOVE";
	public static final String STORAGE_C_MOVE_PDF = "SERVER_STORAGE_C-MOVE_PDF";
	//public static final String STORAGE_C_MOVE_ECG_TO_IMG = "STORAGE_C_MOVE_ECG_TO_IMG";
	
	public static final String MPPS = "SERVER_MPPS";
	public static final String FIND = "SERVER_FIND";
	
	public static final String SERVER_HL7 = "SERVER_HL7";
	
	public static final int IMATGE_INTERNA = 0;
	public static final int IMATGE_EXTERNA = 1;
	public static final int IMATGE_RECUPERADA_DEL_PACS = 2;
	public static final int ECG_RECUPERADO_DEL_PACS_TO_PDF = 3;
	
	public static String urlConfiguracioDev;
	public static String urlConfiguracioPre;
	public static String urlConfiguracioPro;
	 
	public static String urlEntono;
	public static String urlNewImg;
	public static String pathDicomStorage;
	public static List<ConfigValorDTO> capacidadTransferencia = new ArrayList<>();
	public static List<ConfigValorDTO> capacidadTransferenciaFind = new ArrayList<>();
	public static List<ConfigValorDTO> capacidadTransferenciaMpps = new ArrayList<>();
	public static List<ConfigValorDTO> presentationContext = new ArrayList<>();
	public static String urlResultadoCFind;
	public static List<ConfigValorDTO> servidores = new ArrayList<>();
	public static List<MapeosDicomDTO> listaMapeosTags = new ArrayList<MapeosDicomDTO>();
	public static String modalities[] = {"ECG", "US", "MR", "CT", "CR", "DX", "NM", "MG", "PE", "PX", "RF", "SR", "PR", "OT", "XA"};
	
	public static String maquinasQueContenganEnElNombreParaNoEnviarHC3 = "NOHC3";
	//PACS
	public static String aTitlePacs;// = "COLOMA";  
	public static String ipPacs; // = "172.16.8.105"; 
	public static int portPacs; // = 104;
	
	//RSYNCBRIDGE
	public static String aTitleRsync;
	public static String ipRsync; 
	public static int portRsync; 
	
	//ServidorDicomFHES
//	public static String aTitlePacs = "SDFHES";  
//	public static String ipPacs = "172.20.4.97";  
//	public static int portPacs = 211;
	
	//SrvUsuarisPre
//	public static String aTitlePacs = "MICRODICOM";  
//	public static String ipPacs = "172.16.0.102";  
//	public static int portPacs = 555;
	
	public static ConfigValorDTO localServerSender; 
	
	
	
	public static String obtenerAETitleasociado(String as) {
		Pattern pattern = Pattern.compile(".*<-([^\\(]+)");
		Matcher matcher = pattern.matcher(as+"");
		String aEAsociado;
		
		if (matcher.find()) {
            String textoExtraido = matcher.group(1).trim();
            aEAsociado = textoExtraido.toUpperCase();
        } else {
        	aEAsociado = ""; 
        }
		
		return aEAsociado;
	}

}
