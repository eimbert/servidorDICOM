package fhes.cat.dmdcm4che3.impl;

import org.dcm4che3.data.UID;
import org.dcm4che3.net.Commands;
import org.dcm4che3.net.Dimse;
import org.dcm4che3.net.Status;
import org.dcm4che3.net.service.BasicCEchoSCP;
import org.dcm4che3.net.service.DicomServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicCEchoSCPImpl extends BasicCEchoSCP {

	private static final Logger log = LoggerFactory.getLogger(BasicCEchoSCPImpl.class);
	
	 public BasicCEchoSCPImpl() {
	        super(UID.Verification);
	 }
	 
	@Override
	public void onDimseRQ(org.dcm4che3.net.Association as, org.dcm4che3.net.pdu.PresentationContext pc, org.dcm4che3.net.Dimse dimse, org.dcm4che3.data.Attributes cmd, org.dcm4che3.data.Attributes data) throws java.io.IOException{
		log.info("\n******************************************************  Mensaje DICOM recibido - BASIC Echo\n");
		
		if (dimse != Dimse.C_ECHO_RQ)
			throw new DicomServiceException(Status.UnrecognizedOperation);

	    as.tryWriteDimseRSP(pc, Commands.mkEchoRSP(cmd, Status.Success));
	}
}
