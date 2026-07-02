package fhes.cat.dmdcm4che3.impl;

import java.io.IOException;

import org.dcm4che3.net.Association;
import org.dcm4che3.net.AssociationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssociationListenerImpl implements AssociationListener{

	private static final Logger log = LoggerFactory.getLogger(AssociationListenerImpl.class);
	
	@Override
	public void onClose(Association association) {
		try {
			log.info("************ Association {} closed ************", association);
			association.release();
		} catch (IOException e) {
			log.info("Error release association: {}", e.getMessage());
		}
		
	}
}
