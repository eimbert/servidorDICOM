package fhes.cat.services;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.dcm4che3.net.Device;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MyServletContextListener implements ServletContextListener{

	private List<Device> devices = new ArrayList<>(); 
	
	@Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        log.info("Cerrando App y cerrando {} puertos...", devices.size());
        devices.forEach(d -> {
        	d.unbindConnections();
        });
    }
	
	public void addDevice(Device device) {
		devices.add(device);
	}
}
