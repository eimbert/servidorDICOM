package fhes.cat.dmdcm4che3.rsp;

import org.dcm4che2.data.Tag;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.DimseRSPHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fhes.cat.config.APIConstants;
import fhes.cat.dmdcm4che3.utilities.DisplayTagVisitor;
import fhes.cat.dto.ResponseBuscarDicomDTO;
import restapiresponse.RestApiResponse;
import services.SocketService;
import services.impl.BaseServiceImpl;
import services.impl.SocketServiceImpl;

public class DimseRSPHandlerImpl extends DimseRSPHandler{

	private static final Logger log = LoggerFactory.getLogger(DimseRSPHandlerImpl.class);
	private BaseServiceImpl baseService = new BaseServiceImpl();
	private SocketService socketService = new SocketServiceImpl();
	ObjectMapper mapper = new ObjectMapper();
	
	public DimseRSPHandlerImpl(int msgId) {
		super(msgId);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onDimseRSP(Association as, Attributes cmd, Attributes data) {
//        if (stopOnPending || !Status.isPending(cmd.getInt(Tag.Status, -1)))
//            stopTimeout(as);
		ResponseBuscarDicomDTO studio = null;
		try {
			log.info("Resposta \nAss: {}  \ncmd: {} \ndata:{}", as, cmd, data);
			
			//if(cmd.getInt(Tag.Status, 0) >= 0) {
			if(data == null) {
				studio = new ResponseBuscarDicomDTO(cmd.getInt(Tag.MessageIDBeingRespondedTo, -1), 
						"", "", "", "", "", "", "", cmd.getInt(Tag.Status, 0));
			}else {
				studio = new ResponseBuscarDicomDTO(cmd.getInt(Tag.MessageIDBeingRespondedTo, -1), 
					data.getString(Tag.PatientName), data.getString(Tag.PatientID), data.getString(Tag.PatientBirthDate), 
					data.getString(Tag.StudyInstanceUID), data.getString(Tag.AccessionNumber), data.getString(Tag.StudyDate),
					data.getString(Tag.ModalitiesInStudy), cmd.getInt(Tag.Status, 0));
//				try {
//					log.info("\n^^^^^^ DISPLAY DATA ^^^^^^\n");
//					data.accept(new DisplayTagVisitor(), true); //visualiza todos los Atributos de data
//				} catch (Exception e) {
//					log.info("\n^^^^^^^^^^^^^^^^^^^^^^ Error Tags {}", e);
//				}
			}
			
			//Llamar al back-end
			RestTemplate restTemplate = new RestTemplate();	
			String url = APIConstants.urlResultadoCFind;
		
			baseService.mapToObj(RestApiResponse.class, restTemplate.postForObject(url, studio, Object.class));
		}catch(Exception e) {
			log.info("Error: {}", e.getMessage());
        	try {
				socketService.notificarMissatge(SocketServiceImpl.MISSATGE_CRITICAL, "Error C-FIND", "Error informant resultat cerca "+ mapper.writeValueAsString(studio) );
			} catch (JsonProcessingException e1) {
				log.info(e1.getMessage());
			}
		}
		//}
    }
}
