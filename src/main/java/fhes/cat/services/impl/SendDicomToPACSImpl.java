package fhes.cat.services.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.dcm4che2.iod.module.general.SOPCommonModule;
import org.dcm4che2.iod.module.macro.SOPInstanceReference;
import org.dcm4che2.iod.module.macro.SOPInstanceReferenceAndPurpose;
import org.dcm4che2.iod.module.sr.SOPInstanceReferenceAndMAC;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.AssociationHandler;
import org.dcm4che3.net.CancelRQHandler;
import org.dcm4che3.net.Commands;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.DataWriter;
import org.dcm4che3.net.DataWriterAdapter;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.Dimse;
import org.dcm4che3.net.DimseRSP;
import org.dcm4che3.net.DimseRSPHandler;
import org.dcm4che3.net.IncompatibleConnectionException;
import org.dcm4che3.net.Priority;
import org.dcm4che3.net.TransferCapability;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.BasicCMoveSCP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import fhes.cat.config.APIConstants;
import fhes.cat.dmdcm4che3.rsp.DimseRSPHandlerImpl;
import fhes.cat.dto.BuscarDicomDTO;
import fhes.cat.services.ErrorMessageParser;
import fhes.cat.services.SendDicomToPACS;
import lombok.extern.slf4j.Slf4j;
import services.SocketService;
import services.impl.SocketServiceImpl;

@Slf4j
@Service
public class SendDicomToPACSImpl implements SendDicomToPACS {

	private SocketService socketService = new SocketServiceImpl();

	@Autowired
	ErrorMessageParser errorMessage;

	ObjectMapper mapper = new ObjectMapper();

