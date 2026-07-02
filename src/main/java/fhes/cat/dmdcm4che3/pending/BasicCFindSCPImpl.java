package fhes.cat.dmdcm4che3.pending;

import java.io.IOException;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Commands;
import org.dcm4che3.net.QueryOption;
import org.dcm4che3.net.Status;
import org.dcm4che3.net.pdu.ExtendedNegotiation;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.BasicCFindSCP;
import org.dcm4che3.net.service.DicomServiceException;
import org.dcm4che3.net.service.QueryRetrieveLevel;
import org.dcm4che3.net.service.QueryTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class BasicCFindSCPImpl extends BasicCFindSCP{

	private static final Logger log = LoggerFactory.getLogger(BasicCFindSCPImpl.class);
	
	private final String[] qrLevels;
    private final QueryRetrieveLevel rootLevel;

    
    public BasicCFindSCPImpl(String sopClass, String... qrLevels) {
        super(sopClass);
        this.qrLevels = qrLevels;
        this.rootLevel = QueryRetrieveLevel.valueOf(qrLevels[0]);
    }

    @Override
    protected QueryTask calculateMatches(Association as, PresentationContext pc, Attributes rq, Attributes rp) throws DicomServiceException {
    	System.out.println("********************************************** Solicitud Work List C_FIND *********************************************************");
    	log.info("\n********************************************** Solicitud Work List C_FIND *********************************************************\n");
        //QueryRetrieveLevel level = QueryRetrieveLevel.valueOf(keys, qrLevels);
        //level.validateQueryKeys(keys, rootLevel, rootLevel == QueryRetrieveLevel.IMAGE || relational(as, rq));
        
        String patientId = rp.getString(0x00100020);
                
//        System.out.println("*********** Paciente ID: " + patientId);  
//        
//        System.out.println("*********** AffectedSOPClassUID...: " +rq.getString(Tag.AffectedSOPClassUID));
//        System.out.println("*********** CommandField...: " +rq.getString(Tag.CommandField));

                     
        Attributes attributes = new Attributes();
                
        attributes.setString(0x00000002, VR.UI, rq.getString(Tag.AffectedSOPClassUID));
        
        attributes.setInt(0x00000100, VR.US, 32800);
        attributes.setString(0x00000120, VR.US, rq.getString(0x00000110));
        attributes.setString(0x00000800, VR.US, rq.getString(0x00000800));
        
        attributes.setString(Tag.OtherPatientIDs, VR.LO, "");
        attributes.setString(Tag.PatientID, VR.LO, "16112611");
        attributes.setString(Tag.PatientName, VR.PN, "Ciudadano Ficticio");
        attributes.setString(Tag.PatientAge, VR.AS, "80");
        attributes.setString(Tag.PatientBirthDate, VR.DA, "20000101");
        attributes.setString(Tag.PatientBirthTime, VR.TM, "");
        attributes.setString(Tag.PatientSex, VR.CS, "M");
        attributes.setString(Tag.PatientSize, VR.DS, "1.70");
        attributes.setString(Tag.PatientWeight, VR.DS, "70.00");
               
        attributes.setString(Tag.StudyID, VR.SH, "3004");
        attributes.setString(Tag.AccessionNumber, VR.SH, "9005214");
        attributes.setString(Tag.StudyInstanceUID, VR.SH, "9999.755.160084.56310701");
        
        Attributes attributes2 = new Attributes();
        
        attributes2.setString(0x00000002, VR.UI, rq.getString(Tag.AffectedSOPClassUID));
        
        attributes2.setInt(0x00000100, VR.US, 32800);
        attributes2.setString(0x00000120, VR.US, rq.getString(0x00000110));
        attributes2.setString(0x00000800, VR.US, rq.getString(0x00000800));
        
        attributes2.setString(Tag.OtherPatientIDs, VR.LO, "");
        attributes2.setString(Tag.PatientID, VR.LO, "25252525");
        attributes2.setString(Tag.PatientName, VR.PN, "Otro paciente");
        attributes2.setString(Tag.PatientAge, VR.AS, "70");
        attributes2.setString(Tag.PatientBirthDate, VR.DA, "20000101");
        attributes2.setString(Tag.PatientBirthTime, VR.TM, "");
        attributes2.setString(Tag.PatientSex, VR.CS, "M");
        attributes2.setString(Tag.PatientSize, VR.DS, "1.90");
        attributes2.setString(Tag.PatientWeight, VR.DS, "80.00");
               
        attributes2.setString(Tag.StudyID, VR.SH, "3005");
        attributes2.setString(Tag.AccessionNumber, VR.SH, "9005200");
        attributes2.setString(Tag.StudyInstanceUID, VR.SH, "9999.755.160084.56310801");
               
        
        
        //System.out.println("*********** ANTES ************************** \n");
        log.info("*********** A-RELEASE-RQ is...: \n" + rq);
        log.info("*********** A-RELEASE-RP is...: \n" + rp);
        
        log.info("*********** attr is...: \n" + attributes);
        
        log.info("*********** Msg ID is...: \n" + rq.getInt(Tag.MessageID, -1));
  
        
        try {
        	//rp.addAll(attributes);
			as.writeDimseRSP(pc, Commands.mkCFindRSP(rq, Status.Pending), attributes);
			//rp.addAll(attributes2);
			//as.writeDimseRSP(pc, Commands.mkCFindRSP(rq, Status.Pending), attributes2);
			as.writeDimseRSP(pc, Commands.mkCFindRSP(rq, Status.Success));
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
      
       // QueryTask q = new BasicQueryTask(as, pc, rq, attributes);
       // q.run();
        
        return null;
        
        
    }

    private boolean relational(Association as, Attributes rq) {
        String cuid = rq.getString(Tag.AffectedSOPClassUID);
        ExtendedNegotiation extNeg = as.getAAssociateAC().getExtNegotiationFor(cuid);
        return QueryOption.toOptions(extNeg).contains(QueryOption.RELATIONAL);
    }
}
