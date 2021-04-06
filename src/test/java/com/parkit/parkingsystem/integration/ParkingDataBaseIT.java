package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static final DataBaseTestConfig    dataBaseTestConfig = new DataBaseTestConfig();
    private static       FareCalculatorService fareCalculatorService;
    @Spy
    private static ParkingSpotDAO         parkingSpotDAO;
    @Spy
    private static TicketDAO              ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    private Ticket          ticket;

    @BeforeAll
    private static void setUp() throws Exception {

        parkingSpotDAO                = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO                     = new TicketDAO();
        ticketDAO.dataBaseConfig      = dataBaseTestConfig;
        dataBasePrepareService        = new DataBasePrepareService();
        fareCalculatorService         = new FareCalculatorService(ticketDAO);

    }

    @BeforeEach
    private void setUpPerTest() throws Exception {

        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown() {

    }

    /*************************************test Exceptions*******************************************/

    @Test
    public void testIncomingError() {
        try{
            when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("");

            ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

            parkingService.processIncomingVehicle();

        }
        catch (Exception e){
            assertNotNull(e);
        }

    }

    @Test
    public void testExitingErrorExeption() {
        try{

            ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
            when(ticketDAO.getTicket("ABCDEF")).thenReturn(null);
            parkingService.processExitingVehicle();
            testParkingACar();
        }
        catch (Exception e){
            assertNotNull(e);
        }

    }

    @Test
    public void testParkingABike() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(2);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        Date           dateIn         = new Date();
        TimeUnit.SECONDS.sleep(1);
        parkingService.processIncomingVehicle();
        TimeUnit.SECONDS.sleep(1);
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
        assertEquals(5, parkingSpotDAO.getNextAvailableSlot(ParkingType.BIKE));
        verify(ticketDAO, Mockito.times(1)).saveTicket(any(Ticket.class));
        ticket = ticketDAO.getTicket("ABCDEF");
        assertEquals(1, ticket.getId());
        assertEquals(4, ticket.getParkingSpot().getId());
        assertEquals("ABCDEF", ticket.getVehicleRegNumber());
        assertEquals(0.0, ticket.getPrice());
        assertNotNull(ticket.getInTime());
        assertTrue(ticket.getInTime().after(dateIn));
        assertNull(ticket.getOutTime());

    }



    /*************************************test parking with database*******************************************/

    @Test
    public void testParkingACar() throws InterruptedException {

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        Date           dateIn         = new Date();
        TimeUnit.SECONDS.sleep(1);
        parkingService.processIncomingVehicle();
        TimeUnit.SECONDS.sleep(1);
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
        assertEquals(2, parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR));
        verify(ticketDAO, Mockito.times(1)).saveTicket(any(Ticket.class));
        ticket = ticketDAO.getTicket("ABCDEF");
        assertEquals(1, ticket.getId());
        assertEquals(1, ticket.getParkingSpot().getId());
        assertEquals("ABCDEF", ticket.getVehicleRegNumber());
        assertEquals(0.0, ticket.getPrice());
        assertNotNull(ticket.getInTime());
        assertTrue(ticket.getInTime().after(dateIn));
        assertNull(ticket.getOutTime());

    }

    @Test
    public void testParkingLotExit() throws InterruptedException {

        testParkingACar();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        Date           dateAfter      = new Date();
        TimeUnit.SECONDS.sleep(1);
        parkingService.processExitingVehicle();
        TimeUnit.SECONDS.sleep(1);
        verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
        Ticket NewTicket = ticketDAO.getTicket("ABCDEF");
        assertEquals(ticket.getId(), NewTicket.getId());
        assertEquals(ticket.getParkingSpot(), NewTicket.getParkingSpot());
        assertEquals(ticket.getVehicleRegNumber(), NewTicket.getVehicleRegNumber());
        assertEquals(ticket.getInTime(), NewTicket.getInTime());
        assertNotNull(NewTicket.getOutTime());
        assertTrue(NewTicket.getOutTime().after(dateAfter));
        double duration = SECONDS.between(NewTicket.getInTime().toInstant(), NewTicket.getOutTime().toInstant());
        assertEquals(duration > 1800 ? duration / 3600.0 * Fare.CAR_RATE_PER_HOUR : 0, NewTicket.getPrice());
        verify(parkingSpotDAO, Mockito.times(2)).updateParking(any(ParkingSpot.class));
        assertEquals(1, parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR));
    }

    @Test
    public void testRecurrentUser() throws InterruptedException, SQLException, ClassNotFoundException {
        /********************QUIT THE PARKING******************************/
        testParkingACar();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        Date           dateAfter      = new Date();
        TimeUnit.SECONDS.sleep(1);
        parkingService.processExitingVehicle();
        TimeUnit.SECONDS.sleep(1);
        verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
        Ticket NewTicket = ticketDAO.getTicket("ABCDEF");
        assertEquals(ticket.getId(), NewTicket.getId());
        assertEquals(ticket.getParkingSpot(), NewTicket.getParkingSpot());
        assertEquals(ticket.getVehicleRegNumber(), NewTicket.getVehicleRegNumber());
        assertEquals(ticket.getInTime(), NewTicket.getInTime());
        assertNotNull(NewTicket.getOutTime());
        assertTrue(NewTicket.getOutTime().after(dateAfter));

        /***********************************GET IN PARKING***********************************/

        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        Date dateIn = new Date();
        TimeUnit.SECONDS.sleep(1);
        parkingService.processIncomingVehicle();
        TimeUnit.SECONDS.sleep(1);
        verify(parkingSpotDAO, Mockito.times(3)).updateParking(any(ParkingSpot.class));
        assertEquals(2, parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR));
        verify(ticketDAO, Mockito.times(2)).saveTicket(any(Ticket.class));
        ticket = ticketDAO.getTicket("ABCDEF");
        assertEquals(1, ticket.getId());
        assertEquals(1, ticket.getParkingSpot().getId());
        assertEquals("ABCDEF", ticket.getVehicleRegNumber());
        assertTrue(ticketDAO.isRecurentUser("ABCDEF"));


    }

    @Test
    public void testCalculFareRecurrentUserBIKE() throws InterruptedException, SQLException, ClassNotFoundException {
        /********************QUIT THE PARKING******************************/
        testParkingACar();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        Date           dateAfter      = new Date();
        TimeUnit.SECONDS.sleep(1);
        parkingService.processExitingVehicle();
        TimeUnit.SECONDS.sleep(1);
        verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
        Ticket NewTicket = ticketDAO.getTicket("ABCDEF");
        assertEquals(ticket.getId(), NewTicket.getId());
        assertEquals(ticket.getParkingSpot(), NewTicket.getParkingSpot());
        assertEquals(ticket.getVehicleRegNumber(), NewTicket.getVehicleRegNumber());
        assertEquals(ticket.getInTime(), NewTicket.getInTime());
        assertNotNull(NewTicket.getOutTime());
        assertTrue(NewTicket.getOutTime().after(dateAfter));

        /***********************************GET IN PARKING***********************************/

        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        Date dateIn = new Date();
        TimeUnit.SECONDS.sleep(1);
        parkingService.processIncomingVehicle();
        TimeUnit.SECONDS.sleep(1);
        verify(parkingSpotDAO, Mockito.times(3)).updateParking(any(ParkingSpot.class));
        assertEquals(4, parkingSpotDAO.getNextAvailableSlot(ParkingType.BIKE));
        verify(ticketDAO, Mockito.times(2)).saveTicket(any(Ticket.class));
        ticket = ticketDAO.getTicket("ABCDEF");
        assertEquals(1, ticket.getId());
        assertEquals(1, ticket.getParkingSpot().getId());
        assertEquals("ABCDEF", ticket.getVehicleRegNumber());

        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (60 * 60 * 1000));
        Date        outTime     = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE, false);
        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(ticket.getPrice(), Fare.BIKE_RATE_PER_HOUR * 0.95);
        assertTrue(ticketDAO.isRecurentUser("ABCDEF"));


    }

    @Test
    public void testCalculFareRecurrentUserCAR() throws InterruptedException, SQLException, ClassNotFoundException {
        /********************QUIT THE PARKING******************************/
        testParkingACar();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        Date           dateAfter      = new Date();
        TimeUnit.SECONDS.sleep(1);
        parkingService.processExitingVehicle();
        TimeUnit.SECONDS.sleep(1);
        verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
        Ticket NewTicket = ticketDAO.getTicket("ABCDEF");
        assertEquals(ticket.getId(), NewTicket.getId());
        assertEquals(ticket.getParkingSpot(), NewTicket.getParkingSpot());
        assertEquals(ticket.getVehicleRegNumber(), NewTicket.getVehicleRegNumber());
        assertEquals(ticket.getInTime(), NewTicket.getInTime());
        assertNotNull(NewTicket.getOutTime());
        assertTrue(NewTicket.getOutTime().after(dateAfter));

        /***********************************GET IN PARKING***********************************/

        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        TimeUnit.SECONDS.sleep(1);
        parkingService.processIncomingVehicle();
        TimeUnit.SECONDS.sleep(1);
        verify(parkingSpotDAO, Mockito.times(3)).updateParking(any(ParkingSpot.class));
        assertEquals(2, parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR));
        verify(ticketDAO, Mockito.times(2)).saveTicket(any(Ticket.class));
        ticket = ticketDAO.getTicket("ABCDEF");
        assertEquals(1, ticket.getId());
        assertEquals(1, ticket.getParkingSpot().getId());
        assertEquals("ABCDEF", ticket.getVehicleRegNumber());
        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (60 * 60 * 1000));
        Date        outTime     = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(ticket.getPrice(), Fare.CAR_RATE_PER_HOUR * 0.95);
        assertTrue(ticketDAO.isRecurentUser("ABCDEF"));


    }


}