	@Override
	public boolean sendDICOM(Attributes atr, String tsuid, String cuid, String iuid, String calledAET, String callingAET, String ipCalled, int portCalled) {
		
		ApplicationEntity locAE = new ApplicationEntity();
		locAE.setAETitle(APIConstants.localServerSender.getValor2());
		locAE.setInstalled(true);

		log.info("CalledAET: {}", calledAET);
		log.info("CallingAET: {}", callingAET);
		
		String ipAddress = "0.0.0.0";

		Connection localConn = new Connection();
		localConn.setCommonName("loc_conn");
		localConn.setHostname(ipAddress);
		localConn.setPort(Integer.parseInt(APIConstants.localServerSender.getValor1()));
		localConn.setProtocol(Connection.Protocol.DICOM);
		localConn.setInstalled(true);

		String[] transferSyntaxes = {				
				"1.2.840.10008.1.2.1",      // Explicit VR Little Endian
				"1.2.840.10008.1.2.2",      // Explicit VR Big Endian (Retired)
				//"1.2.840.10008.1.2", //lo he puesto, y estaba quitado
				"1.2.840.10008.1.1", //Verification SOP Class
				"1.2.840.10008.1.2.4.80", //JPEG-LS Lossless Image Compression
				"1.2.840.10008.1.2.4.50", // (JPEG Baseline(Process 1))"				
				"1.2.840.10008.1.2.5", //RLE Lossless 
				"1.2.840.10008.1.2.4.51", //JPEG Extended (Process 2 & 4)
				//"1.2.840.10008.1.2.4.70", //JPEG Lossless, Non-Hierarchical, First-Order Prediction
				"1.2.840.10008.1.2.4.90", //JPEG 2000 Image Compression (Lossless Only)
				"1.2.840.10008.1.2.4.91", //JPEG 2000 Image Compression
				"1.2.840.10008.1.2.4.100", //MPEG2 Main Profile / Main Level
				"1.2.840.10008.1.2.4.102", //MPEG-4 AVC/H.264 High Profile / Level 4.1
				"1.2.840.10008.1.2.1.99" //Deflated Explicit VR Little Endian 
		};

		APIConstants.capacidadTransferencia.forEach(cap -> {
			locAE.addTransferCapability(new TransferCapability(cap.getValor1(), cap.getValor2(),TransferCapability.Role.SCP, transferSyntaxes));
		});
        
		locAE.addConnection(localConn);

		// Crear un objeto de conexión DICOM
		ApplicationEntity remoteAE = new ApplicationEntity();
		remoteAE.setAETitle(calledAET); 
		remoteAE.setInstalled(true);

		Connection remoteConn = new Connection();
		remoteConn.setCommonName("rem_conn");
		remoteConn.setHostname(ipCalled);
		remoteConn.setPort(portCalled);
		remoteConn.setProtocol(Connection.Protocol.DICOM);
		remoteConn.setIdleTimeout(0);
		remoteConn.setInstalled(true);
		remoteAE.addConnection(remoteConn);

		AAssociateRQ assocReq = new AAssociateRQ();

		assocReq.setCalledAET(remoteAE.getAETitle());
		assocReq.setCallingAET(callingAET);
		assocReq.setApplicationContext("1.2.840.10008.3.1.1.1");

		// assocReq.setImplVersionName("dcm4che-5.30.0");
		assocReq.setMaxPDULength(65536); //Especifica el tamaño máximo de la Unidad de Datos de Protocolo (PDU) que la aplicación puede recibir, en bytes.
		assocReq.setMaxOpsInvoked(1);
		assocReq.setMaxOpsPerformed(1);

		for (int x = 0; x < APIConstants.presentationContext.size(); x++) {
			assocReq.addPresentationContext(new PresentationContext(x + 1, APIConstants.presentationContext.get(x).getValor1(), APIConstants.presentationContext.get(x).getValor2()));
		}
		

		Device device = new Device("device");
		device.addConnection(localConn);
		device.addApplicationEntity(locAE);

		device.setAssociationHandler(new AssociationHandlerImpl()); // gestiona el evento de negociación

		device.setExecutor(Executors.newSingleThreadExecutor());

		Association assoc = null;
				
		try {
			assoc = locAE.connect(localConn, remoteConn, assocReq);
			DataWriter data = new DataWriterAdapter(atr);
			assoc.cstore(cuid, iuid, 0, data, tsuid);
			
			assoc.release();

		} catch (InterruptedException e) {
			log.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ ERROR InterruptedException {}", e.getMessage());
			return false;
		} catch (IncompatibleConnectionException e) {
			log.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ ERROR IncompatibleConnectionException {}", e.getMessage());
			return false;
		} catch (GeneralSecurityException e) {
			log.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ ERROR GeneralSecurityException {}", e.getMessage());
			return false;
		} catch (IOException e) {
			log.error("Error al enviar imágenes al PACS o RSYNC", e);
			socketService.notificarMissatge(SocketServiceImpl.MISSATGE_CRITICAL, "Error de conexión con " + calledAET, " No enviant imagtes al PACS  " + e.getMessage());
			return false;
		}

		return true;

	}

