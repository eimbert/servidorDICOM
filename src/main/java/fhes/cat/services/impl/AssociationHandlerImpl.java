package fhes.cat.services.impl;

import java.io.IOException;

import org.dcm4che3.data.UID;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.AssociationHandler;
import org.dcm4che3.net.TransferCapability;
import org.dcm4che3.net.pdu.AAssociateAC;
import org.dcm4che3.net.pdu.AAssociateRJ;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.UserIdentityAC;

import lombok.extern.slf4j.Slf4j;
import services.SocketService;
import services.impl.SocketServiceImpl;

@Slf4j
public class AssociationHandlerImpl extends  AssociationHandler{

	private SocketService socketService = new SocketServiceImpl();
	
	 protected AAssociateAC negotiate(Association as, AAssociateRQ rq) throws IOException {		 	
		 	String abstractSyntax = rq.getPresentationContext(1).getAbstractSyntax();
		 	
//		 	log.info("@@@@@@@@@@@@@@@ AAssociateRQ @@@@@@@@@@@@@@@@\n{}", rq);
//		 	log.info("@@@@@@@@@@@@@@@ AE @@@@@@@@@@@@@@@@\n{}", as);
//		 	log.info("@@@@@@@@@@@@@@@ AE getCalledAET @@@@@@@@@@@@@@@@\n{}", as.getCalledAET());
//		 	log.info("@@@@@@@@@@@@@@@ AE getCalledAET @@@@@@@@@@@@@@@@\n{}", as.getCalledAET());
//		 	
	        if ((rq.getProtocolVersion() & 1) == 0) {
	        	socketService.notificarMissatge(SocketServiceImpl.MISSATGE_CRITICAL, "Error Servidor DICOM", "Error negociando - REASON_PROTOCOL_VERSION_NOT_SUPPORTED");
	            throw new AAssociateRJ(AAssociateRJ.RESULT_REJECTED_PERMANENT, AAssociateRJ.SOURCE_SERVICE_PROVIDER_ACSE, AAssociateRJ.REASON_PROTOCOL_VERSION_NOT_SUPPORTED);
	        }
	        if (!rq.getApplicationContext().equals(UID.DICOMApplicationContext)) {
	        	socketService.notificarMissatge(SocketServiceImpl.MISSATGE_CRITICAL, "Error Servidor DICOM", "Error negociando - REASON_APP_CTX_NAME_NOT_SUPPORTED");
	            throw new AAssociateRJ(AAssociateRJ.RESULT_REJECTED_PERMANENT, AAssociateRJ.SOURCE_SERVICE_USER, AAssociateRJ.REASON_APP_CTX_NAME_NOT_SUPPORTED);
	        }
	        
	        ApplicationEntity ae = as.getApplicationEntity();
	        
	        if(!buscarEnApplicationEntity(ae, abstractSyntax)) {
	        	socketService.notificarMissatge(SocketServiceImpl.MISSATGE_CRITICAL, "Error Servidor DICOM", "Error negociando - abstractSyntax "+ abstractSyntax +" no encontrado en TransferCapability del servidor");
	        	throw new AAssociateRJ(AAssociateRJ.RESULT_REJECTED_PERMANENT, AAssociateRJ.SOURCE_SERVICE_USER, AAssociateRJ.REASON_CALLED_AET_NOT_RECOGNIZED);
	        }
	        
	        if (ae == null || !ae.getConnections().contains(as.getConnection()) || !ae.isInstalled() || !ae.isAssociationAcceptor()) {
	        	socketService.notificarMissatge(SocketServiceImpl.MISSATGE_CRITICAL, "Error Servidor DICOM", "Error negociando - REASON_CALLED_AET_NOT_RECOGNIZED");
	            throw new AAssociateRJ(AAssociateRJ.RESULT_REJECTED_PERMANENT, AAssociateRJ.SOURCE_SERVICE_USER, AAssociateRJ.REASON_CALLED_AET_NOT_RECOGNIZED);
	        }
	        
	        if (!ae.isAcceptedCallingAETitle(rq.getCallingAET())) {
	        	socketService.notificarMissatge(SocketServiceImpl.MISSATGE_CRITICAL, "Error Servidor DICOM", "Error negociando - REASON_CALLING_AET_NOT_RECOGNIZED");
	            throw new AAssociateRJ(AAssociateRJ.RESULT_REJECTED_PERMANENT, AAssociateRJ.SOURCE_SERVICE_USER, AAssociateRJ.REASON_CALLING_AET_NOT_RECOGNIZED);
	        }
	        
	        UserIdentityAC userIdentity = getUserIdNegotiator().negotiate(as, rq.getUserIdentityRQ());
	        if (ae.getDevice().isLimitOfAssociationsExceeded(rq)) {
	        	socketService.notificarMissatge(SocketServiceImpl.MISSATGE_CRITICAL, "Error Servidor DICOM", "Error negociando - REASON_LOCAL_LIMIT_EXCEEDED");
	            throw new AAssociateRJ(AAssociateRJ.RESULT_REJECTED_TRANSIENT, AAssociateRJ.SOURCE_SERVICE_PROVIDER_PRES, AAssociateRJ.REASON_LOCAL_LIMIT_EXCEEDED);
	        }
	        
	        return makeAAssociateAC(as, rq, userIdentity);
	    }
	 
	 private boolean buscarEnApplicationEntity(ApplicationEntity ae, String buscar) {
		    if (ae == null) {
		        return false;
		    }
		        
		    // Verifica en las capacidades de transferencia de la ApplicationEntity
		    for (TransferCapability tc : ae.getTransferCapabilities()) {
//		    	log.info("transferencia : {}", tc.toString());
//		    	log.info("transferencia buscada: {}", buscar);
		        if (tc.toString().contains(buscar)) {
		            return true;
		        }
		    }
		    
		    return false;
		}
}
