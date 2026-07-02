package fhes.cat.dto;

import java.util.Calendar;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel
public class RestApiResponse {

	public static final String MEDIA_TYPE_ZIP = "application/zip";
	public static final String MEDIA_TYPE_XLS = "application/vnd.ms-excel";
	public static final String MEDIA_TYPE_JSON = "application/json";
	public static final String APPLICATION_OCTET_STREAM_VALUE = "application/octet-stream";
	
	@ApiModelProperty(position = 1, value = "Marca de tiempo")
	private long timestamp;
	
	@ApiModelProperty(position = 2, value = "Código de salida")
	private int exitCode;


	@ApiModelProperty(position = 2, value = "Mensaje de respuesta")
	private String message;


	@ApiModelProperty(position = 3, value = "Datos de respuesta")
	private Object data;

	public RestApiResponse() {
		timestamp = Calendar.getInstance().getTimeInMillis();
	}

	private RestApiResponse(Object data) {
		this("", data);
	}

	public RestApiResponse(String message, Object data) {
		this.message = message;
		this.data = data;
	}
	
	public RestApiResponse(int exitCode, String message, Object data) {
		this.exitCode = exitCode;
		this.message = message;
		this.data = data;
	}

	public static final RestApiResponse allRight() {
		return new RestApiResponse("All right", "OK");
	}

	public static final RestApiResponse allRight(Object response) {
		return new RestApiResponse(response);
	}

	public static final RestApiResponse empty() {
		return new RestApiResponse("Empty Object", "");
	}

	public static final RestApiResponse checkNull(Object response) {
		return response != null?allRight(response):empty();
	}

	/**
	 * @param content
	 * @param contentType
	 * @param name
	 * @return
	 */
	public static ResponseEntity<byte[]> attachmentResponseEntity(byte[] content, String contentType, String name) {
	    HttpHeaders header = new HttpHeaders();
	    header.set("Content-Type", contentType);
	    header.set(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=%s",name));
	    header.setContentLength(content.length);
	    return new ResponseEntity<>(content, header, HttpStatus.OK);
	}
	
	/**
	 * Descargar archivo de cualquier formato
	 * @param content
	 * @param name
	 * @return
	 */
	public static ResponseEntity<byte[]> attachmentAnyResponseEntity(byte[] content, String name) {
	    return attachmentResponseEntity(content, MediaType.APPLICATION_OCTET_STREAM_VALUE, name);
	}

}