package fhes.cat.dmdcm4che3.impl;

import java.util.List;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import fhes.cat.config.APIConstants;
import fhes.cat.dmdcm4che3.WorkListCustom;
import fhes.cat.dto.BusquedaPacienteDTO;
import fhes.cat.dto.RestApiResponse;
import services.impl.BaseServiceImpl;


public class WorkListCustomImpl extends WorkListCustom{

	private static final Logger log = LoggerFactory.getLogger(WorkListCustomImpl.class);
	
	private BaseServiceImpl baseService = new BaseServiceImpl();	
	
	public WorkListCustomImpl(String... sopClasses) {
		super(sopClasses);
	}

	@Override
	protected boolean buscarPacientes(BusquedaPacienteDTO busquedaPacientes) {
		log.info("**********************************************Llamar a back para buscar paciente *********************************************************");
                                 
		log.info("pre {}", APIConstants.urlEntono);
		List<BusquedaPacienteDTO> resultadoBusqueda = null;
		try {
			String url = APIConstants.urlEntono;
			RestTemplate restTemplate = new RestTemplate();
			resultadoBusqueda = baseService.mapToList(BusquedaPacienteDTO.class, baseService.mapToObj(RestApiResponse.class, restTemplate.postForObject(url, busquedaPacientes, Object.class)).getData());
		}catch(Exception e) {
			return false;
		}
		
		
		//log.info("Resultado: {}", resultadoBusqueda );
		//log.info("Patient id {}", resultadoBusqueda.get(0).getPatientID());
		
		this.clearList();
		
		
		if(resultadoBusqueda == null || resultadoBusqueda.get(0) == null || resultadoBusqueda.isEmpty()) {
			//Attributes attributes = new Attributes();
			return false;
		}
		llenarPacientes(resultadoBusqueda, busquedaPacientes.getDevice());
          
		return true;
	}

}
