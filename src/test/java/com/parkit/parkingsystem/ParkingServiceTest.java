package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {


    @Mock
    public static  ParkingSpotDAO  parkingSpotDAO;
    @Mock
    public static  ParkingSpot     parkingSpot;
    private static ParkingService  parkingService;
    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static TicketDAO       ticketDAO;
    private        Ticket          ticket = new Ticket();

    @BeforeEach
    private void setUpPerTest() {

        //        try {

        parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
        ticket      = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("ABCDEF");


        //        } catch (Exception e) {
        //            e.printStackTrace();
        //            throw new RuntimeException("Failed to set up test mock objects");
        //        }
    }

    @Test
    public void processExitingVehicleTest() throws Exception {

        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);

        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
        parkingService.processExitingVehicle();
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
    }


    @Test
    public void getNextParkingNumberIfAvailableTestError() throws Exception {

        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(0);

        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        Assertions.assertDoesNotThrow(() -> parkingService.getNextParkingNumberIfAvailable());
    }

    @Test
    public void TypeVehicleTestExeptions() {

        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        when(inputReaderUtil.readSelection()).thenReturn(8);
        assertThrows(IllegalArgumentException.class, () -> parkingService.getVehichleType());
    }


}