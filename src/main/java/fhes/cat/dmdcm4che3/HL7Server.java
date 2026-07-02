package fhes.cat.dmdcm4che3;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.app.SimpleServer;
import ca.uhn.hl7v2.llp.LowerLayerProtocol;
import ca.uhn.hl7v2.llp.MinLowerLayerProtocol;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.protocol.ReceivingApplication;
import ca.uhn.hl7v2.protocol.ReceivingApplicationException;

@Component
public class HL7Server {

	private static final Logger log = LoggerFactory.getLogger(HL7Server.class);
	private SimpleServer server;

	public void ServidorHL7(int port) {

		// Crear el servidor HL7
		LowerLayerProtocol llp = new MinLowerLayerProtocol();
		PipeParser parser = new PipeParser();
		SimpleServer server = new SimpleServer(port, llp, parser);

		HL7MessageHandler handler = new HL7MessageHandler();
		server.registerApplication("*", "*", handler);

		server.start();
		log.info("HL7 Server is listening on port " + port);

	}

	public void detenerServidor() {
        if (server != null) {
            server.stop();
            log.info("HL7 Server has been stopped");
        }
    }
	
	public class HL7MessageHandler implements ReceivingApplication {

		@Override
		public boolean canProcess(Message theMessage) {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
        public Message processMessage(Message message, Map theMetadata) throws ReceivingApplicationException, HL7Exception {
            log.info("Received message:\n" + message.encode());
            try {
                return message.generateACK();
            } catch (HL7Exception | IOException e) {
                log.error("Error while processing message", e);
                throw new ReceivingApplicationException(e);
            }
        }
	}

}