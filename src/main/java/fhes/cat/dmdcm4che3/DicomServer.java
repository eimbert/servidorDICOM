package fhes.cat.dmdcm4che3;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.dcm4che3.data.UID;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Connection.Protocol;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.TransferCapability;
import org.dcm4che3.net.service.DicomServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fhes.cat.config.APIConstants;
import fhes.cat.dmdcm4che3.impl.BasicCEchoSCPImpl;
import fhes.cat.dmdcm4che3.impl.BasicCStoreSCPImpl;
import fhes.cat.dmdcm4che3.impl.BasicMPPSImpl;
import fhes.cat.dmdcm4che3.impl.WorkListCustomImpl;
import fhes.cat.services.MyServletContextListener;
import fhes.cat.services.impl.AssociationHandlerImpl;
import services.SocketService;
import services.impl.BaseServiceImpl;
import services.impl.SocketServiceImpl;



@Component
public class DicomServer extends BaseServiceImpl{
	
	private static final Logger log = LoggerFactory.getLogger(DicomServer.class);
	
	private Device device;
	
	private SocketService socketService = new SocketServiceImpl();
	
	
	// TS “preferidas” para STORAGE (MR/CT/US…)
	final String[] TS_STORAGE = new String[] {
	    UID.ExplicitVRLittleEndian, // 1.2.840.10008.1.2.1
	    "1.2.840.10008.1.2.4.50",   // JPEG Baseline (Process 1)
	    "1.2.840.10008.1.2.4.80"    // JPEG-LS Lossless
	    // UID.ImplicitVRLittleEndian // <-- reactivarlo cuando tengas listo el camino Implicit
	};

	// TS para VERIFICATION (C-ECHO)
	final String[] TS_ECHO = new String[] {
	    UID.ImplicitVRLittleEndian, // 1.2.840.10008.1.2
	    UID.ExplicitVRLittleEndian  // 1.2.840.10008.1.2.1
	};
	
	final String VERIFICATION_SOP = "1.2.840.10008.1.1"; // Verification SOP Class
	
	@Autowired
	MyServletContextListener servletContextListener;

	@PostConstruct
	public void DicomServerPost() {

	}
	public void createStorageServer(int portNumber, String serverAET, boolean guardarDicom, boolean reenviarDicom, int origen) {
		log.info("\nCreando Servidor puerto: {}. AET:_{}, guardarDicom: {}, reenviarDicom: {}, origen: {}\n", portNumber, serverAET, guardarDicom, reenviarDicom, origen);
		ApplicationEntity localAE = new ApplicationEntity(serverAET);

	    
        device = new Device("SERVER DICOM"); 
        Connection connection = new Connection();
        connection.setIdleTimeout(0);
        device.addConnection(connection);
               
        device.addApplicationEntity(localAE);
 
              
        
        
        APIConstants.capacidadTransferencia.forEach(cap -> {
            String sopUID = cap.getValor2(); // asegúrate: aquí viene el SOP Class UID
            String[] ts = VERIFICATION_SOP.equals(sopUID) ? TS_ECHO : TS_STORAGE;

            localAE.addTransferCapability(new TransferCapability(
                null,           // common name opcional
                sopUID,         // SOP Class
                TransferCapability.Role.SCP,
                ts              // Transfer Syntaxes aceptadas para ese SOP
            ));
        });

        String ipAddress = "0.0.0.0";
        
        connection.setHostname(ipAddress); //ip del servidor donde reside la app, habría que cogerla dinámicamente
        connection.setPort(portNumber);
        connection.setProtocol(Protocol.DICOM);
        connection.setInstalled(true);
        
        localAE.addConnection(connection);
        
        device.setAssociationHandler(new AssociationHandlerImpl()); //gestiona el evento de negociación
        
        device.setDimseRQHandler(createServiceRegistryStorage(guardarDicom, reenviarDicom, origen));
        device.setExecutor(Executors.newCachedThreadPool());
        
              
        log.error(" Servidor Storage DICOM iniciado en el puerto {} ", connection.getPort());
        
        try {
			device.bindConnections();
			servletContextListener.addDevice(device);
		} catch (IOException | GeneralSecurityException e1) {
			socketService.notificarMissatge(SocketServiceImpl.MISSATGE_CRITICAL, "Error DICOM", "No s'ha pogut iniciar el servidor DICOM - Storage --" + e1.getMessage());
			log.info("^^^^^^^^^^^^^^^^^^^^^^ Error conexión: {}", e1);
			e1.printStackTrace();
		}

	}
	
	

