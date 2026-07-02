package fhes.cat.dmdcm4che3;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Commands;
import org.dcm4che3.net.Dimse;
import org.dcm4che3.net.Status;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.AbstractDicomService;
import org.dcm4che3.net.service.DicomServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fhes.cat.config.APIConstants;
import fhes.cat.dto.BusquedaPacienteDTO;
import fhes.cat.dto.RestApiResponse;
import lombok.Getter;
import lombok.Setter;
import services.impl.BaseServiceImpl;

@Getter
@Setter
public abstract class WorkListCustom extends AbstractDicomService{

	private static final Logger log = LoggerFactory.getLogger(WorkListCustom.class);
	private static final DateTimeFormatter DICOM_DA = DateTimeFormatter.BASIC_ISO_DATE; // yyyyMMdd
	
	private BaseServiceImpl baseService = new BaseServiceImpl();
	
	private List<Attributes> pacientes;
	private int finalCode;
	//private BusquedaPacienteDTO busquedaPaciente;
	
	public WorkListCustom(String... sopClasses) {
		super(sopClasses);
		this.finalCode = Status.Success;
		pacientes = new ArrayList<Attributes>();
		
	}

	public void addPaciente(Attributes paciente) {
		this.pacientes.add(paciente);
	}
	
	public void clearList() {
		this.pacientes.clear();
	}
	
	@Override
	protected void onDimseRQ(Association as, PresentationContext pc, Dimse dimse, Attributes cmd, Attributes data) throws IOException {
		
		log.info("\nAttributes cmd {}", cmd);
		log.info("\nAttributes data {}", data);
		
		if (dimse != Dimse.C_FIND_RQ)
            throw new DicomServiceException(Status.UnrecognizedOperation);
		
		
		if(this.cargarDtoBusqueda(data, as)) {
			if(generarWorkList(as, pc, cmd, data)) {
	        	as.writeDimseRSP(pc, Commands.mkCFindRSP(cmd, Status.Success));
	        }else {
	        	as.writeDimseRSP(pc, Commands.mkCFindRSP(cmd, this.finalCode));
	        }
		}else
			as.writeDimseRSP(pc, Commands.mkCFindRSP(cmd, this.finalCode));
	}
	
	protected abstract boolean buscarPacientes(BusquedaPacienteDTO busquedaPacientes);
	
	protected boolean generarWorkList(Association as, PresentationContext pc, Attributes rq, Attributes rp) throws DicomServiceException {
		
		log.info("********************************************** Generando Work List C_FIND *********************************************************");

	    // Verificar si la lista de pacientes está vacía
	    if (pacientes.isEmpty()) {
	        // Aquí puedes establecer this.finalCode a un código de status específico si es necesario
	        // Por ejemplo, Status.NoSuchObjectInstance si quieres indicar que no se encontraron pacientes
	        this.finalCode = Status.NoSuchObjectInstance; // Asegúrate de definir este código de status correctamente según tu lógica de aplicación
	        return false; // Retorna false para indicar que no se generó ninguna worklist
	    }
	    
    	pacientes.forEach(p -> {
    		try {
    			Attributes rsp = new Attributes();
    			rsp.addAll(p);
    			as.writeDimseRSP(pc, Commands.mkCFindRSP(rq, Status.Pending), rsp);
    			
    			log.info("\nWorklist item enviado: \n{}", rsp);
			} catch (IOException e) {
				this.finalCode = Status.ProcessingFailure;
				e.printStackTrace();
			}
    	});
		        		
        return true;
    }
	
