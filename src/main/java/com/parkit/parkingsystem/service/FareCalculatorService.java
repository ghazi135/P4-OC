package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.Ticket;

import java.sql.SQLException;

import static java.time.temporal.ChronoUnit.SECONDS;

public class FareCalculatorService {

        int HALF_HOUR =1800;
        private TicketDAO userRec;

        public FareCalculatorService( TicketDAO newuserRec){
            userRec = newuserRec;
      }

    public void  calculateFare(Ticket ticket) throws SQLException, ClassNotFoundException {
            if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
                throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString());
            }


            //TODO: Some tests are failing here. Need to check if this logic is correct

            double duration = SECONDS.between(ticket.getInTime().toInstant(), ticket.getOutTime().toInstant());
            double ratePerHour;
            switch (ticket.getParkingSpot().getParkingType()){
                case CAR: {
                    ratePerHour= Fare.CAR_RATE_PER_HOUR;
                    break;
                }
                case BIKE: {
                    ratePerHour= Fare.BIKE_RATE_PER_HOUR;

                    break;
                }
                default: throw new IllegalArgumentException("Unkown Parking Type");
            }
            ticket.setPrice(duration > HALF_HOUR ? duration / 3600.0 * ratePerHour : 0);

            if (userRec.isRec(ticket.getVehicleRegNumber())){
                System.out.println("You are a recurent User");
                ticket.setPrice(ticket.getPrice() * 0.95);
            }

        }

    }
