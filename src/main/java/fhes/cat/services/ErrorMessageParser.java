package fhes.cat.services;

import fhes.cat.dto.ErrorMessageParserDTO;

public interface ErrorMessageParser {

	 public ErrorMessageParserDTO parseErrorMessage(String errorMessage);
}
