package fhes.cat.controller;



import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fhes.cat.config.APIConstants;
import fhes.cat.dto.BajarDicomDTO;
import fhes.cat.dto.BajarDicomExtDTO;
import fhes.cat.dto.BuscarDicomDTO;
import fhes.cat.dto.FileDTO;
import fhes.cat.dto.StoredToPacs;
import fhes.cat.dto.TAGListDTO;
import fhes.cat.services.EnvioImagenesToPacs;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import restapiresponse.RestApiResponse;
import services.SocketService;
import services.impl.SocketServiceImpl;


@RestController
@RequestMapping("dicom")
public class SendStoredDicomToPacs {

	private static final Logger log = LoggerFactory.getLogger(SendStoredDicomToPacs.class);
	private SocketService socketService = new SocketServiceImpl();

	@Autowired
	EnvioImagenesToPacs sendFiles;
	
	ObjectMapper mapper = new ObjectMapper();
	
	@PostMapping(value = "wsEnviarDicomPacs", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Endpoint per enviar imatges guardades a una carpeta to PACS", response = RestApiResponse.class)
	@CrossOrigin("*")
	public @ResponseBody RestApiResponse enviarDicomPacs(HttpServletRequest request, HttpServletResponse response, 
			@ApiParam(name="EjemploDTO", required= true, value = "EjemploDTO") @RequestBody StoredToPacs storedToPacs) throws JsonProcessingException {
		
		//log.info("Recibido JSON StoredToPacs {}", mapper.writeValueAsString(storedToPacs));	
		//log.info("Recibido JSON Origens {}", mapper.writeValueAsString(storedToPacs.getOrigens()));
		String dadesAdicional = mapper.writeValueAsString(storedToPacs.getDadesAddicionals());
		Map<String, String> mapa = null;
		 
		try {
			mapa = mapper.readValue(dadesAdicional, Map.class); // Convertir el JSON en un Map<String, String>
        } catch (Exception e) {
        	socketService.notificarMissatge(SocketServiceImpl.MISSATGE_CRITICAL, "Error SDFHES", "Error en mapejar dadesAdicionals en DICOM - Enviar imatges "+e.getMessage());
            e.printStackTrace();
            return null;
        }
		
		int delayEntreImagenes = storedToPacs.getImatgeExterna() ? 50: 300;
		
		final Map<String, String> mapaFinal = mapa; // Declara una variable final
		Thread asyncThread = new Thread(() -> {
//			if(!storedToPacs.getImatgeExterna())
				sendFiles.crearLista(0, delayEntreImagenes, storedToPacs, mapaFinal, true, storedToPacs.getImatgeExterna());
//			else
//				sendFiles.crearListaDirect(0, 500, storedToPacs, mapaFinal, true);
        });

        asyncThread.start();
				
		return new RestApiResponse();
	}
	
	//Enviar directamente a RSYNC ya que el PACS no envia cambios, solamnete envia la primera vez
	@PostMapping(value = "wsEnviarDicomRsyncbridge", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Endpoint per enviar imatges guardades a una carpeta to PACS", response = RestApiResponse.class)
	@CrossOrigin("*")
	public @ResponseBody RestApiResponse enviarDicomRsyncbridge(HttpServletRequest request, HttpServletResponse response, 
			@ApiParam(name="EjemploDTO", required= true, value = "EjemploDTO") @RequestBody StoredToPacs storedToPacs) throws JsonProcessingException {
		
		//log.info("Recibido JSON StoredToPacs para enviar Rsync{}", mapper.writeValueAsString(storedToPacs));
		//log.info("Recibido JSON Origens {}", mapper.writeValueAsString(storedToPacs.getOrigens()));
		String dadesAdicional = mapper.writeValueAsString(storedToPacs.getDadesAddicionals());
		Map<String, String> mapa = null;
		 
		try {
			mapa = mapper.readValue(dadesAdicional, Map.class); // Convertir el JSON en un Map<String, String>
        } catch (Exception e) {
        	socketService.notificarMissatge(SocketServiceImpl.MISSATGE_CRITICAL, "Error SDFHES", "Error en mapejar dadesAdicionals en DICOM - Enviar imatges "+e.getMessage());
            e.printStackTrace();
            return null;
        }
		int intDelayEntreImagenes = 50;
		final Map<String, String> mapaFinal = mapa; // Declara una variable final
		Thread asyncThread = new Thread(() -> {
			sendFiles.crearLista(0, intDelayEntreImagenes, storedToPacs, mapaFinal, false, false);
        });

        asyncThread.start();
				
		return new RestApiResponse();
	}
	
	@PostMapping(value = "wsRecuperarDicom", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Endpoint per recuperar imatges guardades en el PACS por StudyInstanceUID ", response = RestApiResponse.class)
	@CrossOrigin("*")
	public @ResponseBody RestApiResponse recuperarDicom(HttpServletRequest request, HttpServletResponse response, 
			@ApiParam(name="EjemploDTO", required= true, value = "EjemploDTO") @RequestBody BajarDicomDTO studyInstanceUID) throws JsonProcessingException {
		
		//log.info("studyInstanceUID {}", studyInstanceUID.getStudyInstanceUid());	
		
		sendFiles.bajarEstudioDelPacs(studyInstanceUID.getStudyInstanceUid(), APIConstants.localServerSender.getValor2());
				
		return new RestApiResponse();
	}
	
	@PostMapping(value = "wsRecuperarDicomExt", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Endpoint per recuperar imatges guardades en el PACS por StudyInstanceUID ", response = RestApiResponse.class)
	@CrossOrigin("*")
	public @ResponseBody RestApiResponse recuperarDicomExt(HttpServletRequest request, HttpServletResponse response, 
			@ApiParam(name="EjemploDTO", required= true, value = "EjemploDTO") @RequestBody BajarDicomExtDTO studyInstanceExtUID) throws JsonProcessingException {
		
		//log.info("studyInstanceUID {} StudyDate {} StudyTime {}", studyInstanceExtUID.getStudyInstanceUid(), studyInstanceExtUID.getStudyDate(), studyInstanceExtUID.getStudyTime());	
		
		sendFiles.bajarEstudioDelPacs(studyInstanceExtUID, APIConstants.localServerSender.getValor2());
				
		return new RestApiResponse();
	}
	

	
	
	@PostMapping(value = "wsBuscarDicom", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Endpoint per recuperar imatges guardades en el PACS por StudyInstanceUID ", response = RestApiResponse.class)
	@CrossOrigin("*")
	public @ResponseBody RestApiResponse busacrDicom(HttpServletRequest request, HttpServletResponse response, 
			@ApiParam(name="EjemploDTO", required= true, value = "EjemploDTO") @RequestBody BuscarDicomDTO dicom) throws JsonProcessingException {
		
				
		sendFiles.buscarImagenesDelPacs(dicom);
				
		return new RestApiResponse();
	}
	
	
	@PostMapping(value = "wsBorrarImagenDicom", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Endpoint per recuperar imatges guardades en el PACS por StudyInstanceUID ", response = RestApiResponse.class)
	@CrossOrigin("*")
	public @ResponseBody RestApiResponse borrarDicom(HttpServletRequest request, HttpServletResponse response, 
			@ApiParam(name="EjemploDTO", required= true, value = "EjemploDTO") @RequestBody BuscarDicomDTO dicom) throws JsonProcessingException {
		
				
		sendFiles.borrarImagenDicom();
				
		return new RestApiResponse();
	}
	
	@PostMapping(value = "wsEcgToB64", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Endpoint per recuperar imatges guardades en el PACS por StudyInstanceUID ", response = RestApiResponse.class)
	@CrossOrigin("*")
	public 	@ResponseBody RestApiResponse ecgTob64(HttpServletRequest request, HttpServletResponse response, 
			@ApiParam(name="FileDTO", required= true, value = "FileDTO") @RequestBody FileDTO file) throws JsonProcessingException {
		
		RestApiResponse rest =  new RestApiResponse();
		File dicomFile = new File(file.getPath());
		try {
			rest.setData(sendFiles.convertECGDicomToBase64(dicomFile));
		} catch (IOException e) {
			e.printStackTrace();
		}
				
		return rest;
	}
	
	@PostMapping(value = "wsTomoToB64", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Endpoint per recuperar imatges guardades en el PACS por StudyInstanceUID ", response = RestApiResponse.class)
	@CrossOrigin("*")
	public 	@ResponseBody RestApiResponse recuperaInformeToB64(HttpServletRequest request, HttpServletResponse response, 
			@ApiParam(name="FileDTO", required= true, value = "FileDTO") @RequestBody FileDTO file) throws JsonProcessingException {
		
		RestApiResponse rest =  new RestApiResponse();
		File dicomFile = new File(file.getPath());
		try {
			rest.setData(sendFiles.convertTomoDicomToBase64(dicomFile));
		} catch (IOException e) {
			e.printStackTrace();
		}
				
		return rest;
	}

	@GetMapping(value = "wsObtenerTags", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Endpoint Obtener los Tags", response = RestApiResponse.class)
	@CrossOrigin("*")
	public @ResponseBody RestApiResponse obtenerTags(HttpServletRequest request, HttpServletResponse response) throws JsonProcessingException {
		
		log.info("Solicitar tags");	
			
		return new RestApiResponse (0, "OK", getTagInfo(Tag.class));
		
	}
	
	@GetMapping(value = "wsObtenerTagsVr", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Endpoint per enviar imatges guardades a una carpeta to PACS", response = RestApiResponse.class)
	@CrossOrigin("*")
	public @ResponseBody RestApiResponse obtenerTagsVr(HttpServletRequest request, HttpServletResponse response) throws JsonProcessingException {
		
		log.info("Solicitar tags VR");	
		VR[] allValues = VR.values();
		List <String> lista = new ArrayList<String>();
		
		for (VR vr : allValues) {
			lista.add(vr.name());
        }
		
		return new RestApiResponse (0, "OK", lista);
		
	}
	
	 public List<TAGListDTO> getTagInfo(Class<?> tagClass) {
		Field[] fields = tagClass.getDeclaredFields();
		List<TAGListDTO> list = new ArrayList<TAGListDTO>();
		
        for (Field field : fields) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                try {
                	if(field.get(null) instanceof Integer && (int)field.get(null) > 0)
                		list.add(new TAGListDTO(field.getName(), field.get(null))); 
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return list;
    }
	 	
	
}
