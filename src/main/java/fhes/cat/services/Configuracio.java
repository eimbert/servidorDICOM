package fhes.cat.services;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fhes.cat.config.APIConstants;
import fhes.cat.configuration.ConfigurationFallbackLoader;
import fhes.cat.dmdcm4che3.DicomServer;
import fhes.cat.dmdcm4che3.HL7Server;
import fhes.cat.dto.ConfigValorDTO;
import fhes.cat.dto.MapeosDicomDTO;
import restapiresponse.RestApiResponse;
import services.impl.BaseServiceImpl;
import services.impl.RestApiServiceImpl;
import services.impl.SocketServiceImpl;

@Service
public class Configuracio extends BaseServiceImpl implements InitializingBean {
	
	private static final Logger log = LoggerFactory.getLogger(Configuracio.class);
	
	@Autowired
	private DicomServer mwlist;
	
	@Autowired
	private HL7Server hl7Server;

	@Autowired
	private ConfigurationFallbackLoader configurationFallbackLoader;
	
	ObjectMapper mapper = new ObjectMapper();
	
	private static String urlConfiguracioDev;
	
	private static  String urlConfiguracioProduccio;
		
	@Value("${dev.urlConfiguracio}")
	private void setUrlConfiguracioDev(String urlConfiguracioDev) {
		Configuracio.urlConfiguracioDev = urlConfiguracioDev;
	}
		
	@Value("${produccio.urlConfiguracio}")
	private void setUrlConfiguracioProduccio(String urlConfiguracioProduccio) {
		Configuracio.urlConfiguracioProduccio = urlConfiguracioProduccio;
	}
	
