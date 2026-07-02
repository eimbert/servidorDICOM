package fhes.cat.dmdcm4che3.utilities;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.VR;
import org.dcm4che3.util.TagUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DisplayTagVisitor implements Attributes.Visitor {

	private static final Logger log = LoggerFactory.getLogger(DisplayTagVisitor.class);
	
	@Override
	public boolean visit(Attributes attrs, int tag, VR vr, Object value) throws Exception {
		//log.info("\nTag: {}, VR: {}, Value: {} ", TagUtils.toString(tag), vr, attrs.getString(tag));
        
		return true;
	}

}
