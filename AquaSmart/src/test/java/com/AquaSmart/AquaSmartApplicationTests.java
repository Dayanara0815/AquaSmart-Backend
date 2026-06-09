package com.AquaSmart;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

import com.AquaSmart.service.DashboardService;
import com.AquaSmart.repository.MedidorRepository;
import com.AquaSmart.model.Medidor;
import com.AquaSmart.service.impl.DashboardServiceImpl;

@SpringBootTest
class AquaSmartApplicationTests {

	@Autowired
	private DashboardService dashboardService;

	@Autowired
	private DashboardServiceImpl dashboardServiceImpl;

	@Autowired
	private MedidorRepository medidorRepository;

	@Test
	void testToggleValve() {
		String email = "maria.quispe@example.com";
		System.out.println("TEST START: toggling valve to false...");
		dashboardService.setValve(false, email);
		Medidor m1 = medidorRepository.findAllWithDetails().stream()
			.filter(m -> m.titular != null && email.equalsIgnoreCase(m.titular.correo))
			.findFirst().orElse(null);
		assertNotNull(m1);
		assertEquals("Cerrada", m1.estadoValvula.nombreEstadoValvula);

		System.out.println("TEST: toggling valve to true...");
		dashboardService.setValve(true, email);
		Medidor m2 = medidorRepository.findAllWithDetails().stream()
			.filter(m -> m.titular != null && email.equalsIgnoreCase(m.titular.correo))
			.findFirst().orElse(null);
		assertNotNull(m2);
		assertEquals("Abierta", m2.estadoValvula.nombreEstadoValvula);
	}

	@Test
	void testAutoCloseFugaPreference() {
		String email = "maria.quispe@example.com";
		
		// 1. Desactivar autoCierreFuga con presencia en casa
		dashboardService.setAutoClose(false, email);
		dashboardService.setHomePresence(true, email);
		dashboardService.setValve(true, email);
		
		// Correr el simulador que evalúa fugas
		dashboardServiceImpl.simulateSensorReadings();
		
		// Comprobar que sigue abierta
		Medidor m1 = medidorRepository.findAllWithDetails().stream()
			.filter(m -> m.titular != null && email.equalsIgnoreCase(m.titular.correo))
			.findFirst().orElse(null);
		assertNotNull(m1);
		assertEquals("Abierta", m1.estadoValvula.nombreEstadoValvula);

		// 2. Activar autoCierreFuga con presencia en casa
		dashboardService.setAutoClose(true, email);
		
		// Correr simulador
		dashboardServiceImpl.simulateSensorReadings();
		
		// Comprobar que se cerró automáticamente
		Medidor m2 = medidorRepository.findAllWithDetails().stream()
			.filter(m -> m.titular != null && email.equalsIgnoreCase(m.titular.correo))
			.findFirst().orElse(null);
		assertNotNull(m2);
		assertEquals("Cerrada", m2.estadoValvula.nombreEstadoValvula);

		// 3. Fuera de casa con autoCierreFuga desactivado -> debe cerrarse igual
		dashboardService.setAutoClose(false, email);
		dashboardService.setHomePresence(false, email);
		dashboardService.setValve(true, email);
		
		// Correr simulador
		dashboardServiceImpl.simulateSensorReadings();
		
		// Comprobar que se cerró automáticamente
		Medidor m3 = medidorRepository.findAllWithDetails().stream()
			.filter(m -> m.titular != null && email.equalsIgnoreCase(m.titular.correo))
			.findFirst().orElse(null);
		assertNotNull(m3);
		assertEquals("Cerrada", m3.estadoValvula.nombreEstadoValvula);
	}

	@Autowired
	private com.AquaSmart.controller.UserController userController;

	@Autowired
	private com.AquaSmart.service.ReportService reportService;

	@Test
	void testNewUserReportAnomalyCount() {
		String testEmail = "test.newuser@example.com";
		java.util.Map<String, String> body = java.util.Map.of(
			"nombre", "Test",
			"apellidoPaterno", "User",
			"correo", testEmail,
			"rol", "DOMESTICO"
		);
		
		// Registrar el nuevo usuario
		Object registerResult = userController.registerUser(body);
		assertNotNull(registerResult);
		
		// Obtener el reporte semanal
		java.time.LocalDate today = java.time.LocalDate.now();
		com.AquaSmart.dto.WeeklyReportDto report = reportService.getWeeklyReport(
			today.minusDays(6),
			today,
			testEmail
		);
		
		assertNotNull(report);
		// El usuario tiene 3 consumos normales sembrados de forma predeterminada
		// No deberían considerarse anomalías (debe haber 0 días anómalos)
		assertEquals(0, report.anomalyCount(), "Un nuevo usuario registrado con consumos normales debería tener 0 días anómalos.");
	}
}