	private boolean cargarDtoBusqueda(Attributes dto, Association as) {
		ObjectMapper mapper = new ObjectMapper();
		
		log.info("\n*************Cargando datos búsqueda: {}", dto);
		
		String modalidad = null;
		var spsSeq = dto.getSequence(Tag.ScheduledProcedureStepSequence);
		if (spsSeq != null && !spsSeq.isEmpty()) {
		    modalidad = spsSeq.get(0).getString(Tag.Modality);
		}
		if (modalidad == null || modalidad.isBlank()) modalidad = ""; // o tu default
//		String modalidad = dto.getSequence(Tag.ScheduledProcedureStepSequence).get(0).getString(Tag.Modality);
		
		String maquina = as.getCallingAET(); // dto.getSequence(Tag.ScheduledProcedureStepSequence).get(0).getString(Tag.ScheduledStationAETitle);
		
		log.info("url: {}"+ APIConstants.urlEntono);
		log.info("Modalidad: {}", modalidad);
		log.info("Nombre maquina: {}", maquina);
		BusquedaPacienteDTO busquedaDto = new BusquedaPacienteDTO(modalidad, maquina);
		
		if (areAllFieldsEmpty(dto)) {
	        log.info("Todos los campos del DTO están vacíos. Busco alguna lista valida para maquina modalidad");
	        String url = APIConstants.urlEntono;
			RestTemplate restTemplate = new RestTemplate();
			try {
				log.info("\nBusqueda para generar la WorkList: " + mapper.writeValueAsString(busquedaDto));
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			List<BusquedaPacienteDTO> resultadoBusqueda = baseService.mapToList(BusquedaPacienteDTO.class, baseService.mapToObj(RestApiResponse.class, restTemplate.postForObject(url, busquedaDto , Object.class)).getData());
	        llenarPacientes(resultadoBusqueda, maquina);
	        return true;
	    }
		
		BusquedaPacienteDTO busquedaPaciente = new BusquedaPacienteDTO();
		busquedaPaciente.setAccessionNumber(dto.getString(Tag.AccessionNumber));
		busquedaPaciente.setPatientBirthDate(dto.getString(Tag.PatientBirthDate));
		busquedaPaciente.setPatientID(dto.getString(Tag.PatientID));
		busquedaPaciente.setPatientName(dto.getString(Tag.PatientName));
		busquedaPaciente.setPatientSex(dto.getString(Tag.PatientSex));
		busquedaPaciente.setModality(modalidad);
		busquedaPaciente.setDevice(maquina);
		
		return buscarPacientes(busquedaPaciente);
				
	}
	protected void llenarPacientes(List<BusquedaPacienteDTO> listaPacientes, String maquina) {
		pacientes.clear();
		
		listaPacientes.forEach(p -> {
		    Attributes wl = new Attributes();

		    // Patient
		    if (p.getPatientID() != null)
		        wl.setString(Tag.PatientID, VR.LO, String.valueOf(p.getPatientID()));

		    String pn = ((p.getPatientFirstSurname() == null ? "" : p.getPatientFirstSurname()) + " "
		              + (p.getPatientSecondSurname() == null ? "" : p.getPatientSecondSurname()))
		              .trim()
		              + "^"
		              + (p.getPatientName() == null ? "" : p.getPatientName().trim());

		    pn = pn.replaceAll("\\s*\\^\\s*", "^").trim();
		    if (!pn.equals("^"))
		        wl.setString(Tag.PatientName, VR.PN, pn);

		    if (p.getPatientBirthDate() != null && !p.getPatientBirthDate().isBlank())
		        wl.setString(Tag.PatientBirthDate, VR.DA, p.getPatientBirthDate());

		    if (p.getPatientSex() != null && !p.getPatientSex().isBlank())
		        wl.setString(Tag.PatientSex, VR.CS, p.getPatientSex());

		    // Order/Study identifiers
		    if (p.getAccessionNumber() != null && !p.getAccessionNumber().isBlank())
		        wl.setString(Tag.AccessionNumber, VR.SH, p.getAccessionNumber());

		    if (p.getStudyInstanceUID() != null && !p.getStudyInstanceUID().isBlank())
		        wl.setString(Tag.StudyInstanceUID, VR.UI, p.getStudyInstanceUID());

		    if (p.getIdCita() != null)
		        wl.setString(Tag.RequestedProcedureID, VR.SH, String.valueOf(p.getIdCita()));

		    //wl.setString(Tag.ScheduledProcedureStepID, VR.SH, "1");
		    wl.setString(Tag.RequestedProcedureID, VR.SH, "1"); //para compatibilidad con equipos que requieren algún valor en este campo
		    
		    // SPS (MWL core)
		    Attributes sps = new Attributes();
		    sps.setString(Tag.Modality, VR.CS, p.getModality());
		    // AE Title exacto del equipo
		    sps.setString(Tag.ScheduledStationAETitle, VR.AE, maquina);

		    // Fecha del sistema en formato DICOM DA: yyyyMMdd
		    String today = LocalDate.now(ZoneId.of("Europe/Madrid")).format(DICOM_DA);
		    sps.setString(Tag.ScheduledProcedureStepStartDate, VR.DA, today);

		    // Hora NO se incluye (omitida a propósito)
		    wl.newSequence(Tag.ScheduledProcedureStepSequence, 1).add(sps);

		    // Extras     
		    log.info("Datos de idCita {} e informe {}",  p.getIdCita(), p.getInformar()+"");
		    if ("ECG".equalsIgnoreCase(p.getModality())) {
		        wl.setString(Tag.OtherPatientIDs, VR.LO, String.valueOf(p.getIdCita()));
		        wl.setString(Tag.StudyDescription, VR.LO, String.valueOf(p.getInformar()));
		    }

		    pacientes.add(wl);
		});

	}
	
	private boolean areAllFieldsEmpty(Attributes dto) {
	    String accessionNumber = dto.getString(Tag.AccessionNumber);
	    String patientBirthDate = dto.getString(Tag.PatientBirthDate);
	    String patientID = dto.getString(Tag.PatientID);
	    String patientName = dto.getString(Tag.PatientName);
	    String patientSex = dto.getString(Tag.PatientSex);
				
		try {
			return (accessionNumber == null || accessionNumber.isEmpty()) &&
			       (patientBirthDate == null || patientBirthDate.isEmpty()) &&
			       (patientID == null || patientID.isEmpty()) &&
			       (patientName == null || patientName.isEmpty()) &&
			       (patientSex == null || patientSex.isEmpty());
		}catch(Exception e) {
			log.info("Error: {}", e.getMessage());
			return false;
		}
	}
}
