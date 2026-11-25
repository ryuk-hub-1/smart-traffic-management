package com.smarttraffic.service;

import com.smarttraffic.dao.AttendanceDao;
import com.smarttraffic.dao.DutyDao;
import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.exception.ValidationException;
import com.smarttraffic.model.Attendance;
import com.smarttraffic.model.DutyAssignment;
import com.smarttraffic.model.enums.DutyStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Service managing Officer Duty Assignments, Shifts, and Daily Attendance.
 */
public class DutyService {
    private static DutyService instance;
    private final DutyDao dutyDao;
    private final AttendanceDao attendanceDao;

    private DutyService() {
        this.dutyDao = new DutyDao();
        this.attendanceDao = new AttendanceDao();
    }

    public static synchronized DutyService getInstance() {
        if (instance == null) {
            instance = new DutyService();
        }
        return instance;
    }

    public List<DutyAssignment> getAllDuties() throws DatabaseException {
        return dutyDao.findAll();
    }

    public List<DutyAssignment> getDutiesForOfficer(int officerId) throws DatabaseException {
        return dutyDao.findByOfficerId(officerId);
    }

    public Optional<DutyAssignment> getTodaysActiveDutyForOfficer(int officerId) throws DatabaseException {
        return dutyDao.findActiveDutyForOfficer(officerId, LocalDate.now());
    }

    public DutyAssignment scheduleDuty(DutyAssignment duty) throws ValidationException, DatabaseException {
        if (duty.getOfficerId() <= 0) {
            throw new ValidationException("Officer must be selected.");
        }
        if (duty.getJunctionId() <= 0) {
            throw new ValidationException("Junction must be selected.");
        }
        if (duty.getDutyDate() == null) {
            throw new ValidationException("Duty date is required.");
        }
        return dutyDao.save(duty);
    }

    public boolean updateDutyStatus(int dutyId, DutyStatus status) throws DatabaseException {
        return dutyDao.updateStatus(dutyId, status);
    }

    public boolean deleteDuty(int dutyId) throws DatabaseException {
        return dutyDao.deleteById(dutyId);
    }

    // --- Attendance Operations ---

    public List<Attendance> getAllAttendance() throws DatabaseException {
        return attendanceDao.findAll();
    }

    public List<Attendance> getAttendanceForOfficer(int officerId) throws DatabaseException {
        return attendanceDao.findByOfficerId(officerId);
    }

    public Optional<Attendance> getTodayAttendanceForOfficer(int officerId) throws DatabaseException {
        return attendanceDao.findTodayAttendanceForOfficer(officerId, LocalDate.now());
    }

    public boolean deleteAttendance(int attendanceId) throws DatabaseException {
        return attendanceDao.deleteById(attendanceId);
    }

    public Attendance markCheckIn(int officerId, Integer dutyId, String remarks) throws ValidationException, DatabaseException {
        // Check if already checked in today
        Optional<Attendance> todayAtt = getTodayAttendanceForOfficer(officerId);
        if (todayAtt.isPresent() && todayAtt.get().getCheckInTime() != null) {
            throw new ValidationException("Officer has already checked in today at " + todayAtt.get().getCheckInTime());
        }

        // Auto-link today's active duty if not explicitly passed
        if (dutyId == null || dutyId <= 0) {
            Optional<DutyAssignment> active = getTodaysActiveDutyForOfficer(officerId);
            if (active.isPresent()) {
                dutyId = active.get().getId();
            }
        }

        Attendance att = attendanceDao.checkIn(officerId, dutyId, LocalTime.now(), remarks);
        if (dutyId != null && dutyId > 0) {
            dutyDao.updateStatus(dutyId, DutyStatus.IN_PROGRESS);
        }
        return att;
    }

    public boolean markCheckOut(int attendanceId, Integer dutyId) throws ValidationException, DatabaseException {
        Optional<Attendance> att = attendanceDao.findById(attendanceId);
        if (att.isEmpty()) {
            throw new ValidationException("Attendance record not found.");
        }
        if (att.get().getCheckOutTime() != null) {
            throw new ValidationException("Officer has already checked out at " + att.get().getCheckOutTime());
        }

        // Auto-retrieve linked duty if not explicitly passed
        if ((dutyId == null || dutyId <= 0) && att.get().getDutyId() != null) {
            dutyId = att.get().getDutyId();
        }

        boolean ok = attendanceDao.checkOut(attendanceId, LocalTime.now());
        if (ok && dutyId != null && dutyId > 0) {
            dutyDao.updateStatus(dutyId, DutyStatus.COMPLETED);
        }
        return ok;
    }

    public Attendance recordAttendance(Attendance att) throws DatabaseException {
        return attendanceDao.save(att);
    }
}
