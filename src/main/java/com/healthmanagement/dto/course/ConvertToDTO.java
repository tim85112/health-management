package com.healthmanagement.dto.course;

import com.healthmanagement.model.course.Course;
import com.healthmanagement.model.course.Enrollment;
// TODO: 如果你的教練是 User 實體，也需要 import User
import com.healthmanagement.model.member.User; // 假設教練是 User 實體

import org.springframework.stereotype.Component;

@Component
public class ConvertToDTO {

	public EnrollmentDTO convertToEnrollmentDTO(Enrollment enrollment) {

		Course associatedCourse = enrollment.getCourse(); // 先獲取關聯的 Course

		// 獲取關聯的教練 (假設 Course 實體有 getCoach() 方法返回 User 實體)
		User coach = (associatedCourse != null) ? associatedCourse.getCoach() : null;

		// 從 Entity 獲取數據，加入 Null 檢查並構建 DTO
		return EnrollmentDTO.builder().id(enrollment.getId())
				.userId(enrollment.getUser() != null ? enrollment.getUser().getId() : null) // 假設 Enrollment 有 getUser()
				.userName(enrollment.getUser() != null ? enrollment.getUser().getName() : null) // 假設 User 有 getName()
				.courseId(associatedCourse != null ? associatedCourse.getId() : null)
				.dayOfWeek(associatedCourse != null ? associatedCourse.getDayOfWeek() : null) // 如果 DTO 需要這些信息
				.startTime(associatedCourse != null ? associatedCourse.getStartTime() : null) // 如果 DTO 需要這些信息
				.enrollmentTime(enrollment.getEnrollmentTime())
				.courseName(associatedCourse != null ? associatedCourse.getName() : null).status(enrollment.getStatus())
				// **新增這裡：設置教練名字**
				.coachName(coach != null ? coach.getName() : null) // <-- 獲取教練名字並設置
				.build();
	}
}

// TODO: 如果你有其他需要轉換的 DTO，可以在這裡添加相應的方法
// 例如：public CourseDTO convertToCourseDTO(Course course) { ... }
//       public UserDTO convertToUserDTO(User user) { ... }