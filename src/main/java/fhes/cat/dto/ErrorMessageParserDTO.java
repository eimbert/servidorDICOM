package fhes.cat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ErrorMessageParserDTO {

	private String resultCode;
    private String source;
    private String reason;
        
}
