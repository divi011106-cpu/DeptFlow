package com.example.deptflow;

import com.example.deptflow.feature.faculty.models.FacultyUser;
import com.example.deptflow.feature.faculty.models.Task;
import com.example.deptflow.feature.faculty.repository.TaskRepository;
import com.example.deptflow.hod.TaskData;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class TaskSyncUnitTest {

    @Before
    public void setUp() {
        TaskData.tasks.clear();
    }

    @Test
    public void testHodAssignedTaskParsedAndMatchedCorrectly() {
        // 1. Simulate HOD AssignTaskActivity creating the task
        String hodTaskRaw = "Title: 1\nDescription: 123\nDeadline: 16/9/2026\nFaculty: Dr. T. Sarnya\n";
        TaskData.tasks.add(hodTaskRaw);

        assertEquals(1, TaskData.tasks.size());

        // 2. Parse HOD task
        Task parsed = TaskRepository.parseHodTask(TaskData.tasks.get(0), 0);
        assertNotNull(parsed);
        assertEquals("HOD-TASK-0", parsed.getTaskId());
        assertEquals("1", parsed.getTaskTitle());
        assertEquals("123", parsed.getDescription());
        assertEquals("16/9/2026", parsed.getDeadline());
        assertEquals("Dr. T. Sarnya", parsed.getAssignedTo());
        assertEquals(Task.STATUS_PENDING, parsed.getStatus());
    }

    @Test
    public void testMultiFacultyDynamicIsolation() {
        // HOD assigns:
        // Task A -> Dr. T. Sarnya
        // Task B -> Dr. R. Vijayalakshmi
        // Task C -> Dr. R. Raja Sudharsan
        // Task D -> Dr. T. Sarnya (second task)
        TaskData.tasks.add("Title: Task A\nDescription: For Sarnya\nDeadline: 20/09/2026\nFaculty: Dr. T. Sarnya\n");
        TaskData.tasks.add("Title: Task B\nDescription: For Vijayalakshmi\nDeadline: 21/09/2026\nFaculty: Dr. R. Vijayalakshmi\n");
        TaskData.tasks.add("Title: Task C\nDescription: For Raja Sudharsan\nDeadline: 22/09/2026\nFaculty: Dr. R. Raja Sudharsan\n");
        TaskData.tasks.add("Title: Task D\nDescription: Second for Sarnya\nDeadline: 23/09/2026\nFaculty: Dr. T. Sarnya\n");

        assertEquals(4, TaskData.tasks.size());

        FacultyUser sarnya = new FacultyUser("FAC-101", "Dr. T. Sarnya", "sarnya@deptflow.edu", "IT", "FACULTY");
        FacultyUser vijayalakshmi = new FacultyUser("FAC-102", "Dr. R. Vijayalakshmi", "viji@deptflow.edu", "IT", "FACULTY");
        FacultyUser sudharsan = new FacultyUser("FAC-103", "Dr. R. Raja Sudharsan", "sudharsan@deptflow.edu", "IT", "FACULTY");
        FacultyUser other = new FacultyUser("FAC-104", "Mrs. M. Prabha", "prabha@deptflow.edu", "IT", "FACULTY");

        // Parse all HOD tasks
        List<Task> allParsed = new java.util.ArrayList<>();
        for (int i = 0; i < TaskData.tasks.size(); i++) {
            allParsed.add(TaskRepository.parseHodTask(TaskData.tasks.get(i), i));
        }

        // Test Sarnya sees Task A and Task D (Total = 2)
        int sarnyaCount = 0;
        for (Task t : allParsed) {
            if (t.getAssignedTo().contains("Sarnya")) {
                sarnyaCount++;
            }
        }
        assertEquals(2, sarnyaCount);

        // Test Vijayalakshmi sees Task B only (Total = 1)
        int vijiCount = 0;
        for (Task t : allParsed) {
            if (t.getAssignedTo().contains("Vijayalakshmi")) {
                vijiCount++;
            }
        }
        assertEquals(1, vijiCount);

        // Test Sudharsan sees Task C only (Total = 1)
        int sudharsanCount = 0;
        for (Task t : allParsed) {
            if (t.getAssignedTo().contains("Raja Sudharsan")) {
                sudharsanCount++;
            }
        }
        assertEquals(1, sudharsanCount);

        // Test Prabha sees 0 tasks
        int prabhaCount = 0;
        for (Task t : allParsed) {
            if (t.getAssignedTo().contains("Prabha")) {
                prabhaCount++;
            }
        }
        assertEquals(0, prabhaCount);
    }

    @Test
    public void testStatusUpdateSyncsBackToHodTaskData() {
        // HOD creates task
        String raw = "Title: Syllabus Review\nDescription: Complete unit 1\nDeadline: 30/09/2026\nFaculty: Dr. T. Sarnya\n";
        TaskData.tasks.add(raw);

        // Faculty reads task
        Task task = TaskRepository.parseHodTask(TaskData.tasks.get(0), 0);
        assertEquals(Task.STATUS_PENDING, task.getStatus());

        // Faculty updates status -> IN_PROGRESS
        int index = 0;
        String currentRaw = TaskData.tasks.get(index);
        String updatedRaw = currentRaw.trim() + "\nStatus: IN_PROGRESS";
        TaskData.tasks.set(index, updatedRaw);

        // HOD ViewTasks reads TaskData.tasks.get(0)
        String hodViewedTask = TaskData.tasks.get(0);
        assertTrue(hodViewedTask.contains("Status: IN_PROGRESS"));

        // Faculty re-parses task
        Task updatedTask = TaskRepository.parseHodTask(TaskData.tasks.get(0), 0);
        assertEquals(Task.STATUS_IN_PROGRESS, updatedTask.getStatus());

        // Faculty updates status -> COMPLETED
        updatedRaw = TaskData.tasks.get(0).replaceAll("(?i)Status:[^\r\n]*", "Status: COMPLETED");
        TaskData.tasks.set(index, updatedRaw);

        // Verify HOD sees COMPLETED
        assertTrue(TaskData.tasks.get(0).contains("Status: COMPLETED"));
        Task completedTask = TaskRepository.parseHodTask(TaskData.tasks.get(0), 0);
        assertEquals(Task.STATUS_COMPLETED, completedTask.getStatus());
    }
}
