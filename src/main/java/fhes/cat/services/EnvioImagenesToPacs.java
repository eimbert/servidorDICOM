package fhes.cat.services;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import fhes.cat.dto.BajarDicomExtDTO;
import fhes.cat.dto.BuscarDicomDTO;
import fhes.cat.dto.StoredToPacs;

public interface EnvioImagenesToPacs {
	public void crearLista(int delayEntreImagenes, int horaInicio, StoredToPacs stored, Map<String, String> mapa, boolean isForPacs, boolean esExterna);
	//public void crearListaDirect(int delayEntreImagenes, int horaInicio, StoredToPacs stored, Map<String, String> mapa, boolean isForPacs);
	//public void leerTags(String pathImatge);
	//public void bajarEstudioDelPacs(String ID);
	public void bajarEstudioDelPacs(String ID, String localAETitle);
	public void bajarEstudioDelPacs(BajarDicomExtDTO attrDicom, String localAETitle);
	public void buscarImagenesDelPacs(BuscarDicomDTO dicom);
	public void borrarImagenDicom();
	public String convertECGDicomToBase64(File file) throws IOException;
	public String convertTomoDicomToBase64(File file) throws IOException;
	

}
