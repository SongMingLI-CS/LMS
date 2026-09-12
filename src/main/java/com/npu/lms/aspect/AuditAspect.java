package com.npu.lms.aspect;

import com.npu.lms.entity.Book;
import com.npu.lms.entity.User;
import com.npu.lms.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

@Aspect
@Component
public class AuditAspect {

    @Autowired
    private AuditLogService auditLogService;

    // --- 1. 定义切点 (Pointcuts) ---

    // 拦截所有借书操作
    @Pointcut("execution(* com.npu.lms.service.RecordService.borrowBook(..))")
    public void borrowBookPointcut() {}

    // 拦截所有还书操作
    @Pointcut("execution(* com.npu.lms.service.RecordService.returnBook(..))")
    public void returnBookPointcut() {}

    // 拦截所有图书创建
    @Pointcut("execution(* com.npu.lms.service.BookService.saveBook(..))")
    public void createBookPointcut() {}

    // 拦截所有图书更新
    @Pointcut("execution(* com.npu.lms.service.BookService.updateBook(..))")
    public void updateBookPointcut() {}

    // 拦截所有用户创建/更新 (来自 UserController)
    @Pointcut("execution(* com.npu.lms.service.UserService.createUser(..))")
    public void createUserPointcut() {}

    @Pointcut("execution(* com.npu.lms.service.UserService.updateUser(..))")
    public void saveUserPointcut() {}

    // 拦截所有用户删除
    @Pointcut("execution(* com.npu.lms.service.UserService.deleteUser(..))")
    public void deleteUserPointcut() {}

    // 拦截数据导出（敏感操作）
    @Pointcut("execution(* com.npu.lms.controller.RecordController.exportRecordsExcel(..)) || " +
              "execution(* com.npu.lms.controller.AnalysisController.export*(..))")
    public void exportPointcut() {}

    // --- 2. 定义通知 (Advices) ---

    // 在借书成功后记录
    @AfterReturning(pointcut = "borrowBookPointcut()", returning = "result")
    public void logBorrow(JoinPoint joinPoint, Object result) {
        String details = "借阅成功";
        // (可以从 result 或 joinPoint.getArgs() 中获取更详细信息)
        log(joinPoint, "BORROW_BOOK", details);
    }

    // 在还书成功后记录
    @AfterReturning(pointcut = "returnBookPointcut()", returning = "result")
    public void logReturn(JoinPoint joinPoint, Object result) {
        log(joinPoint, "RETURN_BOOK", "还书成功");
    }

    // 在创建图书成功后记录
    @AfterReturning(pointcut = "createBookPointcut()", returning = "result")
    public void logCreateBook(JoinPoint joinPoint, Object result) {
        if (result instanceof Book book && joinPoint.getArgs().length > 0) {
            // 假设 saveBook 用于创建 (ID 为 null)
            if (book.getId() != null) {
                log(joinPoint, "CREATE_BOOK", "图书入库: " + book.getTitle() + " (ID: " + book.getId() + ")");
            }
        }
    }

    // 在更新图书成功后记录
    @AfterReturning(pointcut = "updateBookPointcut()", returning = "result")
    public void logUpdateBook(JoinPoint joinPoint, Object result) {
        if (result instanceof Book book) {
            log(joinPoint, "UPDATE_BOOK", "图书更新: " + book.getTitle() + " (ID: " + book.getId() + ")");
        }
    }

    // 在创建用户成功后记录
    @AfterReturning(pointcut = "createUserPointcut()", returning = "result")
    public void logCreateUser(JoinPoint joinPoint, Object result) {
        if (result instanceof User user) {
            log(joinPoint, "CREATE_USER", "新增用户: " + user.getUsername() + " (ID: " + user.getId() + ")");
        }
    }

    // 在保存用户成功后记录
    @AfterReturning(pointcut = "saveUserPointcut()", returning = "result")
    public void logSaveUser(JoinPoint joinPoint, Object result) {
        if (result instanceof User user) {
            log(joinPoint, "SAVE_USER", "保存用户: " + user.getUsername() + " (ID: " + user.getId() + ")");
        }
    }

    // 在删除用户成功后记录
    @AfterReturning(pointcut = "deleteUserPointcut()")
    public void logDeleteUser(JoinPoint joinPoint) {
        // 从参数中获取 ID
        Long userId = (Long) joinPoint.getArgs()[0];
        log(joinPoint, "DELETE_USER", "删除用户 ID: " + userId);
    }

    // 在数据导出成功后记录
    @AfterReturning(pointcut = "exportPointcut()")
    public void logExport(JoinPoint joinPoint) {
        log(joinPoint, "EXPORT_DATA", "数据导出: " + joinPoint.getSignature().getName());
    }


    // --- 3. 通用日志记录方法 ---
    private void log(JoinPoint joinPoint, String action, String details) {
        try {
            // 获取当前用户名（定时任务/匿名上下文可能为 null，必须防御）
            org.springframework.security.core.Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();
            String username = (authentication == null || !authentication.isAuthenticated())
                    ? "system"
                    : authentication.getName();

            // 获取 IP 地址
            String ipAddress = "Unknown";
            try {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    ipAddress = request.getRemoteAddr();
                }
            } catch (Exception e) {
                // 忽略 (例如在定时任务中调用时)
            }

            // 异步保存日志
            auditLogService.logAction(username, action, details, ipAddress);
        } catch (Exception e) {
            // 审计失败绝不允许影响主业务流程（@AfterReturning 抛出的异常会传播给调用方）
            org.slf4j.LoggerFactory.getLogger(AuditAspect.class)
                    .warn("写入审计日志失败: action={}, error={}", action, e.getMessage());
        }
    }
}