package fhes.cat.services.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import fhes.cat.dto.ErrorMessageParserDTO;
import fhes.cat.services.ErrorMessageParser;

@Service
public class ErrorMessageParserImpl implements ErrorMessageParser {

	@Override
	public ErrorMessageParserDTO parseErrorMessage(String errorMessage) {
		Pattern pattern = Pattern.compile("A-ASSOCIATE-RJ\\[result: (.*?), source: (.*?), reason: (.*?)\\]");
        Matcher matcher = pattern.matcher(errorMessage);
	
        if (matcher.find()) {
            String resultCode = matcher.group(1);
            String source = matcher.group(2);
            String reason = matcher.group(3);

            return new ErrorMessageParserDTO(resultCode, source, reason);
        } else {
            return null;
        }
	}

}