	@Override
	public boolean sendCMove(Attributes atr, String localAETitle) {
		// Local

		log.info("Creando la conexión con el PACS para buscar una imagen.");

		ApplicationEntity locAE = new ApplicationEntity();
		locAE.setAETitle(APIConstants.localServerSender.getValor2());
		locAE.setInstalled(true);

		log.info("AETitle: {}", APIConstants.aTitlePacs);

//		InetAddress localHost = null;
//
//		try {
//			localHost = InetAddress.getLocalHost();
//		} catch (UnknownHostException e) {
//			socketService.notificarMissatge(SocketServiceImpl.MISSATGE_CRITICAL, "Error DICOM", "No s'ha pogut iniciar el servidor DICOM - Storage --" + e.getMessage());
//			log.info("No se ha podido iniciar el servidor DICOM {}", e.getMessage());
//			e.printStackTrace();
//		}
		// String ipAddress = localHost.getHostAddress();
		String ipAddress = "0.0.0.0";

		Connection localConn = new Connection();
		localConn.setCommonName("loc_conn");
		localConn.setHostname(ipAddress);
		localConn.setPort(Integer.parseInt(APIConstants.localServerSender.getValor1())); //1112
		localConn.setProtocol(Connection.Protocol.DICOM);
		localConn.setInstalled(true);

		String[] transferSyntaxes = { 
				"1.2.840.10008.1.2.1",      // Explicit VR Little Endian
				"1.2.840.10008.1.2.2",      // Explicit VR Big Endian (Retired)
				//"1.2.840.10008.1.2",
				"1.2.840.10008.1.2.4.70", //JPEG Lossless, Non-Hierarchical, First-Order Prediction (Process 14 [Selection Value 1])
				"1.2.840.10008.1.1", //Verification SOP Class
				"1.2.840.10008.1.2.4.80", //JPEG-LS Lossless Image Compression
				"1.2.840.10008.1.2.4.50", // (JPEG Baseline(Process 1))"				
				"1.2.840.10008.1.2.5", //RLE Lossless 
				"1.2.840.10008.1.2.4.51", //JPEG Extended (Process 2 & 4)
				//"1.2.840.10008.1.2.4.70", //JPEG Lossless, Non-Hierarchical, First-Order Prediction
				"1.2.840.10008.1.2.4.90", //JPEG 2000 Image Compression (Lossless Only)
				"1.2.840.10008.1.2.4.91", //JPEG 2000 Image Compression
				"1.2.840.10008.1.2.4.100", //MPEG2 Main Profile / Main Level
				"1.2.840.10008.1.2.4.102", //MPEG-4 AVC/H.264 High Profile / Level 4.1
				"1.2.840.10008.1.2.1.99" //Deflated Explicit VR Little Endian 
		};

		APIConstants.capacidadTransferencia.forEach(cap -> {
			locAE.addTransferCapability(new TransferCapability(cap.getValor1(), cap.getValor2(),TransferCapability.Role.SCP, transferSyntaxes));
		});
        
   
//		locAE.addTransferCapability(new TransferCapability("Query/Retrieve SOP Class", "1.2.840.10008.5.1.4.1.2.1.2", TransferCapability.Role.SCP, transferSyntaxes));

		locAE.addConnection(localConn);

		// Crear un objeto de conexión DICOM
		ApplicationEntity remoteAE = new ApplicationEntity();
		remoteAE.setAETitle(APIConstants.aTitlePacs);
		remoteAE.setInstalled(true);

		Connection remoteConn = new Connection();
		remoteConn.setCommonName("rem_conn");
		remoteConn.setHostname(APIConstants.ipPacs);
		remoteConn.setPort(APIConstants.portPacs);
		remoteConn.setProtocol(Connection.Protocol.DICOM);
		remoteConn.setIdleTimeout(0);
		remoteConn.setInstalled(true);
		remoteAE.addConnection(remoteConn);

		AAssociateRQ assocReq = new AAssociateRQ();

		assocReq.setCalledAET(remoteAE.getAETitle());
		assocReq.setCallingAET(localAETitle); //"SDFHES");
		assocReq.setApplicationContext("1.2.840.10008.3.1.1.1");
		assocReq.setImplClassUID("1.2.40.0.13.1.3");
		assocReq.setMaxPDULength(65536); //Especifica el tamaño máximo de la Unidad de Datos de Protocolo (PDU) que la aplicación puede recibir, en bytes.
		assocReq.setMaxOpsInvoked(1);
		assocReq.setMaxOpsPerformed(1);

		
		for (int x = 0; x < APIConstants.presentationContext.size(); x++) {
			assocReq.addPresentationContext(new PresentationContext(x + 1, APIConstants.presentationContext.get(x).getValor1(), APIConstants.presentationContext.get(x).getValor2()));
		}	
        
		Device device = new Device("device");
		device.addConnection(localConn);
		device.addApplicationEntity(locAE);

		device.setAssociationHandler(new AssociationHandlerImpl()); // gestiona el evento de negociación
		ExecutorService exec = Executors.newSingleThreadExecutor();
		device.setExecutor(exec);
		
	//	device.setExecutor(Executors.newCachedThreadPool());
		
				
		Association assoc = null;
		
		String cuid = "1.2.840.10008.5.1.4.1.2.2.2";  //Study Root Query/Retrieve Information Model – MOVE
       
		try {			
			assoc = locAE.connect(localConn, remoteConn, assocReq);
			
			//Attributes cmd = new Attributes();
			
			//atr.setString(Tag.QueryRetrieveLevel, VR.CS, "STUDY"); //IMAGE STUDY
			//cmd.setInt(Tag.MessageID, VR.US, 1034);
			atr.setString(Tag.MoveDestination, VR.AE, APIConstants.localServerSender.getValor2());
			atr.setInt(Tag.Priority, VR.US, Priority.NORMAL);
			atr.setInt(Tag.CommandDataSetType, VR.US,  0x0102);
			atr.setInt(Tag.InstanceNumber, VR.IS, 1);
			atr.setInt(Tag.CommandField, VR.US,  0x0021); //c-move
			//Es el Identificador Único de Sintaxis de Transferencia (Transfer Syntax UID), que especifica cómo se codifican los datos que se envían a través de una asociación DICOM.
			assoc.cmove(cuid, Priority.NORMAL, atr, "1.2.840.10008.1.2.1", APIConstants.localServerSender.getValor2(), new DimseRSPHandlerImpl(assoc.nextMessageID()));
						
		} catch (InterruptedException e) {
			log.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ ERROR InterruptedException {}", e.getMessage());
			return false;
		} catch (IncompatibleConnectionException e) {
			log.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ ERROR IncompatibleConnectionException {}", e.getMessage());
			return false;
		} catch (GeneralSecurityException e) {
			log.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ ERROR GeneralSecurityException {}", e.getMessage());
			return false;
		} catch (IOException e) {
			log.error("Error al traer imágenes del PACS (sendCMove)", e);
			socketService.notificarMissatge(SocketServiceImpl.MISSATGE_CRITICAL, "Error de conexión con "+APIConstants.aTitlePacs, "No enviant imagtes al PACS " + e.getMessage());
			return false;
		}

		return true;
	}

