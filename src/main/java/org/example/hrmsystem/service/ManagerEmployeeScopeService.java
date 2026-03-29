package org.example.hrmsystem.service;

import org.example.hrmsystem.model.Department;
import org.example.hrmsystem.model.Employee;
import org.example.hrmsystem.model.Role;
import org.example.hrmsystem.model.UserAccount;
import org.example.hrmsystem.repository.DepartmentRepository;
import org.example.hrmsystem.repository.EmployeeRepository;
import org.example.hrmsystem.repository.UserAccountRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Xác định nhân viên thuộc phạm vi quản lý của trưởng phòng.
 * Hàng chờ duyệt đơn + lịch sử chấm công của Manager chỉ gồm nhân viên vai trò EMPLOYEE (hoặc chưa có tài khoản).
 */
@Service
public class ManagerEmployeeScopeService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;

    public ManagerEmployeeScopeService(
            DepartmentRepository departmentRepository,
            EmployeeRepository employeeRepository,
            UserAccountRepository userAccountRepository
    ) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.userAccountRepository = userAccountRepository;
    }

    public Set<Long> visibleEmployeeIdsForManager(Long managerEmployeeId) {
        if (managerEmployeeId == null) {
            return Set.of();
        }
        Set<Long> deptIds = new HashSet<>();
        for (Department d : departmentRepository.findByManagerId(managerEmployeeId)) {
            deptIds.add(d.getId());
        }
        if (deptIds.isEmpty()) {
            employeeRepository.findById(managerEmployeeId)
                    .map(Employee::getDepartmentId)
                    .filter(Objects::nonNull)
                    .ifPresent(deptIds::add);
        }
        if (deptIds.isEmpty()) {
            return Set.of();
        }
        return employeeRepository.findByDepartmentIdIn(deptIds).stream()
                .map(Employee::getId)
                .collect(Collectors.toSet());
    }

    public boolean managesEmployee(Long managerEmployeeKey, Long employeeId) {
        if (employeeId == null) {
            return false;
        }
        return visibleEmployeeIdsForManager(managerEmployeeKey).contains(employeeId);
    }

    /**
     * Phạm vi “cấp dưới nghiệp vụ” của trưởng phòng: như {@link #visibleEmployeeIdsForManager} nhưng bỏ HR/MANAGER/ADMIN.
     */
    public Set<Long> managedEmployeeApplicantIds(Long managerEmployeeId) {
        return filterEmployeeApplicantIds(visibleEmployeeIdsForManager(managerEmployeeId));
    }

    private Set<Long> filterEmployeeApplicantIds(Set<Long> employeeIds) {
        if (employeeIds.isEmpty()) {
            return Set.of();
        }
        List<UserAccount> accounts = userAccountRepository.findByEmployeeIdIn(employeeIds);
        Map<Long, Role> roleByEmpId = accounts.stream()
                .filter(ua -> ua.getEmployeeId() != null)
                .collect(Collectors.toMap(UserAccount::getEmployeeId, UserAccount::getRole, (a, b) -> a));
        return employeeIds.stream()
                .filter(id -> {
                    Role r = roleByEmpId.get(id);
                    return r == null || r == Role.EMPLOYEE;
                })
                .collect(Collectors.toSet());
    }
}
