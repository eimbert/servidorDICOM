package fhes.cat.services;

import org.dcm4che3.data.Attributes;

import fhes.cat.dto.BuscarDicomDTO;

public interface SendDicomToPACS {

	//public boolean sendDICOM(Attributes atr, String tsuid, String cuid, String iuid);
	public boolean sendDICOM(Attributes atr, String tsuid, String cuid, String iuid, String calledAET, String callingAET, String ipCalled, int portCalled);
	public boolean sendCMove(Attributes atr, String localAETitle);
	public boolean sendCFind(BuscarDicomDTO atr);
	public boolean deleteDicomImage(String cuid, String iuid);
	
	public boolean sendECGtoIMG(Attributes atr);
}