	public void createFindServer(int portNumber, String serverAET) {
		ApplicationEntity localAE = new ApplicationEntity(serverAET);
        device = new Device("SERVER DICOM"); 
        Connection connection = new Connection();
        connection.setIdleTimeout(0);
        device.addConnection(connection);
        device.addApplicationEntity(localAE); 
              
        String[] transferSyntaxes = {
        		"1.2.840.10008.1.2.1", // (Explicit VR Little Endian)
        		"1.2.840.10008.1.2", // (Implicit VR Little Endian)
        		"1.2.840.10008.1.1", // (Verification SOP Class)
        };
        

        
//        APIConstants.presentationContext.forEach(cap -> {
//        	localAE.addTransferCapability(new TransferCapability(cap.getValor1(), cap.getValor2(), TransferCapability.Role.SCP, transferSyntaxes));
//        });
        
        localAE.addTransferCapability(new TransferCapability("Query/Retrieve SOP Class", "1.2.840.10008.5.1.4.1.2.2.1", TransferCapability.Role.SCP, transferSyntaxes));
        localAE.addTransferCapability(new TransferCapability("Verification SOP Class", "1.2.840.10008.1.1", TransferCapability.Role.SCP,transferSyntaxes));
        localAE.addTransferCapability(new TransferCapability("Worklist information Model", "1.2.840.10008.5.1.4.31", TransferCapability.Role.SCP,transferSyntaxes));
        
        String ipAddress = "0.0.0.0";
        
        connection.setHostname(ipAddress); //ip del servidor donde reside la app, habría que cogerla dinámicamente
        connection.setPort(portNumber);
        connection.setProtocol(Protocol.DICOM);
        connection.setInstalled(true);
        localAE.addConnection(connection);
        
        device.setDimseRQHandler(createServiceRegistryFind()); 
        device.setExecutor(Executors.newCachedThreadPool());
              
        log.info(" Servidor FIND DICOM iniciado en el puerto {} ", connection.getPort());
        
        try {
			device.bindConnections();
			servletContextListener.addDevice(device);
		} catch (IOException | GeneralSecurityException e1) {
			socketService.notificarMissatge(SocketServiceImpl.MISSATGE_CRITICAL, "Error DICOM", "No s'ha pogut iniciar el servidor DICOM - FIND -- " + e1.getMessage());
			log.info("^^^^^^^^^^^^^^^^^^^^^^ Error conexión: {}", e1);
			e1.printStackTrace();
		}      
		
	}
	
	public void createMPPServer(int portNumber, String serverAET) {
		ApplicationEntity localAE = new ApplicationEntity(serverAET);
        device = new Device("SERVER DICOM"); 
        Connection connection = new Connection();
        connection.setIdleTimeout(0);
        device.addConnection(connection);
        device.addApplicationEntity(localAE); 
              
        String[] transferSyntaxes = {
        		"1.2.840.10008.1.2.1", // (Explicit VR Little Endian)
        		"1.2.840.10008.1.2", // (Implicit VR Little Endian)
        		"1.2.840.10008.1.1", // (Verification SOP Class)
        };
        
        APIConstants.capacidadTransferenciaMpps.forEach(cap -> {
        	localAE.addTransferCapability(new TransferCapability(cap.getValor1(), cap.getValor2(), TransferCapability.Role.SCP, transferSyntaxes));
        });
                        
        String ipAddress = "0.0.0.0";

        connection.setHostname(ipAddress); //ip del servidor donde reside la app
        connection.setPort(portNumber);
        connection.setProtocol(Protocol.DICOM);
        connection.setInstalled(true);
        localAE.addConnection(connection);
        
        device.setDimseRQHandler(createServiceRegistryMPPS()); 
        device.setExecutor(Executors.newCachedThreadPool());
              
        log.info(" Servidor MPPS DICOM iniciado en el puerto {} ", connection.getPort());
        
        try {
			device.bindConnections();
			servletContextListener.addDevice(device);
		} catch (IOException | GeneralSecurityException e1) {
			socketService.notificarMissatge(SocketServiceImpl.MISSATGE_CRITICAL, "Error DICOM", "No s'ha pogut iniciar el servidor DICOM - MPPS -- " + e1.getMessage());
			log.info("^^^^^^^^^^^^^^^^^^^^^^ Error conexión: {}", e1);
			e1.printStackTrace();
		}      
		
	}
	
	
		
	private DicomServiceRegistry createServiceRegistryStorage(boolean guardarDicom, boolean reenviarDicom, int origen) {
		DicomServiceRegistry serviceRegistry = new DicomServiceRegistry();

  
		serviceRegistry.addDicomService(new BasicCEchoSCPImpl());
		serviceRegistry.addDicomService(new BasicCStoreSCPImpl(guardarDicom, reenviarDicom, origen));
		
		return serviceRegistry;
	}

	private DicomServiceRegistry createServiceRegistryFind() {
		DicomServiceRegistry serviceRegistry = new DicomServiceRegistry();
 
		serviceRegistry.addDicomService(new BasicCEchoSCPImpl());
		serviceRegistry.addDicomService(new WorkListCustomImpl(UID.ModalityWorklistInformationModelFind));       

		return serviceRegistry;
  
	}
	
	private DicomServiceRegistry createServiceRegistryMPPS() {
		DicomServiceRegistry serviceRegistry = new DicomServiceRegistry();
 
		serviceRegistry.addDicomService(new BasicMPPSImpl());

		return serviceRegistry;
  
	}
	
	
	
	private SSLContext createSSLContext(String protocol, String keyStorePath, String keyStorePassword, String trustStorePath, String trustStorePassword) throws GeneralSecurityException, IOException {
	    KeyStore keyStore = KeyStore.getInstance("JKS");
	    try (FileInputStream keyStoreIS = new FileInputStream(keyStorePath)) {
	        keyStore.load(keyStoreIS, keyStorePassword.toCharArray());
	    }

	    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
	    kmf.init(keyStore, keyStorePassword.toCharArray());

	    KeyStore trustStore = KeyStore.getInstance("JKS");
	    try (FileInputStream trustStoreIS = new FileInputStream(trustStorePath)) {
	        trustStore.load(trustStoreIS, trustStorePassword.toCharArray());
	    }

	    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
	    tmf.init(trustStore);

	    SSLContext sslContext = SSLContext.getInstance(protocol);
	    sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

	    return sslContext;
	}
	
	
}