	@Override
	public boolean sendCFind(BuscarDicomDTO dicom) {
		log.info("Creando la conexión con el PACS para buscar una imagen.");

		ApplicationEntity locAE = new ApplicationEntity();
		locAE.setAETitle(APIConstants.localServerSender.getValor2());
		locAE.setInstalled(true);

		log.info("AETitle: {}", APIConstants.aTitlePacs);

		InetAddress localHost = null;

		try {
			localHost = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			socketService.notificarMissatge(SocketServiceImpl.MISSATGE_CRITICAL, "Error DICOM","No s'ha pogut iniciar el servidor DICOM - Storage --" + e.getMessage());
			log.info("No se ha podido iniciar el servidor DICOM {}", e.getMessage());
			e.printStackTrace();
		}
		// String ipAddress = localHost.getHostAddress();
		String ipAddress = "0.0.0.0";

		Connection localConn = new Connection();
		localConn.setCommonName("loc_conn");
		localConn.setHostname(ipAddress);
		localConn.setPort(1112);// APIConstants.localServerSender.getValor1()));
		localConn.setProtocol(Connection.Protocol.DICOM);
		localConn.setInstalled(true);

		String[] transferSyntaxes = { 
				"1.2.840.10008.1.2.1",      // Explicit VR Little Endian
				"1.2.840.10008.1.2.2",      // Explicit VR Big Endian (Retired)
				
				"1.2.840.10008.1.1", //Verification SOP Class
				"1.2.840.10008.1.2.4.80", //JPEG-LS Lossless Image Compression
				"1.2.840.10008.1.2.4.50", // (JPEG Baseline(Process 1))"				
				"1.2.840.10008.1.2.5", //RLE Lossless 
				"1.2.840.10008.1.2.4.51", //JPEG Extended (Process 2 & 4)
				"1.2.840.10008.1.2.4.70", //JPEG Lossless, Non-Hierarchical, First-Order Prediction
				"1.2.840.10008.1.2.4.90", //JPEG 2000 Image Compression (Lossless Only)
				"1.2.840.10008.1.2.4.91", //JPEG 2000 Image Compression
				"1.2.840.10008.1.2.4.100", //MPEG2 Main Profile / Main Level
				"1.2.840.10008.1.2.4.102", //MPEG-4 AVC/H.264 High Profile / Level 4.1
				"1.2.840.10008.1.2.1.99" //Deflated Explicit VR Little Endian 
		};
		
		APIConstants.capacidadTransferencia.forEach(cap -> {
			locAE.addTransferCapability(new TransferCapability(cap.getValor1(), cap.getValor2(),TransferCapability.Role.SCP, transferSyntaxes));
		});

   
		//locAE.addTransferCapability(new TransferCapability("Query/Retrieve SOP Class", "1.2.840.10008.5.1.4.1.2.1.2", TransferCapability.Role.SCP, transferSyntaxes));

		locAE.addConnection(localConn);

		// Crear un objeto de conexión DICOM
		ApplicationEntity remoteAE = new ApplicationEntity();
		remoteAE.setAETitle(APIConstants.aTitlePacs);
		remoteAE.setInstalled(true);

		Connection remoteConn = new Connection();
		remoteConn.setCommonName("rem_conn");
		remoteConn.setHostname(APIConstants.ipPacs);
		remoteConn.setPort(APIConstants.portPacs);
		remoteConn.setProtocol(Connection.Protocol.DICOM);
		remoteConn.setIdleTimeout(0);
		remoteConn.setInstalled(true);
		remoteAE.addConnection(remoteConn);

		AAssociateRQ assocReq = new AAssociateRQ();

		assocReq.setCalledAET(remoteAE.getAETitle());
		assocReq.setCallingAET(APIConstants.localServerSender.getValor2());
		assocReq.setApplicationContext("1.2.840.10008.3.1.1.1");
		assocReq.setImplClassUID("1.2.40.0.13.1.3");
		assocReq.setMaxPDULength(65536); //Especifica el tamaño máximo de la Unidad de Datos de Protocolo (PDU) que la aplicación puede recibir, en bytes.
		assocReq.setMaxOpsInvoked(1);
		assocReq.setMaxOpsPerformed(1);

		
		for (int x = 0; x < APIConstants.presentationContext.size(); x++) {
			assocReq.addPresentationContext(new PresentationContext(x + 1, APIConstants.presentationContext.get(x).getValor1(), APIConstants.presentationContext.get(x).getValor2()));
		}

		
		Device device = new Device("device");
		device.addConnection(localConn);
		device.addApplicationEntity(locAE);

		device.setAssociationHandler(new AssociationHandlerImpl()); // gestiona el evento de negociación
		ExecutorService exec = Executors.newSingleThreadExecutor();
		device.setExecutor(exec);
		
				
		Association assoc = null;
		
		String cuid = "1.2.840.10008.5.1.4.1.2.2.1";  //Study Root Query/Retrieve Information Model – FIND
		//String cuid = "1.2.840.10008.5.1.4.1.2.1.1"; //	Patient Root Query/Retrieve Information Model – FIND
       
		try {			
			assoc = locAE.connect(localConn, remoteConn, assocReq);
			
			Attributes cmd = new Attributes();
			
			cmd.setString(Tag.QueryRetrieveLevel, VR.CS, "STUDY"); //IMAGE STUDY
			cmd.setInt(Tag.Priority, VR.US, Priority.NORMAL);
			cmd.setInt(Tag.CommandDataSetType, VR.US,  0x0102);
			cmd.setInt(Tag.CommandField, VR.US,  0x0020); //c-find

			cmd.setString(Tag.AccessionNumber, VR.SH, dicom.getAccessionNumber());
			cmd.setString(Tag.StudyDate, VR.DA, dicom.getStudyDate());// "20230921-20230921");
			cmd.setString(Tag.PatientID, VR.LO, dicom.getPatientID()); //16126 11
			cmd.setString(Tag.PatientName, VR.LO, dicom.getPatientName()); //FICTICIO ACTIVO^CIUDADANO
			cmd.setString(Tag.PatientBirthDate, VR.DA, dicom.getPatientBirthDate()); //19540403
			cmd.setString(Tag.ModalitiesInStudy, VR.CS, dicom.getModality());
			
			cmd.setString(Tag.SeriesInstanceUID, VR.UI, "");
			cmd.setString(Tag.SOPInstanceUID, VR.UI, "");
			
			cmd.setString(Tag.StudyInstanceUID, VR.UI, ""); //2.25.178871392199659569306424781142514997762.1
			
			//log.info("Atributos de la búsqueda: {}", cmd);
			assoc.cfind(cuid, Priority.NORMAL, cmd, "1.2.840.10008.1.2.1", new DimseRSPHandlerImpl(dicom.getIdBusqueda()));			
			
		} catch (InterruptedException e) {
			log.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ ERROR InterruptedException {}", e.getMessage());
			return false;
		} catch (IncompatibleConnectionException e) {
			log.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ ERROR IncompatibleConnectionException {}", e.getMessage());
			return false;
		} catch (GeneralSecurityException e) {
			log.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ ERROR GeneralSecurityException {}", e.getMessage());
			return false;
		} catch (IOException e) {
			log.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ ERROR Reason: {}", e.getMessage());
			// socketService.notificarMissatge(SocketServiceImpl.MISSATGE_CRITICAL, "Error
			// de conexión con "+APIConstants.aTitlePacs, "No enviant imagtes al PACS " +
			// e.getMessage());
			return false;
		}

		return true;
	}

