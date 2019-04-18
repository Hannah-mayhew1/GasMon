package org.softwire.training.gasmon.services;

import org.softwire.training.gasmon.model.Event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EventsService {

    private List<Event> allPreviousEvents;

    public EventsService() {
        this.allPreviousEvents = new ArrayList<>();
    }

    public List<Event> getAllPreviousEvents() {
        return allPreviousEvents;
    }

    public void addEvent(Event event) {
        allPreviousEvents.add(event);
    }

    public boolean eventHasBeenSeen(Event event) {
        return allPreviousEvents.contains(event);
    }

    public double averageValueOfEventsWithin1Minute() {
        long currentTimestamp = System.currentTimeMillis();
        double sumOfValues = 0;
        int count = 0;
        for (Event event : allPreviousEvents) {
            if (currentTimestamp - event.getTimestamp() >= 300000 && currentTimestamp - event.getTimestamp() < 360000) {
                sumOfValues +=  event.getValue();
                count++;
            }
        }
        return sumOfValues/count;
    }

    public HashMap<String, Double> averageValueOfLocation() {
        HashMap<String, List<Event>> map = new HashMap<>();

        for(Event event : allPreviousEvents) {
            if (!map.containsKey(event.getLocationId())) {
                map.put(event.getLocationId(), new ArrayList<>());
            }
            map.get(event.getLocationId()).add(event);
        }

        HashMap<String, Double> uniqueLocationAverage = new HashMap<>();

        for (String locationId : map.keySet()) {
            double sumOfValues = 0;
            int count = 0;
            for(Event event : map.get(locationId)) {
                sumOfValues +=  event.getValue();
                count++;
            }
            double average = sumOfValues/count;
            uniqueLocationAverage.put(locationId, average);
        }
        return uniqueLocationAverage;
    }

    public void deleteEventsOlderThan10Minutes() {
        long currentTimestamp = System.currentTimeMillis();
        List<Event> survivingEvents = new ArrayList<>();
        for (Event event : allPreviousEvents) {
            if (event.getTimestamp() > currentTimestamp - 600000) {
                survivingEvents.add(event);
            }
        }
        allPreviousEvents = survivingEvents;
    }
}
