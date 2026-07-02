package fhes.cat.dmdcm4che3.impl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.dcm4che3.imageio.plugins.dcm.DicomImageReadParam;

public class DicomImageViewer {

	public BufferedImage dicomImage(byte[] dicomData) {
		 ByteArrayInputStream bais = new ByteArrayInputStream(dicomData);
		    BufferedImage buff = null;
		    Iterator<ImageReader> iter = ImageIO.getImageReadersByFormatName("DICOM");
		    ImageReader reader = (ImageReader) iter.next();
		    DicomImageReadParam param = (DicomImageReadParam) reader.getDefaultReadParam();
		    try {
		    	ImageInputStream iis = ImageIO.createImageInputStream(bais);
		    	reader.setInput(iis, false);
		    
				buff = reader.read(0, param);
				iis.close();
				if (buff == null) {
					throw new IOException("Could not read Dicom file. Maybe pixel data is invalid.");
		    }
			} catch (IOException e) {
				e.printStackTrace();
			}
		    
		    return buff;
	}
}
