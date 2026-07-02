package fhes.cat.dmdcm4che3.impl;

import java.io.IOException;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Dimse;
import org.dcm4che3.net.Status;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.BasicMPPSSCP;
import org.dcm4che3.net.service.DicomServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fhes.cat.config.APIConstants;
import fhes.cat.services.NotificarEstats;
import fhes.cat.services.impl.NotificacioEstatImpl;



public class BasicMPPSImpl extends BasicMPPSSCP{

	private static final Logger log = LoggerFactory.getLogger(BasicMPPSImpl.class);
	
	String aEAsociado;
	NotificarEstats notificaEstats = new NotificacioEstatImpl();
	
	@Override
    public void onDimseRQ(Association as, PresentationContext pc, Dimse dimse, Attributes rq, Attributes rqAttrs) throws IOException {
		log.info("MPPS is on fire....");
		
		aEAsociado = APIConstants.obtenerAETitleasociado(as+"");
		
		
//		log.info("^^^^^^^^^^^^^^^^^^^^^^ PresentationContext:\n {}", pc);
//		log.info("********************** Association: \n {}", as);
//		log.info("********************** Attributes rq: \n {}", rq);
//		log.info("********************** Attributes rsp: \n {}", rqAttrs);
		
        switch (dimse) {
        case N_CREATE_RQ:
            onNCreateRQ(as, pc, rq, rqAttrs);
            create(rq, rqAttrs);
            break;
        case N_SET_RQ:
            onNSetRQ(as, pc, rq, rqAttrs);
            set(rq, rqAttrs);
            break;
        default:
            throw new DicomServiceException(Status.UnrecognizedOperation);
        }
    }
	
	public void create(Attributes in, Attributes patient) {
		
    	//log.info("CREATE - Attributes rqAttrs: {}", patient);                                                                          
		log.info("************************************************** MPPS CREATE SERVER Informando **************************************");
//    	log.info("AffectedSOPInstanceUID {}", in.getString(Tag.AffectedSOPInstanceUID));
//    	log.info("Patient ID: {}", patient.getString(Tag.PatientID));
//    	log.info("Modality: {}", patient.getString(Tag.Modality));
//    	log.info("Estado: {}", patient.getString(Tag.PerformedProcedureStepStatus));
    	
		String studyInstanceUID = "";
		try {
			studyInstanceUID = patient.getSequence(Tag.ScheduledStepAttributesSequence).get(0).getString(Tag.StudyInstanceUID);
		}catch(Exception e) {
			log.info("Error: {}", e.getMessage());
		}
//    	log.info("StudyInstanceUID: {}", studyInstanceUID);
//    	log.info("************************************************** MPPS SERVER FIN ****************************************************");
    	
    	notificaEstats.notificaEstat(in.getString(Tag.AffectedSOPInstanceUID), patient.getString(Tag.PatientID), patient.getString(Tag.Modality), patient.getString(Tag.PerformedProcedureStepStatus), 
    									studyInstanceUID, patient.getString(Tag.StationName),aEAsociado, in.getString(Tag.PatientName), in.getString(Tag.StudyDescription));
    	
	}
	
	public void set(Attributes in, Attributes patient) {
		
    	//log.info("CREATE - Attributes rqAttrs: {}", patient);
//		log.info("************************************************** MPPS SET SERVER Informando *****************************************");
//    	log.info("RequestedSOPInstanceUID {}", in.getString(Tag.RequestedSOPInstanceUID));
//    	log.info("Estado: {}", patient.getString(Tag.PerformedProcedureStepStatus));
//    	log.info("************************************************** MPPS SERVER FIN ****************************************************");
    	
    	notificaEstats.notificaEstat(in.getString(Tag.RequestedSOPInstanceUID),  patient.getString(Tag.PerformedProcedureStepStatus), in.getString(Tag.PatientName), in.getString(Tag.StudyDescription));
	}
}
