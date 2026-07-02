package fhes.cat.dmdcm4che3.utilities;

import java.util.List;

import org.springframework.web.client.RestTemplate;

import fhes.cat.dto.BusquedaPacienteDTO;
import fhes.cat.dto.RestApiResponse;
import lombok.extern.slf4j.Slf4j;
import services.impl.BaseServiceImpl;

@Slf4j
public class ObtenerUrlPorEntorno extends BaseServiceImpl{
	
	@SuppressWarnings("unchecked")
	public <T> List<T>  consulta(String pre, String pro, Object obj) {
		RestTemplate restTemplate = new RestTemplate();
		
//		log.info("url: {}", pre);
//		log.info("json {}", mapToObj(RestApiResponse.class, restTemplate.postForObject(pre, obj, Object.class)));
//		log.info("resultado: {}", mapToList(BusquedaPacienteDTO.class, mapToObj(RestApiResponse.class, restTemplate.postForObject(pre, obj, Object.class)).getData()));
		
		
		return (List<T>) mapToList(BusquedaPacienteDTO.class, mapToObj(RestApiResponse.class, restTemplate.postForObject(pre, obj, Object.class)).getData());
		
		
	}

}
