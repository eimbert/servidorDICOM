package fhes.cat;


//@Slf4j
//@Component
//public class EventosAplicacion  implements ApplicationListener<ApplicationStartedEvent> {
//	
//	@Autowired
//	private DicomServer mwlist;
	

	
//	@Override
//	public void onApplicationEvent(ApplicationStartedEvent event) {
//		log.info("Paso crear servers");
//		BaseService baseService = new BaseServiceImpl();
//		
//		APIConstants.servidores.forEach(server ->{
//			log.info("Servidor: {}", server.getValor1());
//		});
		
//		APIConstants.servidores.forEach(server ->{
//			if(server.getCamp().equals(APIConstants.STORAGE)) {
//				mwlist.createStorageServer(Integer.parseInt(server.getValor1()), server.getValor2(), true, false, false);
//			}else if(server.getCamp().equals(APIConstants.FIND)) {
//				mwlist.createFindServer(Integer.parseInt(server.getValor1()), server.getValor2());
//			}else if(server.getCamp().equals(APIConstants.MPPS)) {
//				mwlist.createMPPServer(Integer.parseInt(server.getValor1()), server.getValor2());
//			}
//		});
		
//		mwlist.createFindServer(210, "SDFHES");
//		mwlist.createMPPServer(215, "SDFHES");
//		mwlist.createStorageServer(211, "SDFHES", true, false, false);//puerto imagenes internas
//		mwlist.createStorageServer(212, "SDFHES", true, false, true);//puerto imagenes externas
//	}

//}