	@Override
	public boolean deleteDicomImage(String cuid, String iuid) {
		
		log.info("Creando la conexión con el PACS para borrar una imagen.");

		ApplicationEntity locAE = new ApplicationEntity();
		locAE.setAETitle(APIConstants.localServerSender.getValor2());
		locAE.setInstalled(true);

		InetAddress localHost = null;

		try {
			localHost = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			socketService.notificarMissatge(SocketServiceImpl.MISSATGE_CRITICAL, "Error DICOM", "No s'ha pogut iniciar el servidor DICOM - Storage --" + e.getMessage());
			log.info("No se ha podido iniciar el servidor DICOM {}", e.getMessage());
			e.printStackTrace();
		}
		// String ipAddress = localHost.getHostAddress();
		String ipAddress = "0.0.0.0";

		Connection localConn = new Connection();
		localConn.setCommonName("loc_conn");
		localConn.setHostname(ipAddress);
		localConn.setPort(1112);// APIConstants.localServerSender.getValor1()));
		localConn.setProtocol(Connection.Protocol.DICOM);
		localConn.setInstalled(true);

		String[] transferSyntaxes = { 
				"1.2.840.10008.1.2.1",      // Explicit VR Little Endian
				"1.2.840.10008.1.2.2",      //Explicit VR Big Endian (Retired)
				
				"1.2.840.10008.1.1", //Verification SOP Class
				"1.2.840.10008.1.2.4.80", //JPEG-LS Lossless Image Compression
				"1.2.840.10008.1.2.4.50", // (JPEG Baseline(Process 1))"				
				"1.2.840.10008.1.2.5", //RLE Lossless 
				"1.2.840.10008.1.2.4.51", //JPEG Extended (Process 2 & 4)
				"1.2.840.10008.1.2.4.70", //JPEG Lossless, Non-Hierarchical, First-Order Prediction
				"1.2.840.10008.1.2.4.90", //JPEG 2000 Image Compression (Lossless Only)
				"1.2.840.10008.1.2.4.91", //JPEG 2000 Image Compression
				"1.2.840.10008.1.2.4.100", //MPEG2 Main Profile / Main Level
				"1.2.840.10008.1.2.4.102", //MPEG-4 AVC/H.264 High Profile / Level 4.1
				"1.2.840.10008.1.2.1.99" //Deflated Explicit VR Little Endian 
		};
		
		APIConstants.capacidadTransferencia.forEach(cap -> {
			locAE.addTransferCapability(new TransferCapability(cap.getValor1(), cap.getValor2(),TransferCapability.Role.SCP, transferSyntaxes));
		});

//		locAE.addTransferCapability(new TransferCapability("Query/Retrieve SOP Class", "1.2.840.10008.5.1.4.1.2.1.2", TransferCapability.Role.SCP, transferSyntaxes));

        locAE.addConnection(localConn);

		// Crear un objeto de conexión DICOM
		ApplicationEntity remoteAE = new ApplicationEntity();
		remoteAE.setAETitle(APIConstants.aTitlePacs);
		remoteAE.setInstalled(true);

		Connection remoteConn = new Connection();
		remoteConn.setCommonName("rem_conn");
		remoteConn.setHostname(APIConstants.ipPacs);
		remoteConn.setPort(APIConstants.portPacs);
		remoteConn.setProtocol(Connection.Protocol.DICOM);
		remoteConn.setIdleTimeout(0);
		remoteConn.setInstalled(true);
		remoteAE.addConnection(remoteConn);

		AAssociateRQ assocReq = new AAssociateRQ();

		assocReq.setCalledAET(remoteAE.getAETitle());
		assocReq.setCallingAET(APIConstants.localServerSender.getValor2()); //"SDFHES");
		assocReq.setApplicationContext("1.2.840.10008.3.1.1.1");
		assocReq.setImplClassUID("1.2.40.0.13.1.3");
		assocReq.setMaxPDULength(65536); //Especifica el tamaño máximo de la Unidad de Datos de Protocolo (PDU) que la aplicación puede recibir, en bytes.
		assocReq.setMaxOpsInvoked(1);
		assocReq.setMaxOpsPerformed(1);

		
		for (int x = 0; x < APIConstants.presentationContext.size(); x++) {
			assocReq.addPresentationContext(new PresentationContext(x + 1, APIConstants.presentationContext.get(x).getValor1(), APIConstants.presentationContext.get(x).getValor2()));
		}
				
//        assocReq.addPresentationContext(new PresentationContext(1, "1.2.840.10008.5.1.4.1.2.2.1", "1.2.840.10008.1.2.1"));
        
		Device device = new Device("device");
		device.addConnection(localConn);
		device.addApplicationEntity(locAE);

		device.setAssociationHandler(new AssociationHandlerImpl()); // gestiona el evento de negociación
		ExecutorService exec = Executors.newSingleThreadExecutor();
		device.setExecutor(exec);
						
						
		Association assoc = null;
		
		cuid =  "2.25.29839540022949921025787978155315852367.1.1";  //N_DELETE
		//Segon ocmentari
		//String studyUID="2.25.29839540022949921025787978155315852367.1";
		//String seriesUID="2.25.29839540022949921025787978155315852367.1.1";
		iuid="2.25.29839540022949921025787978155315852367.1.1698235667";
		try {			
			assoc = locAE.connect(localConn, remoteConn, assocReq);						
			//assoc.ndelete(asuid, cuid, iuid, rspHandler);
			assoc.ndelete(cuid, iuid, new DimseRSPHandlerImpl(assoc.nextMessageID()));
			
						
		} catch (InterruptedException e) {
			log.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ ERROR InterruptedException {}", e.getMessage());
			return false;
		} catch (IncompatibleConnectionException e) {
			log.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ ERROR IncompatibleConnectionException {}", e.getMessage());
			return false;
		} catch (GeneralSecurityException e) {
			log.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ ERROR GeneralSecurityException {}", e.getMessage());
			return false;
		} catch (IOException e) {
			log.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ ERROR Reason: {}", e.getMessage());
			 socketService.notificarMissatge(SocketServiceImpl.MISSATGE_CRITICAL, "Error de conexión con "+APIConstants.aTitlePacs, "No enviant imagtes al PACS " + e.getMessage());
			return false;
		}

		return true;
	}

	@Override
	public boolean sendECGtoIMG(Attributes atr) {
		// TODO Auto-generated method stub
		return false;
	}

}