	@PostConstruct
	public void ConfiguracioPost() {
		String username = System.getProperty("user.name");
        log.info("\nEl usuario que ejecuta la aplicación es: " + username + " y tiene acceso a las siguientes variables de entorno:\n");
		//Map<String, String> envVariables = System.getenv();
//		for (Map.Entry<String, String> entry : envVariables.entrySet()) {
//            log.info(entry.getKey() + ": " + entry.getValue());
//        }
		
		carregarConfiguracio();
	
		APIConstants.servidores.forEach(server ->{
			log.info("Servidor: {}", server.getCamp());
			log.info("(por System no log) Servidor: " + server.getCamp());
			if(server.getCamp().equals(APIConstants.STORAGE_INT)) {
				mwlist.createStorageServer(Integer.parseInt(server.getValor1()), server.getValor2(), true, false, APIConstants.IMATGE_INTERNA);
			}if(server.getCamp().equals(APIConstants.STORAGE_EXT)) {
				mwlist.createStorageServer(Integer.parseInt(server.getValor1()), server.getValor2(), true, false, APIConstants.IMATGE_EXTERNA);
			}else if(server.getCamp().equals(APIConstants.STORAGE_C_MOVE)) {
				mwlist.createStorageServer(Integer.parseInt(server.getValor1()), server.getValor2(), true, false, APIConstants.IMATGE_RECUPERADA_DEL_PACS);
			}else if(server.getCamp().equals(APIConstants.STORAGE_C_MOVE_PDF)) {//para los electros que hay que pasar a pdf e incluir en la peticion externa
				mwlist.createStorageServer(Integer.parseInt(server.getValor1()), server.getValor2(), true, false, APIConstants.ECG_RECUPERADO_DEL_PACS_TO_PDF);
			}else if(server.getCamp().equals(APIConstants.FIND)) {
				mwlist.createFindServer(Integer.parseInt(server.getValor1()), server.getValor2());
			}else if(server.getCamp().equals(APIConstants.MPPS)) {
				mwlist.createMPPServer(Integer.parseInt(server.getValor1()), server.getValor2());
			}if(server.getCamp().equals(APIConstants.SERVER_HL7)) {
				hl7Server.ServidorHL7(Integer.parseInt(server.getValor1()));
			}
		});
		
		try {
			log.info("Mapeos: {}", mapper.writeValueAsString(APIConstants.listaMapeosTags));
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
				
	}
	
	// Llista amb les variables de configuració
	public static List<ConfigValorDTO> listConfiguracio = new ArrayList<ConfigValorDTO>();

	
	// Variables de configuració de l'aplicació
	public static final String SERVIDOR_DICOM_FHES = "SERVIDOR DICOM FHES";
	public static final String CAPACIDAD_TRANSFERENCIA = "CAPACIDAD_TRANSFERENCIA";
	public static final String PRESENTATION_CONTEXT = "PRESENTATION_CONTEXT";
	public static final String CAPACIDAD_TRANSFERENCIA_FIND = "CAPACIDAD_TRANSFERENCIA_FIND";
	public static final String CAPACIDAD_TRANSFERENCIA_MPPS = "CAPACIDAD_TRANSFERENCIA_MPPS";
	public static final String TIPUS_SERVER = "SERVER";
	public static final String URL_MWL = "URL_MWL";
	public static final String URL_NEW_IMG = "URL_NEW_IMG";
	
	public static final String ATITLE_PACS = "ATITLE_PACS";
	public static final String IP_PACS = "IP_PACS";
	public static final String PORT_PACS = "PORT_PACS";
	
	public static final String ATITLE_RSYNC = "ATITLE_RSYNC";
	public static final String IP_RSYNC = "IP_RSYNC";
	public static final String PORT_RSYNC = "PORT_RSYNC";
	
	public static final String LOCAL_SERVER_SENDER = "LOCAL_SERVER_SENDER";
	public static final String BASE_PATH_IMATGES = "BASE_PATH_IMATGES";
	public static final String URL_RESULTADO_CFIND = "URL_RESULTADO_C-FIND";
	
	
	private RestApiServiceImpl restApiService = new RestApiServiceImpl();
	
	private SocketServiceImpl socketService = new SocketServiceImpl();
	
	
	public ConfigValorDTO buscarConfiguracio(String camp) {
		
		for(ConfigValorDTO cv : listConfiguracio) {
			if(cv.getCamp() != null && cv.getCamp().trim().contentEquals(camp)) {
				return cv;
			}
		}
		socketService.notificarMissatge(SocketServiceImpl.MISSATGE_INFO, SERVIDOR_DICOM_FHES, "Valor de configuració no trobat: "+camp+"");
		return null;
	}
	
	
	
	public void carregarConfiguracio() {
		//Carreguem la configuració de l'aplicació
		log.info("Cargando configuración");
		log.info("Variable PROFILE_SERVER: "+obtenirVariableEntorn("PROFILE_SERVER"));
		boolean remoteConfigurationLoaded = false;
		try { 
			log.info("Carregar configuració: "+obtainString(urlConfiguracioDev, urlConfiguracioProduccio));
			
			String urlConfiguracio = obtainString(urlConfiguracioDev, urlConfiguracioProduccio);
			if(urlConfiguracio == null) {
				urlConfiguracio = urlConfiguracioDev;
			}
			
		    RestApiResponse respostaConfiguracio = restApiService.executeGet(urlConfiguracio);
//			RestApiResponse respostaConfiguracio = restApiService.executeGet(urlConfiguracioDev);
			log.info("Resposta: "+respostaConfiguracio.getExitCode()+ " - "+mapper.writeValueAsString(respostaConfiguracio.getData()));
		    if(respostaConfiguracio.getExitCode() == 0) {
		    	List<ConfigValorDTO> listConfig = mapToList(ConfigValorDTO.class, respostaConfiguracio.getData());
		    	if(listConfig.size() > 0) {
		    		// La llista que s'ha carregat està informada, reemplacem
		    		listConfiguracio = listConfig;
		    		// Configurem els llistats del socket
		    		configurarLlistatsSocket();
					remoteConfigurationLoaded = true;
		    		
		    	}
		    }
		} catch (Exception e) {
			log.warn("Configuracion DICOM BBDD remota no disponible: "+e.getMessage());
			try {
				socketService.notificarMissatge(SocketServiceImpl.MISSATGE_INFO, SERVIDOR_DICOM_FHES, "Configuracion DICOM BBDD remota no disponible: "+e.getMessage());
			} catch (Exception notificationError) {
				log.warn("No se pudo notificar el error de configuracion remota", notificationError);
			}
		}

		if (!remoteConfigurationLoaded) {
			try {
				listConfiguracio = configurationFallbackLoader.load();
				configurarLlistatsSocket();
				log.warn("Configuracion cargada desde local JSON: {}", configurationFallbackLoader.getConfigurationFile());
			} catch (Exception fallbackError) {
				log.error("No se pudo cargar la configuracion remota ni el fallback JSON", fallbackError);
				throw new IllegalStateException("Configuracion DICOM no disponible", fallbackError);
			}
		}
	}
	
	private void configurarLlistatsSocket() {
		log.info("******************** BUSCANDO CONFIG *****************************");
		APIConstants.capacidadTransferencia.clear();
		APIConstants.presentationContext.clear();
		APIConstants.capacidadTransferenciaFind.clear();
		APIConstants.capacidadTransferenciaMpps.clear();
		APIConstants.servidores.clear();
		APIConstants.listaMapeosTags.clear();
		
		for(ConfigValorDTO cv : listConfiguracio) {
			if(cv.getCamp() != null && cv.getCamp().contentEquals(CAPACIDAD_TRANSFERENCIA)) {
				APIConstants.capacidadTransferencia.add(cv);
			} else if(cv.getCamp() != null && cv.getCamp().contentEquals(PRESENTATION_CONTEXT)) {
				APIConstants.presentationContext.add(cv);
			} else if(cv.getCamp() != null && cv.getCamp().contentEquals(CAPACIDAD_TRANSFERENCIA_FIND)) {
				APIConstants.capacidadTransferenciaFind.add(cv);
			} else if(cv.getCamp() != null && cv.getCamp().contentEquals(CAPACIDAD_TRANSFERENCIA_MPPS)) {
				APIConstants.capacidadTransferenciaMpps.add(cv); 
			} else if(cv.getTipus() != null && cv.getTipus().contentEquals(TIPUS_SERVER)) {
				APIConstants.servidores.add(cv);
			} else if(cv.getTipus() != null && Arrays.stream(APIConstants.modalities).anyMatch(modality -> cv.getTipus().equals(modality))){
				APIConstants.listaMapeosTags.add(new MapeosDicomDTO(cv.getId(), cv.getTipus(), cv.getCamp(), Integer.parseInt(cv.getValor1()), cv.getValor2()));
			} else if(cv.getCamp() != null && cv.getCamp().contentEquals(URL_MWL)) {
				APIConstants.urlEntono = cv.getValor1();
			}else if(cv.getCamp() != null && cv.getCamp().contentEquals(URL_NEW_IMG)) {
				APIConstants.urlNewImg = cv.getValor1();
			}else if(cv.getCamp() != null && cv.getCamp().contentEquals(ATITLE_PACS)) {
				APIConstants.aTitlePacs = cv.getValor1();
			}else if(cv.getCamp() != null && cv.getCamp().contentEquals(PORT_PACS)) {
				APIConstants.portPacs = Integer.parseInt(cv.getValor1());
			}else if(cv.getCamp() != null && cv.getCamp().contentEquals(IP_PACS)) {
				APIConstants.ipPacs = cv.getValor1();
			}else if(cv.getCamp() != null && cv.getCamp().contentEquals(LOCAL_SERVER_SENDER)) {
				APIConstants.localServerSender = cv;
			}else if(cv.getCamp() != null && cv.getCamp().contentEquals(BASE_PATH_IMATGES)) {
				APIConstants.pathDicomStorage = cv.getValor1();
			}else if(cv.getCamp() != null && cv.getCamp().contentEquals(URL_RESULTADO_CFIND)) {
				APIConstants.urlResultadoCFind = cv.getValor1();
			}else if(cv.getCamp() != null && cv.getCamp().contentEquals(ATITLE_RSYNC)) {
				APIConstants.aTitleRsync = cv.getValor1();
			}else if(cv.getCamp() != null && cv.getCamp().contentEquals(IP_RSYNC)) {
				APIConstants.ipRsync = cv.getValor1();
			}else if(cv.getCamp() != null && cv.getCamp().contentEquals(PORT_RSYNC)) {
				APIConstants.portRsync = Integer.parseInt(cv.getValor1());
			}
		}
	}

}
