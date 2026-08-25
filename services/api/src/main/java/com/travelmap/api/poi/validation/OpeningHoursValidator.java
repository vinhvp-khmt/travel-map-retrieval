package com.travelmap.api.poi.validation;

import com.travelmap.api.common.ApiException;
import com.travelmap.api.poi.dto.OpeningHourRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class OpeningHoursValidator {
    private static final int MINUTES_PER_DAY = 1440;
    private static final int MINUTES_PER_WEEK = 10080;

    public void validate(List<OpeningHourRequest> requests) {
        List<Interval> intervals = new ArrayList<>();
        Set<Integer> closedDays = new HashSet<>();
        Set<Integer> openDays = new HashSet<>();
        for (OpeningHourRequest request : requests) {
            if (request.closed()) {
                if (request.openTime() != null || request.closeTime() != null || openDays.contains(request.dayOfWeek())
                        || !closedDays.add(request.dayOfWeek())) {
                    invalid();
                }
                continue;
            }
            if (request.openTime() == null || request.closeTime() == null || closedDays.contains(request.dayOfWeek())) {
                invalid();
            }
            openDays.add(request.dayOfWeek());
            int start = (request.dayOfWeek() - 1) * MINUTES_PER_DAY
                    + request.openTime().getHour() * 60 + request.openTime().getMinute();
            int end = (request.dayOfWeek() - 1) * MINUTES_PER_DAY
                    + request.closeTime().getHour() * 60 + request.closeTime().getMinute();
            if (end <= start) end += MINUTES_PER_DAY;
            intervals.add(new Interval(start, end));
        }
        intervals.sort(Comparator.comparingInt(Interval::start));
        for (int index = 1; index < intervals.size(); index++) {
            if (intervals.get(index).start() < intervals.get(index - 1).end()) invalid();
        }
        if (!intervals.isEmpty()) {
            Interval firstNextWeek = new Interval(intervals.get(0).start() + MINUTES_PER_WEEK,
                    intervals.get(0).end() + MINUTES_PER_WEEK);
            if (firstNextWeek.start() < intervals.get(intervals.size() - 1).end()) invalid();
        }
    }

    private static void invalid() {
        throw new ApiException(HttpStatus.BAD_REQUEST, "OPENING_HOURS_INVALID",
                "Opening hours must be complete and must not overlap");
    }

    private record Interval(int start, int end) { }
}
